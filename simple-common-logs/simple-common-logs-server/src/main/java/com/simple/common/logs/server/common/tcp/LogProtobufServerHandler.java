package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.proto.LogBatch;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * Protobuf 批量日志服务端处理器（Fire-and-Forget 模式）
 * <p>
 * 接收客户端批量日志，直接转交保存管理器处理，不发送任何响应。
 * 这种设计可减少网络往返开销和编解码消耗，适合极高吞吐场景。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class LogProtobufServerHandler extends SimpleChannelInboundHandler<LogBatch> {

    /**
     * 单帧允许的最大日志条数：10MB 帧长上限下可塞入海量小日志，
     * 一次性全量转换会产生内存放大，超限直接丢弃并告警
     */
    private static final int MAX_LOGS_PER_BATCH = 10000;

    private final LogsSaveManager logsSaveManager;

    /**
     * 业务线程池：持久化涉及 JSON 序列化与 WAL 磁盘 IO（共享锁），
     * 直接在 Netty IO 线程执行会阻塞 event loop，拖慢同一 worker 上所有连接
     */
    private final ExecutorService saveExecutor;

    public LogProtobufServerHandler(LogsSaveManager logsSaveManager, ExecutorService saveExecutor) {
        this.logsSaveManager = logsSaveManager;
        this.saveExecutor = saveExecutor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogBatch batch) throws Exception {
        // 空批次直接忽略，避免无谓的对象分配与线程池调度
        if (batch == null || batch.getLogsCount() == 0) {
            return;
        }
        int count = batch.getLogsCount();
        if (count > MAX_LOGS_PER_BATCH) {
            log.warn("批量日志条数 {} 超过上限 {}，丢弃该批次, 远端: {}", count, MAX_LOGS_PER_BATCH, ctx.channel().remoteAddress());
            return;
        }
        // 转移到业务线程池执行：JSON 序列化/WAL 磁盘 IO 不得阻塞 IO 线程；
        // 线程池饱和时显式告警并丢弃（日志为尽力而为语义），不在 IO 线程堆积任务
        try {
            saveExecutor.execute(() -> handleBatch(batch, count));
        } catch (RejectedExecutionException e) {
            log.error("日志保存线程池已饱和，丢弃 {} 条日志", count);
        }
    }

    private void handleBatch(LogBatch batch, int count) {
        try {
            // 将 Protobuf LogBatch 转换为 LogDataEvent 列表
            List<LogDataEvent> events = LogDataEvent.fromBatchProto(batch);

            // 批量保存日志（充分利用批量接口，减少入队次数）
            logsSaveManager.saveLogBatch(events);

            log.debug("批量日志处理完成, 数量: {}", events.size());
        } catch (Exception e) {
            // 携带批次条数，保证丢数据可观测
            log.error("批量日志处理失败, 批次条数: {}", count, e);
            // Fire-and-Forget 模式下无需向客户端返回错误信息
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 仅协议级（解码/帧）异常断连；其余异常仅记录并保留连接，
        // 避免 Fire-and-Forget 场景下瞬时异常导致无谓断连与客户端重连风暴
        if (cause instanceof DecoderException) {
            log.error("日志数据解码异常, 关闭连接 {}: {}", ctx.channel().remoteAddress(), cause.getMessage(), cause);
            ctx.close();
        } else {
            log.error("日志数据处理异常, 远端: {}", ctx.channel().remoteAddress(), cause);
        }
    }
}
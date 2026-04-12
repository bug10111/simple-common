package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.proto.common.event.LogDataEvent;
import com.simple.common.logs.proto.LogBatch;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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

    private final LogsSaveManager logsSaveManager;

    public LogProtobufServerHandler(LogsSaveManager logsSaveManager) {
        this.logsSaveManager = logsSaveManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogBatch batch) throws Exception {
        try {
            // 将 Protobuf LogBatch 转换为 LogDataEvent 列表
            List<LogDataEvent> events = LogDataEvent.fromBatchProto(batch);

            // 批量保存日志（充分利用批量接口，减少入队次数）
            logsSaveManager.saveLogBatch(events);

            log.debug("批量日志处理完成, 数量: {}", events.size());
        } catch (Exception e) {
            log.error("批量日志处理失败", e);
            // Fire-and-Forget 模式下无需向客户端返回错误信息
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理日志数据异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
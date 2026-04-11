package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.proto.LogData;
import com.simple.common.logs.proto.LogResponse;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf 日志服务端处理器
 *
 * @author qty
 */
@Slf4j
public class LogProtobufServerHandler extends SimpleChannelInboundHandler<LogData> {

    private final LogsSaveManager logsSaveManager;

    public LogProtobufServerHandler(LogsSaveManager logsSaveManager) {
        this.logsSaveManager = logsSaveManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogData msg) throws Exception {
        log.debug("收到日志数据: traceId={}, operUrl={}", msg.getTraceId(), msg.getOperUrl());

        try {
            // 将 Protobuf LogData 转换为 LogDataEvent
            LogDataEvent event = LogDataEvent.fromProto(msg);

            // 保存日志
            logsSaveManager.saveLog(event);

            // 发送成功响应
            LogResponse response = LogResponse.newBuilder().setSuccess(true).setMessage("日志保存成功").build();
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("保存日志失败: {}", e.getMessage(), e);

            // 发送失败响应
            LogResponse response = LogResponse.newBuilder().setSuccess(false).setMessage("日志保存失败: " + e.getMessage()).build();
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理日志数据异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
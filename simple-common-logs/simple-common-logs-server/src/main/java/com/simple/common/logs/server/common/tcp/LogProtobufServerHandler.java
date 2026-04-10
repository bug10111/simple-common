package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.proto.LogData;
import com.simple.common.logs.proto.LogDataEvent;
import com.simple.common.logs.proto.LogResponse;
import com.simple.common.logs.server.common.converter.LogDataConverter;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf 日志服务端处理器
 * 处理客户端发送的日志数据
 *
 * @author Admin
 */
@Slf4j
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class LogProtobufServerHandler extends SimpleChannelInboundHandler<LogData> {

    private final LogsSaveManager logsSaveManager;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogData msg) throws Exception {
        try {
            // 转换为 LogDataEvent
            LogDataEvent event = LogDataConverter.toEvent(msg);

            // 添加到保存队列
            logsSaveManager.addLogData(event);

            // 发送成功响应
            LogResponse response = LogResponse.newBuilder().setCode(200).setMessage("success").setSuccess(true).build();
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("处理日志数据失败: {}", e.getMessage(), e);
            // 发送失败响应
            LogResponse response = LogResponse.newBuilder().setCode(500).setMessage("处理失败: " + e.getMessage()).setSuccess(false).build();
            ctx.writeAndFlush(response);
        }
    }

    /**
     * 处理异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("处理日志数据异常", cause);
        // 发送错误响应
        LogResponse response = LogResponse.newBuilder().setSuccess(false).setMessage("处理失败: " + cause.getMessage()).build();
        ctx.writeAndFlush(response);
    }
}
package com.simple.common.logs.server.common.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * TCP日志服务端处理器
 * 接收并处理客户端发送的日志数据
 *
 * @author qty
 */
@Slf4j
public class LogTcpServerHandler extends SimpleChannelInboundHandler<byte[]> {
    private final LogsSaveManager logsSaveManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LogTcpServerHandler(LogsSaveManager logsSaveManager) {
        this.logsSaveManager = logsSaveManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] data) throws Exception {
        try {
            // 解析JSON数据
            String jsonStr = new String(data, StandardCharsets.UTF_8);
            LogDataEvent logDataEvent = objectMapper.readValue(jsonStr, LogDataEvent.class);
            
            log.info("收到日志数据: traceId={}, operName={}, method={}, userId={}, status={}, costTime={}ms",
                    logDataEvent.getId(), logDataEvent.getOperName(), logDataEvent.getMethod(),
                    logDataEvent.getUserId(), logDataEvent.getStatus(), logDataEvent.getCostTime());

            // 添加到保存队列
            logsSaveManager.addLogData(logDataEvent);

            // 发送响应（可选）
            ctx.writeAndFlush("ACK".getBytes());

        } catch (Exception e) {
            log.error("处理日志数据失败", e);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端断开: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("TCP连接异常: {}", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

}
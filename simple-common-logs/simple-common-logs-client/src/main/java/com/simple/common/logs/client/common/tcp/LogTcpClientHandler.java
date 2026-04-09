package com.simple.common.logs.client.common.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP日志客户端处理器
 * 处理服务端响应
 *
 * @author qty
 */
@Slf4j
public class LogTcpClientHandler extends SimpleChannelInboundHandler<byte[]> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
        // 处理服务端响应
        String response = new String(msg);
        log.debug("收到服务端响应: {}", response);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("已连接到服务端: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("与服务端断开连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("TCP连接异常", cause);
        ctx.close();
    }
}

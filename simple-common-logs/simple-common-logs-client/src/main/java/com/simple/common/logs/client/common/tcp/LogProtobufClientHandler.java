package com.simple.common.logs.client.common.tcp;

import com.simple.common.logs.proto.LogResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf 日志客户端处理器
 * 处理服务端响应
 *
 * @author Admin
 */
@Slf4j
public class LogProtobufClientHandler extends SimpleChannelInboundHandler<LogResponse> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LogResponse msg) throws Exception {
        if (msg.getSuccess()) {
            log.debug("日志发送成功: code={}, message={}", msg.getCode(), msg.getMessage());
        } else {
            log.warn("日志发送失败: code={}, message={}", msg.getCode(), msg.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("日志客户端异常: {}", cause.getMessage(), cause);
        ctx.close();
    }
}
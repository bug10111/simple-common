package com.simple.common.logs.client.common.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf 日志客户端处理器（Fire-and-Forget 模式）
 * <p>
 * 本客户端仅负责发送日志，不处理任何服务端响应。
 * 为提升性能，已移除响应编解码和业务处理逻辑。
 * </p>
 *
 * @author Admin
 */
@Slf4j
public class LogProtobufClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Fire-and-Forget 模式：忽略所有入站数据，直接释放
        // 可根据需要记录调试日志，生产环境建议注释掉以避免性能损耗
        // log.debug("收到服务端数据，已忽略");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("日志客户端异常: {}", cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 心跳事件处理（可选）
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.WRITER_IDLE) {
                log.debug("写空闲，可发送心跳包（如需要）");
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
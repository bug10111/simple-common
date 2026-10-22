package com.simple.common.logs.client.common.tcp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
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
 * @author qty
 */
@Slf4j
public class LogProtobufClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Fire-and-Forget 模式：忽略所有入站数据，但必须释放引用计数对象，
        // 否则管道未解码时 msg 为 ByteBuf，直接丢弃引用会泄漏堆外内存（LEAK 报告/OOM）
        try {
            log.debug("收到服务端数据，已忽略");
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("日志客户端异常: {}", cause.getMessage(), cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 心跳事件处理
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.WRITER_IDLE) {
                // 写空闲：发送 0 长度心跳帧（4 字节全 0 长度前缀），
                // 既能保活又能探测半开连接；服务端将其识别为心跳帧直接丢弃
                ctx.writeAndFlush(Unpooled.copyInt(0)).addListener(future -> {
                    if (!future.isSuccess()) {
                        log.warn("心跳帧发送失败，主动关闭触发重连: {}", future.cause().getMessage());
                        ctx.close();
                    }
                });
            } else if (event.state() == IdleState.READER_IDLE || event.state() == IdleState.ALL_IDLE) {
                // 本协议为单向推送、服务端从不回包：读/全空闲属预期行为，仅记录，不处理
                log.debug("读空闲/全空闲事件（本协议服务端不回包，属预期），忽略");
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
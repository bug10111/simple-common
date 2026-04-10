package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.proto.LogResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Protobuf 日志响应编码器
 * 将 LogResponse Protobuf 消息编码为字节数组进行传输
 *
 * @author Admin
 */
public class LogResponseEncoder extends MessageToByteEncoder<LogResponse> {

    @Override
    protected void encode(ChannelHandlerContext ctx, LogResponse msg, ByteBuf out) throws Exception {
        if (msg == null) {
            return;
        }
        
        // 将 Protobuf 消息序列化为字节数组
        byte[] bytes = msg.toByteArray();
        
        // 写入消息长度（4字节）
        out.writeInt(bytes.length);
        // 写入消息体
        out.writeBytes(bytes);
    }
}
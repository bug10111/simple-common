package com.simple.common.logs.client.common.tcp;

import com.google.protobuf.MessageLite;
import com.simple.common.logs.proto.LogBatch;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Protobuf 批量日志编码器
 * 将 LogBatch Protobuf 消息编码为字节数组进行传输
 *
 * @author qty
 */
public class LogProtobufEncoder extends MessageToByteEncoder<LogBatch> {

    @Override
    protected void encode(ChannelHandlerContext ctx, LogBatch msg, ByteBuf out) throws Exception {
        if (msg == null) {
            return;
        }
        byte[] bytes = msg.toByteArray();
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
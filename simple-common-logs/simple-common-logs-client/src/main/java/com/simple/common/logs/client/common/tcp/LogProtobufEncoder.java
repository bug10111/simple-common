package com.simple.common.logs.client.common.tcp;

import com.simple.common.logs.proto.LogData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Protobuf 日志数据编码器
 * 将 LogData Protobuf 消息编码为字节数组进行传输
 *
 * @author Admin
 */
public class LogProtobufEncoder extends MessageToByteEncoder<LogData> {

    @Override
    protected void encode(ChannelHandlerContext ctx, LogData msg, ByteBuf out) throws Exception {
        if (msg == null) {
            return;
        }
        byte[] bytes = msg.toByteArray();
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
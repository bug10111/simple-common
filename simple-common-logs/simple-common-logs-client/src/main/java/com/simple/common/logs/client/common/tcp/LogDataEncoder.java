package com.simple.common.logs.client.common.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 日志数据编码器
 * 将LogDataEvent对象序列化为JSON字节数组
 *
 * @author qty
 */
public class LogDataEncoder extends MessageToByteEncoder<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        byte[] data = objectMapper.writeValueAsBytes(msg);
        out.writeBytes(data);
    }
}
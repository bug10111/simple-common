package com.simple.common.logs.client.common.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 日志帧编码器
 * 将字节数据封装为帧格式：帧头(4字节) + 长度(4字节) + 数据 + 帧尾(4字节)
 *
 * @author qty
 */
public class LogFrameEncoder extends MessageToByteEncoder<byte[]> {

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] data, ByteBuf out) throws Exception {
        // 帧头 4字节
        out.writeInt(0xAA55);
        // 数据长度 4字节
        out.writeInt(data.length);
        // 数据内容
        out.writeBytes(data);
        // 帧尾 4字节
        out.writeInt(0x55AA);
    }
}
package com.simple.common.logs.client.common.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * 日志帧解码器
 * 解析帧格式：帧头(4字节) + 长度(4字节) + 数据 + 帧尾(4字节)
 *
 * @author qty
 */
public class LogFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 至少需要12字节（帧头4 + 长度4 + 帧尾4）
        if (in.readableBytes() < 12) {
            return;
        }

        // 标记读位置
        in.markReaderIndex();

        // 读取帧头
        int frameHead = in.readInt();
        if (frameHead != 0xAA55) {
            // 帧头不正确，丢弃数据
            in.clear();
            return;
        }

        // 读取数据长度
        int dataLength = in.readInt();

        // 检查数据长度是否合理
        if (dataLength <= 0 || dataLength > 10 * 1024 * 1024) { // 最大10MB
            in.clear();
            return;
        }

        // 检查是否有足够的数据可读
        if (in.readableBytes() < dataLength + 4) { // 数据 + 帧尾
            in.resetReaderIndex();
            return;
        }

        // 读取数据内容
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 读取帧尾
        int frameTail = in.readInt();
        if (frameTail != 0x55AA) {
            // 帧尾不正确，丢弃数据
            return;
        }

        // 输出解码后的数据
        out.add(data);
    }
}
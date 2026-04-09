package com.simple.common.logs.server.common.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 日志帧解码器
 * 处理粘包和拆包问题
 *
 * 帧格式：
 * +--------+--------+----------+--------+
 * | 帧头   | 长度   | 数据内容 | 帧尾   |
 * | 4字节  | 4字节  | N字节    | 4字节  |
 * | 0xAA55 |        |          | 0x55AA |
 * +--------+--------+----------+--------+
 *
 * @author qty
 */
@Slf4j
public class LogFrameDecoder extends ByteToMessageDecoder {

    /**
     * 帧头标识
     */
    private static final int FRAME_HEAD = 0xAA55;

    /**
     * 帧尾标识
     */
    private static final int FRAME_TAIL = 0x55AA;

    /**
     * 最小帧长度（帧头4 + 长度4 + 帧尾4 = 12字节）
     */
    private static final int MIN_FRAME_LENGTH = 12;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 至少需要12字节才能读取帧头和长度
        if (in.readableBytes() < MIN_FRAME_LENGTH) {
            return;
        }

        // 标记读取位置
        in.markReaderIndex();

        // 读取帧头
        int frameHead = in.readInt();
        if (frameHead != FRAME_HEAD) {
            log.warn("无效的帧头: 0x{}, 期望: 0xAA55", Integer.toHexString(frameHead));
            // 跳过这个字节，继续寻找有效帧头
            in.resetReaderIndex();
            in.skipBytes(1);
            return;
        }

        // 读取数据长度
        int dataLength = in.readInt();
        if (dataLength <= 0 || dataLength > 10 * 1024 * 1024) { // 最大10MB
            log.warn("无效的数据长度: {}", dataLength);
            in.skipBytes(4); // 跳过帧尾位置
            return;
        }

        // 检查是否有足够的数据可读（数据内容 + 帧尾）
        if (in.readableBytes() < dataLength + 4) {
            // 数据不完整，重置读取位置，等待更多数据
            in.resetReaderIndex();
            return;
        }

        // 读取数据内容
        byte[] data = new byte[dataLength];
        in.readBytes(data);

        // 读取帧尾
        int frameTail = in.readInt();
        if (frameTail != FRAME_TAIL) {
            log.warn("无效的帧尾: 0x{}, 期望: 0x55AA", Integer.toHexString(frameTail));
            return;
        }

        // 输出解码后的数据
        out.add(data);
    }
}
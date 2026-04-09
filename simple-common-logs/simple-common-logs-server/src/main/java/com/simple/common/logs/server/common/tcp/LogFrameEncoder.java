
package com.simple.common.logs.server.common.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志帧编码器
 * 将数据编码为帧格式
 *
 * @author qty
 */
@Slf4j
public class LogFrameEncoder extends MessageToByteEncoder<byte[]> {

    /**
     * 帧头标识
     */
    private static final int FRAME_HEAD = 0xAA55;

    /**
     * 帧尾标识
     */
    private static final int FRAME_TAIL = 0x55AA;

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] data, ByteBuf out) throws Exception {
        // 写入帧头
        out.writeInt(FRAME_HEAD);
        
        // 写入数据长度
        out.writeInt(data.length);
        
        // 写入数据内容
        out.writeBytes(data);
        
        // 写入帧尾
        out.writeInt(FRAME_TAIL);
    }
}

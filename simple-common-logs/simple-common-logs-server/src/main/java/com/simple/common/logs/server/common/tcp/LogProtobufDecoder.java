package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.proto.LogBatch;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Protobuf 批量日志解码器
 * 将接收到的字节数组解码为 LogBatch Protobuf 消息
 * 解码格式：[长度(4字节)] + [消息体]
 *
 * @author Admin
 */
public class LogProtobufDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 至少需要4字节来存储消息长度
        if (in.readableBytes() < 4) {
            return;
        }
        
        // 标记读取位置
        in.markReaderIndex();
        
        // 读取消息长度
        int length = in.readInt();
        
        // 防止恶意数据
        if (length <= 0 || length > 10 * 1024 * 1024) {
            ctx.close();
            return;
        }
        
        // 检查是否有足够的可读字节
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }
        
        // 读取消息体
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        
        // 解析 Protobuf 消息为 LogBatch
        LogBatch batch = LogBatch.parseFrom(bytes);
        out.add(batch);
    }
}
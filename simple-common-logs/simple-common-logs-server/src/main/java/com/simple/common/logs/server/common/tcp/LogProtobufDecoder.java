package com.simple.common.logs.server.common.tcp;

import com.google.protobuf.InvalidProtocolBufferException;
import com.simple.common.logs.proto.LogBatch;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Protobuf 批量日志解码器
 * 将接收到的字节数组解码为 LogBatch Protobuf 消息
 * 解码格式：[长度(4字节)] + [消息体]
 *
 * @author qty
 */
@Slf4j
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

        // 客户端写空闲心跳帧（长度前缀为 0）：仅用于保活与半开连接探测，直接忽略
        if (length == 0) {
            log.debug("收到心跳帧, 远端: {}", ctx.channel().remoteAddress());
            return;
        }

        // 防御恶意/异常数据：长度非法时回退读指针再断连。
        // resetReaderIndex 必须先行：close 是异步的，回退前若还有字节到达，
        // 下一轮 decode 会把被拒帧的尾部字节误当作新的长度前缀，造成流永久错位
        if (length < 0 || length > LogDataEvent.MAX_FRAME_LENGTH) {
            log.warn("收到异常长度消息: length={}, 关闭连接 {}", length, ctx.channel().remoteAddress());
            in.resetReaderIndex();
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

        // 解析 Protobuf 消息为 LogBatch，解析失败主动断连：
        // 单帧脏数据直接抛 DecoderException 会以裸异常断连、现场信息不足，
        // 这里带上下文记录并有序关闭，避免污染后续帧解析
        try {
            out.add(LogBatch.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            log.warn("Protobuf 消息解析失败, length={}, 关闭连接 {}", length, ctx.channel().remoteAddress(), e);
            ctx.close();
        }
    }
}
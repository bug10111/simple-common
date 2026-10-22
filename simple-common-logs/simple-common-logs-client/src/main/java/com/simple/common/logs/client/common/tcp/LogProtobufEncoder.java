package com.simple.common.logs.client.common.tcp;

import com.simple.common.logs.proto.LogBatch;
import com.simple.common.logs.proto.common.event.LogDataEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Protobuf 批量日志编码器
 * 将 LogBatch Protobuf 消息编码为字节数组进行传输
 * 编码格式：[长度(4字节)] + [消息体]
 * <p>
 * 帧长约束与服务端 {@code LogProtobufDecoder} 共用 {@link LogDataEvent#MAX_FRAME_LENGTH}，
 * 空批次与超限批次直接跳过：服务端会将其视为异常数据断连，
 * 提前拦截可避免无谓的连接抖动与日志静默丢弃。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class LogProtobufEncoder extends MessageToByteEncoder<LogBatch> {

    @Override
    protected void encode(ChannelHandlerContext ctx, LogBatch msg, ByteBuf out) throws Exception {
        // 直接取序列化尺寸，避免 toByteArray 的全量拷贝（大批次时减少一份中间数组与 GC 压力）
        int size = msg.getSerializedSize();
        if (size == 0) {
            // 空批次无意义，且服务端会将非法帧直接断连
            log.debug("空日志批次，跳过发送");
            return;
        }
        if (size > LogDataEvent.MAX_FRAME_LENGTH) {
            // 超限批次直接丢弃并告警：与服务端帧长上限一致，避免整批被拒后断连
            log.warn("日志批次超出帧长上限 {}，已丢弃, size={}, 条数={}", LogDataEvent.MAX_FRAME_LENGTH, size, msg.getLogsCount());
            return;
        }
        out.writeInt(size);
        // 流式序列化进 ByteBuf，省去中间字节数组拷贝
        msg.writeTo(new ByteBufOutputStream(out));
    }
}

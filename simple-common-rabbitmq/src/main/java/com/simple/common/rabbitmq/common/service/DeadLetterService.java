package com.simple.common.rabbitmq.common.service;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;

/**
 * 死信队列服务接口。
 * <p>
 * 用于处理无法正常消费的消息，将死信消息持久化到失败兜底存储供后续处理或分析。
 * 默认实现 {@link com.simple.common.rabbitmq.service.DefaultDeadLetterService} 将死信消息
 * <b>写入本地兜底文件</b>（JSON 行格式，按天滚动；而非发送到死信交换机），
 * 生产环境建议实现本接口将死信落库。
 * </p>
 *
 * @author qty
 */
public interface DeadLetterService {

    /**
     * 保存死信消息
     *
     * @param exchange 交换机（投递元数据缺失时可能为 null，实现需容忍）
     * @param key      路由键（投递元数据缺失时可能为 null，实现需容忍）
     * @param queue    队列（投递元数据缺失时可能为 null，实现需容忍）
     * @param body     消息体文本：统一为消息载荷的 JSON 文本（DefaultMessage 序列化结果；
     *                 无法反序列化时为原始报文的 UTF-8 文本），实现按此约定落库/落文件
     * @param e        引发死信的异常（可能为 null）
     */
    void save(String exchange, String key, String queue, String body, Exception e);

    /**
     * 保存死信消息（便捷重载）：从 {@link Message} 提取路由元数据与消息体。
     * <p>
     * 元数据取自 ReceivedExchange/ReceivedRoutingKey/ConsumerQueue，三者均为消费者投递元数据，
     * 非容器消费或重发布场景可能为 null，本方法原样传递，实现需容忍 null。
     * 消息体转换失败（非 JSON/非 DefaultMessage 结构）不视为致命错误：降级为原始报文 UTF-8 文本，
     * 保证死信记录不丢失、不掩盖原始消费异常。
     * </p>
     *
     * @param message 死信消息载体
     * @param e       引发死信的异常
     */
    default void save(Message message, Exception e) {
        MessageProperties properties = message.getMessageProperties();
        String body;
        try {
            body = JsonUtils.toJsonStr(SerializeUtils.deserialize(message.getBody(), DefaultMessage.class));
        } catch (Exception ex) {
            // 反序列化失败保留原始报文，确保死信保存本身不抛异常掩盖原始消费异常
            byte[] raw = message.getBody();
            body = raw == null ? null : new String(raw, StandardCharsets.UTF_8);
        }
        save(properties.getReceivedExchange(), properties.getReceivedRoutingKey(),
                properties.getConsumerQueue(), body, e);
    }
}

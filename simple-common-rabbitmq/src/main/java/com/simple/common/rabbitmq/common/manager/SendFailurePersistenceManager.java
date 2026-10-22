package com.simple.common.rabbitmq.common.manager;

import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

/**
 * 发送失败持久化管理器，用于处理 RabbitMQ 发送确认失败或路由失败的消息。
 * 集成方应实现此接口，将失败消息持久化（如数据库、文件），并提供补偿重试机制。
 *
 * @author qty
 */
public interface SendFailurePersistenceManager {

    /**
     * 保存发送确认失败的信息（完整版）
     *
     * @param correlationId    消息唯一ID（可能为 null）
     * @param cause            失败原因
     * @param receivedExchange 目标交换机
     * @param receivedRoutingKey 路由键
     * @param defaultMessage   消息体对象（可能为 null）
     * @param messageBody      原始消息字节数组（兜底，优先级低于 defaultMessage）
     */
    void saveConfirmFailure(String correlationId, String cause, String receivedExchange, String receivedRoutingKey,
                            DefaultMessage defaultMessage, byte[] messageBody);

    /**
     * 保存路由失败的消息（ReturnCallback 触发）
     *
     * @param message   原始消息对象
     * @param replyCode 回复码
     * @param replyText 回复文本
     * @param exchange  交换机
     * @param routingKey 路由键
     */
    void saveReturnFailure(Message message, int replyCode, String replyText, String exchange, String routingKey);

    /**
     * 保存发送失败的消息（ConfirmCallback 收到 ack=false）- 兼容旧调用
     * <p>
     * 本方法会尽力反序列化消息体为 {@link DefaultMessage}；反序列化失败（非 JSON / 非 DefaultMessage 结构、
     * 空报文等）不视为致命错误——此时 dm 为 null，原始字节仍会作为兜底载荷传递给完整版方法，
     * 保证失败消息内容不丢失。反序列化是尽力而为的辅助，不应反噬失败记录本身。
     * </p>
     *
     * @param correlationId 消息唯一ID
     * @param message       原始消息对象（可能为 null）
     * @param cause         失败原因
     */
    default void saveConfirmFailure(String correlationId, Message message, String cause) {
        String exchange = null;
        String routingKey = null;
        DefaultMessage dm = null;
        byte[] body = null;
        if (message != null) {
            body = message.getBody();
            try {
                dm = SerializeUtils.deserialize(body, DefaultMessage.class);
            } catch (Exception e) {
                // 反序列化失败降级为 null：完整版实现会用原始字节 body 兜底，不掩盖原始发送失败原因
            }
            exchange = message.getMessageProperties().getReceivedExchange();
            routingKey = message.getMessageProperties().getReceivedRoutingKey();
        }
        // 调用完整版方法
        saveConfirmFailure(correlationId, cause, exchange, routingKey, dm, body);
    }

    /**
     * 简化版方法（用于回调中无 Message 对象、仅有 JSON 文本时）
     * <p>
     * 兼容旧调用：将 JSON 文本按 UTF-8 编码后作为 messageBody 兜底传递，
     * 避免失败消息内容被静默丢弃；建议实现类优先使用完整版方法。
     * </p>
     *
     * @param correlationId 消息唯一ID
     * @param cause         失败原因
     * @param exchange      交换机
     * @param routingKey    路由键
     * @param jsonStr       消息 JSON 文本（可能为 null）
     */
    default void saveConfirmFailure(String correlationId, String cause, String exchange, String routingKey,
                                    String jsonStr) {
        saveConfirmFailure(correlationId, cause, exchange, routingKey, null,
                jsonStr == null ? null : jsonStr.getBytes(StandardCharsets.UTF_8));
    }
}

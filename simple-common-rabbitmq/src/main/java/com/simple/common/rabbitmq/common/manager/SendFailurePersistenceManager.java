package com.simple.common.rabbitmq.common.manager;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;

/**
 * 发送失败持久化管理器，用于处理 RabbitMQ 发送确认失败或路由失败的消息。
 * 集成方应实现此接口，将失败消息持久化（如数据库、文件），并提供补偿重试机制。
 *
 * @author qty
 */
public interface SendFailurePersistenceManager {

    /**
     * 保存到交换机失败的信息（完整版）
     *
     * @param correlationId      消息唯一ID（可能为 null）
     * @param cause              失败原因
     * @param receivedExchange   目标交换机
     * @param receivedRoutingKey key
     * @param defaultMessage     消息体对象（可能为 null）
     * @param messageBody        原始消息字节数组（兜底）
     */
    void saveConfirmFailure(String correlationId, String cause, String receivedExchange, String receivedRoutingKey,
                            DefaultMessage defaultMessage, byte[] messageBody);

    /**
     * 保存路由失败的消息（ReturnCallback 触发）
     *
     * @param message    原始消息对象
     * @param replyCode  回复码
     * @param replyText  回复文本
     * @param exchange   交换机
     * @param routingKey 路由键
     */
    void saveReturnFailure(Message message, int replyCode, String replyText, String exchange, String routingKey);

    /**
     * 保存发送失败的消息（ConfirmCallback 收到 ack=false）- 兼容旧调用
     *
     * @param correlationId 消息唯一ID
     * @param message       原始消息对象（可能为 null）
     * @param cause         失败原因
     */
    default void saveConfirmFailure(String correlationId, Message message, String cause) {
        String jsonStr = null;
        String exchange = null;
        String routingKey = null;
        DefaultMessage dm = null;
        byte[] body = null;
        if (message != null) {
            dm = SerializeUtils.deserialize(message.getBody(), DefaultMessage.class);
            jsonStr = JsonUtils.toJsonStr(dm);
            exchange = message.getMessageProperties().getReceivedExchange();
            routingKey = message.getMessageProperties().getReceivedRoutingKey();
            body = message.getBody();
        }
        // 调用完整版方法
        saveConfirmFailure(correlationId, cause, exchange, routingKey, dm, body);
    }

    /**
     * 简化版方法（用于回调中无 DefaultMessage 对象时）
     * 实现类必须 override 此方法，根据实际需求处理
     */
    default void saveConfirmFailure(String correlationId, String cause, String exchange, String routingKey,
                                    String jsonStr) {
        // 此方法保留用于兼容，但建议实现类使用完整版方法
        saveConfirmFailure(correlationId, cause, exchange, routingKey, null, null);
    }
}
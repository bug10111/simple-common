package com.simple.common.rabbitmq.common.manager;

import org.springframework.amqp.core.Message;

/**
 * 发送失败持久化管理器，用于处理 RabbitMQ 发送确认失败或路由失败的消息。
 * 集成方应实现此接口，将失败消息持久化（如数据库、文件），并提供补偿重试机制。
 *
 * @author qty
 */
public interface SendFailurePersistenceManager {

    /**
     * 保存发送失败的消息（ConfirmCallback 收到 ack=false）
     *
     * @param correlationId 消息唯一ID
     * @param message       原始消息对象
     * @param cause         失败原因
     */
    void saveConfirmFailure(String correlationId, Message message, String cause);

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
}
package com.simple.common.rabbitmq.common.manager;

import com.rabbitmq.client.Channel;

/**
 * RabbitMQ消息确认管理器接口。
 * <p>
 * 用于管理RabbitMQ消息的确认和拒绝操作。
 * 默认实现 {@link com.simple.common.rabbitmq.manager.DefaultAckRMQManager} 提供了
 * 标准的消息确认和拒绝逻辑。
 * </p>
 *
 * @author qty
 */
public interface AckRMQManager {

    /**
     * 确认消息已成功消费。
     * <p>
     * 告知RabbitMQ该消息已成功处理，可以从队列中移除。
     * </p>
     *
     * @param channel     RabbitMQ通道
     * @param deliveryTag 消息投递标签，用于标识消息
     */
    void basicAck(Channel channel, Long deliveryTag);

    /**
     * 拒绝消息。
     * <p>
     * 告知RabbitMQ该消息处理失败，可选择是否重新入队。
     * </p>
     *
     * @param channel     RabbitMQ通道
     * @param deliveryTag 消息投递标签，用于标识消息
     * @param requeue     是否重新入队：true-消息重新入队等待再次消费；false-消息直接丢弃或进入死信队列
     */
    void basicNack(Channel channel, Long deliveryTag, boolean requeue);
}
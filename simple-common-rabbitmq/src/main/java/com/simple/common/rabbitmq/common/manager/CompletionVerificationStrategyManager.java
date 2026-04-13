package com.simple.common.rabbitmq.common.manager;

import org.springframework.amqp.core.Message;

/**
 * 业务完成校验策略，每个队列可对应不同的校验实现。
 *
 * @author qty
 */
public interface CompletionVerificationStrategyManager {

    /**
     * 判断指定业务ID对应的业务是否已完成
     *
     * @param businessId 业务方法返回的唯一标识
     * @param message    原始消息对象
     * @return true-已完成，可安全丢弃消息；false-未完成，应重新消费
     */
    boolean isCompleted(String businessId, Message message);

    /**
     * 返回该策略支持的队列名称
     */
    String getQueueName();
}
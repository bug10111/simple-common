package com.simple.common.rabbitmq.common.manager;

import com.rabbitmq.client.Channel;

/**
 * Created with IntelliJ IDEA
 * Description: 提交相关接口
 *
 * @author qty
 */
public interface AckRMQManager {

    /**
     * 设置手动应答消息
     *
     * @param channel     信道
     * @param deliveryTag deliveryTag
     */
    void basicAck(Channel channel, Long deliveryTag);

    /**
     * 设置手动拒绝消息
     *
     * @param channel     信道
     * @param deliveryTag 消息
     * @param b           是否放回队列
     */
    void basicNack(Channel channel, Long deliveryTag, boolean b);
}
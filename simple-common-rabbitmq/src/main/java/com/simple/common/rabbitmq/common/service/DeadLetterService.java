package com.simple.common.rabbitmq.common.service;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.common.config.DefaultMessage;
import org.springframework.amqp.core.Message;

/**
 * Created with IntelliJ IDEA
 * Description: 消息消费失败的保存方法
 *
 * @author qty
 */
public interface DeadLetterService {

    /**
     * 保存失败的消息
     *
     * @param exchange 交换机
     * @param key      key
     * @param queue    队列
     * @param body     内容
     * @param e        异常信息
     */
    void save(String exchange, String key, String queue, String body, Exception e);

    /**
     * 保存失败消息
     *
     * @param message 消息载体
     * @param e       失败得到异常
     */
    default void save(Message message, Exception e) {
        save(message.getMessageProperties().getReceivedExchange(), message.getMessageProperties().getReceivedRoutingKey(),
             message.getMessageProperties().getConsumerQueue(), JsonUtils.toJsonStr(SerializeUtils.deserialize(message.getBody(), DefaultMessage.class)), e);
    }

}

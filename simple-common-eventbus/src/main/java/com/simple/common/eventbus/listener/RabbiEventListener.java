package com.simple.common.eventbus.listener;

import com.rabbitmq.client.Channel;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import com.simple.common.eventbus.util.EventThreadLocalUtils;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class RabbiEventListener {

    @Autowired
    private EventHandlerManager eventHandlerManager;

    @RabbitListener(queues = "#{T(com.simple.common.eventbus.util.MqNameUtil).queueName(applicationProperties.getName())}", concurrency = "#{eventProperties.getConcurrency()}")
    @RabbitMqConsumption
    public void receiveMessage(Message message, Channel channel) throws Exception {
        EventData eventData;
        try {
            eventData = SerializeUtils.deserialize(message.getBody(), EventData.class);
            if (eventData == null) {
                log.error("反序列化事件数据失败，消息体为空");
                throw new IllegalArgumentException("反序列化事件数据失败，消息体为空");
            }

            if (eventData.getMsgData() == null) {
                log.error("事件消息体 msgData 为空，无法执行处理器。事件名称: {}", eventData.getEventName());
                return;
            }

            // ThreadLocal 设置已移至 EventHandlerManager 内部统一管理
            eventHandlerManager.handler(eventData);
        } catch (Exception e) {
            log.error("处理事件消息失败", e);
            throw e;
        }
    }
}
package com.simple.common.eventbus.listener;

import com.rabbitmq.client.Channel;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import com.simple.common.eventbus.util.EventThreadLocalUtils;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Component
public class RabbiEventListener {

    @Autowired
    private EventHandlerManager eventHandlerManager;

    @RabbitListener(queues = "#{T(com.simple.common.eventbus.util.MqNameUtil).queueName(applicationProperties.getName())}", concurrency = "#{eventProperties.getConcurrency()}")
    @RabbitMqConsumption
    public void receiveMessage(Message message, Channel channel) {
        EventData eventData = SerializeUtils.deserialize(message.getBody(), EventData.class);
        EventThreadLocalUtils.put(eventData.getClass().getName(), eventData);
        eventHandlerManager.handler(eventData);
        EventThreadLocalUtils.clear();
    }
}

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
        EventData eventData = null;
        try {
            eventData = SerializeUtils.deserialize(message.getBody(), EventData.class);
            if (eventData == null) {
                log.error("反序列化事件数据失败，消息体为空，消息将被拒绝且不重新入队");
                return;
            }

            EventThreadLocalUtils.put(eventData.getClass().getName(), eventData);
            eventHandlerManager.handler(eventData);
        } catch (Exception e) {
            log.error("处理事件消息失败", e);
            throw e;
        } finally {
            try {
                EventThreadLocalUtils.clear();
            } catch (Exception e) {
                log.warn("清理ThreadLocal资源失败", e);
            }
        }
    }
}
package com.simple.common.rabbitmq.service;

import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.common.config.DefaultMessage;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Service
public class DefaultRabbitMqService implements RabbitMqService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey) {
        defaultMessage.initialization();
        Message message = MessageBuilder.withBody(SerializeUtils.serialize(defaultMessage)).build();
        message.getMessageProperties().setCorrelationId(defaultMessage.getId());
        CorrelationData data = new CorrelationData(defaultMessage.getId());
        rabbitTemplate.send(exchangeName, routingKey, message, data);
    }

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit) {
        defaultMessage.initialization();
        Message message = MessageBuilder.withBody(SerializeUtils.serialize(defaultMessage)).build();
        message.getMessageProperties().setCorrelationId(defaultMessage.getId());
        message.getMessageProperties().setDelayLong(toMilliseconds(time, timeUnit));
        CorrelationData data = new CorrelationData(defaultMessage.getId());
        rabbitTemplate.send(exchangeName, routingKey, message, data);
    }
}

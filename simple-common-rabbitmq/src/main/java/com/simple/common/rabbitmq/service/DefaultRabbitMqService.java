package com.simple.common.rabbitmq.service;

import com.simple.common.rabbitmq.common.config.DefaultMessage;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultRabbitMqService implements RabbitMqService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey) {
        defaultMessage.initialization();
        CorrelationData data = new CorrelationData(defaultMessage.getId());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, defaultMessage, message -> {
            message.getMessageProperties().setCorrelationId(defaultMessage.getId());
            message.getMessageProperties().setContentType("application/json");
            return message;
        }, data);
    }

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit) {
        defaultMessage.initialization();
        CorrelationData data = new CorrelationData(defaultMessage.getId());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, defaultMessage, message -> {
            message.getMessageProperties().setCorrelationId(defaultMessage.getId());
            message.getMessageProperties().setDelayLong(toMilliseconds(time, timeUnit));
            message.getMessageProperties().setContentType("application/json");
            return message;
        }, data);
    }
}
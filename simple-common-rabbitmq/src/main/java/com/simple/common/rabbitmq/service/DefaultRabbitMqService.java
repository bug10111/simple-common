package com.simple.common.rabbitmq.service;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.entity.EnhancedCorrelationData;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 服务默认实现，提供消息发送能力。
 * 包含异常捕获与性能监控日志。
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultRabbitMqService implements RabbitMqService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey) {
        long start = System.currentTimeMillis();
        try {
            defaultMessage.initialization();
            String msgId = defaultMessage.getId();
            // 构建携带消息详情的 CorrelationData
            EnhancedCorrelationData data = EnhancedCorrelationData.of(
                    msgId, defaultMessage, exchangeName, routingKey, null);

            rabbitTemplate.convertAndSend(exchangeName, routingKey, defaultMessage, message -> {
                message.getMessageProperties().setCorrelationId(msgId);
                message.getMessageProperties().setContentType("application/json");
                // 保存消息体字节数组供确认回调使用
                data.setMessageBody(message.getBody());
                return message;
            }, data);

            if (log.isDebugEnabled()) {
                log.debug("消息发送成功，exchange={}, routingKey={}, msgId={}, 耗时={}ms",
                        exchangeName, routingKey, msgId, System.currentTimeMillis() - start);
            }
        } catch (AmqpException e) {
            log.error("消息发送失败，exchange={}, routingKey={}, msgId={}, 耗时={}ms",
                    exchangeName, routingKey, defaultMessage.getId(), System.currentTimeMillis() - start, e);
            throw e;
        }
    }

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit) {
        long start = System.currentTimeMillis();
        try {
            defaultMessage.initialization();
            String msgId = defaultMessage.getId();
            EnhancedCorrelationData data = EnhancedCorrelationData.of(
                    msgId, defaultMessage, exchangeName, routingKey, null);

            rabbitTemplate.convertAndSend(exchangeName, routingKey, defaultMessage, message -> {
                message.getMessageProperties().setCorrelationId(msgId);
                message.getMessageProperties().setDelayLong(toMilliseconds(time, timeUnit));
                message.getMessageProperties().setContentType("application/json");
                data.setMessageBody(message.getBody());
                return message;
            }, data);

            if (log.isDebugEnabled()) {
                log.debug("延迟消息发送成功，exchange={}, routingKey={}, msgId={}, delay={}ms, 耗时={}ms",
                        exchangeName, routingKey, msgId, toMilliseconds(time, timeUnit),
                        System.currentTimeMillis() - start);
            }
        } catch (AmqpException e) {
            log.error("延迟消息发送失败，exchange={}, routingKey={}, msgId={}, 耗时={}ms",
                    exchangeName, routingKey, defaultMessage.getId(), System.currentTimeMillis() - start, e);
            throw e;
        }
    }
}
package com.simple.common.rabbitmq.common.config;

import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 生产者配置：消息转换器 + 发送确认 + 路由失败回调
 *
 * @author qty
 */
@Slf4j
@Configuration
public class RabbitMqProducerConfig {

    @Autowired
    private SendFailurePersistenceManager sendFailurePersistenceManager;

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 使用 Jackson JSON 转换器
        template.setMessageConverter(new Jackson2JsonMessageConverter());

        // 发布确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                String id = correlationData != null ? correlationData.getId() : null;
                // 由于 CorrelationData 未携带原始 Message，此处 message 参数传 null
                sendFailurePersistenceManager.saveConfirmFailure(id, null, cause);
            }
        });

        // 路由失败回调
        template.setReturnsCallback(returned -> {
            Message message = returned.getMessage();
            sendFailurePersistenceManager.saveReturnFailure(
                    message,
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey()
            );
        });

        // 必须设置为 true，否则 ReturnCallback 不生效
        template.setMandatory(true);

        return template;
    }
}
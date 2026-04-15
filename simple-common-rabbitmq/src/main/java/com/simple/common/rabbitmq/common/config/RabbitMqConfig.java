package com.simple.common.rabbitmq.common.config;

import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 延迟重试配置
 * 使用 RabbitMQ 延迟交换机插件（rabbitmq_delayed_message_exchange）
 *
 * @author qty
 */
@EnableRabbit
@Configuration
@ComponentScan(basePackages = { "com.simple.common.rabbitmq" })
public class RabbitMqConfig {

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

    @Autowired
    private RabbitMqProperties rabbitMqProperties;

    /**
     * 定义延迟交换机，类型为 x-delayed-message
     * 使用 CustomExchange 来声明，底层消息路由类型为 direct
     */
    @Bean("simpleDelayedRetryExchange")
    public Exchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(rabbitMqProperties.getDelayedExchange(), "x-delayed-message", true, false, args);
    }

    // 实际使用时，请根据业务需求在各自的配置类中绑定，此处仅作演示。
    // @Bean
    // public Binding delayedOrderBinding(Queue orderQueue, Exchange delayedExchange) {
    //     return BindingBuilder.bind(orderQueue).to(delayedExchange).with("order.routing.key").noargs();
    // }
}
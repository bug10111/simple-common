package com.simple.common.rabbitmq.common.config;

import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
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
public class DelayedRetryConfig {

    @Autowired
    private RabbitMqProperties advancedProperties;

    /**
     * 定义延迟交换机，类型为 x-delayed-message
     * 使用 CustomExchange 来声明，底层消息路由类型为 topic
     */
    @Bean
    public Exchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");
        return new CustomExchange(advancedProperties.getDelayedExchange(), "x-delayed-message", true, false, args);
    }

    /**
     * 示例绑定：将订单队列绑定到延迟交换机
     * 实际使用时，请根据业务需求在各自的配置类中绑定，此处仅作演示。
     */
    @Bean
    public Binding delayedOrderBinding(Queue orderQueue, Exchange delayedExchange) {
        return BindingBuilder.bind(orderQueue).to(delayedExchange).with("order.routing.key").noargs();
    }
}
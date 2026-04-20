package com.simple.common.eventbus.common.config;

import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.util.MqNameUtil;
import com.simple.common.rabbitmq.common.config.RabbitMqConfig;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ异步事件队列相关声明和创建
 *
 * @author qty
 */
@ComponentScan(basePackages = { "com.simple.common.eventbus" })
@Component
public class RabbiEventConfig {

    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * 声明本服务事件队列
     *
     * @return Queue
     */
    @Bean("simpleEventQueue")
    public Queue simpleEventQueue() {
        return new Queue(MqNameUtil.queueName(applicationProperties.getName()), true, false, false);
    }

    /**
     * 正常事件交换机（Direct类型）
     *
     * @return DirectExchange
     */
    @Bean("simpleEventExchange")
    public DirectExchange simpleEventExchange() {
        return new DirectExchange(MqNameUtil.exchangeName(applicationProperties.getName()), true, false);
    }

    /**
     * 延迟事件交换机（基于rabbitmq_delayed_message_exchange插件）
     *
     * @return Exchange
     */
    @Bean("simpleEventDelayExchange")
    public Exchange simpleEventDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(MqNameUtil.delayExchangeName(applicationProperties.getName()), "x-delayed-message", true, false, args);
    }

    /**
     * 全局事件交换机（Fanout类型）
     *
     * @return FanoutExchange
     */
    @Bean("simpleEventAllExchange")
    public FanoutExchange simpleEventAllExchange() {
        return new FanoutExchange(MqNameUtil.exchangeName(EventConstant.TARGET_ALL_X), true, false);
    }

    /**
     * 全局延迟事件交换机（Fanout + 延迟）
     *
     * @return Exchange
     */
    @Bean("simpleEventAllDelayExchange")
    public Exchange simpleEventAllDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "fanout");
        return new CustomExchange(MqNameUtil.delayExchangeName(EventConstant.TARGET_ALL_X), "x-delayed-message", true, false, args);
    }

    /**
     * 绑定本服务队列到本服务正常交换机
     *
     * @param queue    注入本服务队列
     * @param exchange 注入本服务正常交换机
     * @return Binding
     */
    @Bean("bindingSimpleEventExchange")
    public Binding bindingSimpleEventExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleEventExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MqNameUtil.keyName(applicationProperties.getName()));
    }

    /**
     * 绑定本服务队列到本服务延迟交换机
     *
     * @param queue    注入本服务队列
     * @param exchange 注入本服务延迟交换机
     * @return Binding
     */
    @Bean("bindingDelaySimpleEventExchange")
    public Binding bindingDelaySimpleEventExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleEventDelayExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MqNameUtil.keyName(applicationProperties.getName())).noargs();
    }

    /**
     * 绑定本服务队列到全局正常交换机
     *
     * @param queue    注入本服务队列
     * @param exchange 注入全局正常交换机
     * @return Binding
     */
    @Bean("bindingSimpleEventAllExchange")
    public Binding bindingSimpleEventAllExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleEventAllExchange") FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    /**
     * 绑定本服务队列到全局延迟交换机
     *
     * @param queue    注入本服务队列
     * @param exchange 注入全局延迟交换机
     * @return Binding
     */
    @Bean("bindingDelaySimpleEventAllExchange")
    public Binding bindingDelaySimpleEventAllExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleEventAllDelayExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("").noargs();
    }

    /**
     * 绑定本服务队列到重试延迟交换机（用于消息消费失败后的延迟重试）
     *
     * @param queue    注入本服务队列
     * @param exchange 注入重试延迟交换机（由父类RabbitMqConfig定义）
     * @return Binding
     */
    @Bean("bindingSimpleDelayedRetryExchange")
    public Binding bindingSimpleDelayedRetryExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleDelayedRetryExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MqNameUtil.keyName(applicationProperties.getName())).noargs();
    }
}
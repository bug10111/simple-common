package com.simple.common.eventbus.common.config;

import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.util.MqNameUtil;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ异步事件队列相关声明和创建
 * <p>装配条件：仅当 simple.event.type=mq（或缺省默认 mq）时才声明队列/交换机/绑定等 MQ 组件，
 * 同步模式（simple.event.type=sync）下本类 MQ Bean 全部跳过，纯同步使用无需 RabbitMQ 连接；
 * 类上的 @ComponentScan 保持无条件执行，保证事件模块自身组件（事件管理器、同步/MQ 事件执行器等）正常装配</p>
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
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
    public Queue simpleEventQueue() {
        return new Queue(MqNameUtil.queueName(applicationProperties.getName()), true, false, false);
    }

    /**
     * 正常事件交换机（Direct类型）
     *
     * @return DirectExchange
     */
    @Bean("simpleEventExchange")
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
    public DirectExchange simpleEventExchange() {
        return new DirectExchange(MqNameUtil.exchangeName(applicationProperties.getName()), true, false);
    }

    /**
     * 延迟事件交换机（基于rabbitmq_delayed_message_exchange插件）
     *
     * @return Exchange
     */
    @Bean("simpleEventDelayExchange")
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
    public FanoutExchange simpleEventAllExchange() {
        return new FanoutExchange(MqNameUtil.exchangeName(EventConstant.TARGET_ALL_X), true, false);
    }

    /**
     * 全局延迟事件交换机（Fanout + 延迟）
     *
     * @return Exchange
     */
    @Bean("simpleEventAllDelayExchange")
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
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
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
    public Binding bindingDelaySimpleEventAllExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleEventAllDelayExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("").noargs();
    }

    /**
     * 绑定本服务队列到重试延迟交换机（用于消息消费失败后的延迟重试，重试交换机由 simple-common-rabbitmq 的 RabbitMqConfig 定义）
     *
     * @param queue    注入本服务队列
     * @param exchange 注入重试延迟交换机
     * @return Binding
     */
    @Bean("bindingSimpleDelayedRetryExchange")
    @ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
    public Binding bindingSimpleDelayedRetryExchange(@Qualifier("simpleEventQueue") Queue queue, @Qualifier("simpleDelayedRetryExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(MqNameUtil.keyName(applicationProperties.getName())).noargs();
    }
}

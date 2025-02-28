package com.simple.common.eventbus.common.config;

import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.util.MqNameUtil;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: RabbitMQ异步事件队列相关声明和创建
 *
 * @author 兄台丶请冷静
 */
@ComponentScan(basePackages = { "com.simple.common.eventbus" })
@Component
public class RabbiEventConfig {

    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * 声明一队列
     *
     * @return Queue
     */
    @Bean("simpleEventQueue")
    public Queue simpleEventQueue() {
        return new Queue(MqNameUtil.queueName(applicationProperties.getName()), true, false, false);
    }

    /**
     * 正常事件交换机
     */
    @Bean("simpleEventExchange")
    public DirectExchange simpleEventExchange() {
        return new DirectExchange(MqNameUtil.exchangeName(applicationProperties.getName()), true, false);
    }

    /**
     * 延迟事件交换机
     */
    @Bean("simpleEventDelayExchange")
    public DirectExchange simpleEventDelayExchange() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        DirectExchange directExchange = new DirectExchange(MqNameUtil.delayExchangeName(applicationProperties.getName()), true, false, map);
        directExchange.setDelayed(true);
        return directExchange;
    }

    /**
     * 全局事件交换机
     */
    @Bean("simpleEventAllExchange")
    public FanoutExchange simpleEventAllExchange() {
        return new FanoutExchange(MqNameUtil.exchangeName(EventConstant.TARGET_ALL_X), true, false);
    }

    /**
     * 全局延迟事件交换机
     */
    @Bean("simpleEventAllDelayExchange")
    public FanoutExchange simpleEventAllDelayExchange() {
        Map<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        FanoutExchange fanoutExchange = new FanoutExchange(MqNameUtil.delayExchangeName(EventConstant.TARGET_ALL_X), true, false, map);
        fanoutExchange.setDelayed(true);
        return fanoutExchange;
    }

    /**
     * 绑定正常事件
     */
    @Bean("bindingSimpleEventExchange")
    public Binding bindingSimpleEventExchange() {
        return new Binding(MqNameUtil.queueName(applicationProperties.getName()), Binding.DestinationType.QUEUE,
                           MqNameUtil.exchangeName(applicationProperties.getName()), MqNameUtil.keyName(applicationProperties.getName()), null);
    }

    /**
     * 绑定延迟事件
     */
    @Bean("bindingDelaySimpleEventAllExchange")
    public Binding bindingDelaySimpleEventAllExchange() {
        return new Binding(MqNameUtil.queueName(applicationProperties.getName()), Binding.DestinationType.QUEUE,
                           MqNameUtil.delayExchangeName(applicationProperties.getName()), MqNameUtil.keyName(applicationProperties.getName()), null);
    }

    /**
     * 绑定全局正常事件
     */
    @Bean("bindingSimpleEventAllExchange")
    public Binding bindingSimpleEventAllExchange() {
        return new Binding(MqNameUtil.queueName(applicationProperties.getName()), Binding.DestinationType.QUEUE,
                           MqNameUtil.exchangeName(EventConstant.TARGET_ALL_X), MqNameUtil.keyName(EventConstant.TARGET_ALL_X), null);
    }

    /**
     * 绑定全局延迟事件
     */
    @Bean("bindingDelaySimpleEventExchange")
    public Binding bindingDelaySimpleEventExchange() {
        return new Binding(MqNameUtil.queueName(applicationProperties.getName()), Binding.DestinationType.QUEUE,
                           MqNameUtil.delayExchangeName(EventConstant.TARGET_ALL_X), MqNameUtil.keyName(EventConstant.TARGET_ALL_X), null);
    }
}

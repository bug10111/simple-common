package com.simple.common.rabbitmq.common.service.process;

import com.rabbitmq.client.Channel;
import com.simple.common.core.common.service.process.BasProcessService;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import org.springframework.amqp.core.Message;

/**
 * Created with IntelliJ IDEA
 * Description: 定义RabbitMq消费前置流程接口
 *
 * @author qty
 */
public interface RabbitMqProcess extends BasProcessService {

    /**
     * 执行流程
     *
     * @param message             msg载体
     * @param channel             通道
     * @param rabbitMqConsumption 注解
     */
    void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption);

}
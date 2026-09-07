package com.simple.common.rabbitmq.service.process;

import com.rabbitmq.client.Channel;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.rabbitmq.common.enums.RMQKindProcess;
import com.simple.common.rabbitmq.common.service.process.RabbitMqProcess;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消息消费责任链默认处理器（占位实现）
 * <p>
 * 绑定占位枚举 {@link RMQKindProcess#TEST}（execute=false），不会参与责任链实际执行；
 * execution 为空实现，集成方扩展前置处理时应新增自定义处理器，而非修改此占位实现。
 * </p>
 *
 * @author qty
 */
@Component
public class DefaultRMQProcess implements RabbitMqProcess {

    @Override
    public DefaultKindProcess getProcess() {
        return RMQKindProcess.TEST;
    }

    @Override
    public void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption) {
        // 可在此处添加默认前置处理逻辑
    }

}
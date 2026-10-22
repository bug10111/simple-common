package com.simple.common.rabbitmq.service.process;

import com.rabbitmq.client.Channel;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.rabbitmq.common.enums.RMQKindProcess;
import com.simple.common.rabbitmq.common.service.process.RabbitMqProcess;
import org.springframework.amqp.core.Message;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class DefaultRMQProcess implements RabbitMqProcess {

    @Override
    public DefaultKindProcess getProcess() {
        return RMQKindProcess.TEST;
    }

    @Override
    public void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption) {
        log.debug("占位前置处理器被触发（正常配置下不会执行，出现本日志说明枚举配置被误开启），queue={}", message.getMessageProperties().getConsumerQueue());
    }

}
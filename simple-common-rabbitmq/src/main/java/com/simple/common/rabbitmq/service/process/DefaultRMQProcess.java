package com.simple.common.rabbitmq.service.process;

import com.rabbitmq.client.Channel;
import com.simple.common.core.common.enums.process.DefaultKindProcess;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.rabbitmq.common.enums.RMQKindProcess;
import com.simple.common.rabbitmq.common.service.process.RabbitMqProcess;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 重复消费
 *
 * @author 兄台丶请冷静
 */
@Component
public class DefaultRMQProcess implements RabbitMqProcess {

    @Override
    public DefaultKindProcess getProcess() {
        return RMQKindProcess.TEST;
    }

    @Override
    public void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption) {

    }

}

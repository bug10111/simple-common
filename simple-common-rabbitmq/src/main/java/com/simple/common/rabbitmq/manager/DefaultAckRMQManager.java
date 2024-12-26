package com.simple.common.rabbitmq.manager;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.common.manager.AckRMQManager;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class DefaultAckRMQManager implements AckRMQManager {

    @Override
    public void basicAck(Channel channel, Long deliveryTag) throws IOException {

        /*
         * 手动应答:
         * 1.消息标记tag-类似唯一主键
         * 2.是否批量应答信道里的消息，一般使用false，true会提升速度  但是会有丢失消息的风险
         * */
        channel.basicAck(deliveryTag, false);
    }

    @Override
    public void basicNack(Channel channel, Long deliveryTag, boolean b) throws IOException {

        /*
         * 手动应答:
         * 1.消息标记tag-类似唯一主键
         * 2.是否批量应答信道里的消息，一般使用false
         * 3.是否放回队列
         * */
        channel.basicNack(deliveryTag, false, b);
    }
}

package com.simple.common.rabbitmq.manager;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.common.manager.AckRMQManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * RabbitMQ 消息确认管理器默认实现
 * <p>
 * 确认/拒绝失败（IOException、通道已关闭等）以未检查异常向上传播（契约见接口 javadoc），
 * 便于调用方感知并记录——未送达的 ack/nack 会导致消息被重新投递。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultAckRMQManager implements AckRMQManager {

    @Override
    @SneakyThrows
    public void basicAck(Channel channel, long deliveryTag, boolean multiple) {
        Objects.requireNonNull(channel, "channel 不能为 null");
        /*
         * 手动应答:
         * 1.消息标记tag-类似唯一主键
         * 2.是否批量应答信道里的消息，一般使用false，true会提升速度  但是会有丢失消息的风险
         */
        channel.basicAck(deliveryTag, multiple);
    }

    @Override
    @SneakyThrows
    public void basicNack(Channel channel, long deliveryTag, boolean requeue) {
        Objects.requireNonNull(channel, "channel 不能为 null");
        /*
         * 手动应答:
         * 1.消息标记tag-类似唯一主键
         * 2.是否批量应答信道里的消息，一般使用false
         * 3.是否放回队列
         */
        channel.basicNack(deliveryTag, false, requeue);
    }
}

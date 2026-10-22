package com.simple.common.rabbitmq.common.manager;

import com.rabbitmq.client.Channel;

/**
 * RabbitMQ消息确认管理器接口。
 * <p>
 * 用于管理RabbitMQ消息的确认和拒绝操作。
 * 默认实现 {@link com.simple.common.rabbitmq.manager.DefaultAckRMQManager} 提供
 * 标准的消息确认和拒绝逻辑。
 * </p>
 *
 * <h3>错误处理契约：</h3>
 * <p>
 * 底层 {@code Channel.basicAck/basicNack} 声明抛出受检异常 {@link java.io.IOException}
 * （以及通道已关闭时的 {@link com.rabbitmq.client.AlreadyClosedException}）；
 * 实现方以未检查异常向上传播（实现使用 @SneakyThrows），接口不改变方法签名。
 * 调用方必须意识到<b>确认动作可能失败</b>：未成功送达的 ack/nack 会导致消息被代理重新投递，
 * 调用方应捕获运行时异常并记录日志，不得静默吞掉。
 * </p>
 *
 * @author qty
 */
public interface AckRMQManager {

    /**
     * 确认消息已成功消费。
     * <p>
     * 告知RabbitMQ该消息已成功处理，可以从队列中移除。
     * </p>
     *
     * @param channel     RabbitMQ通道，不允许为 null
     * @param deliveryTag 消息投递标签（由代理下发，必为正数，不存在 null 语义）
     * @param multiple    是否批量确认：false-仅确认本条；true-批量确认小于等于该 deliveryTag 的全部未确认消息
     *                    （批量确认有误确认未处理消息的风险，框架内默认使用单条确认）
     */
    void basicAck(Channel channel, long deliveryTag, boolean multiple);

    /**
     * 确认消息已成功消费（单条确认便捷重载）。
     *
     * @param channel     RabbitMQ通道，不允许为 null
     * @param deliveryTag 消息投递标签
     */
    default void basicAck(Channel channel, long deliveryTag) {
        basicAck(channel, deliveryTag, false);
    }

    /**
     * 拒绝消息。
     * <p>
     * 告知RabbitMQ该消息处理失败。{@code requeue=false} 时消息被丢弃或路由到死信交换机——
     * 对于确定性失败（毒消息）必须使用 requeue=false，否则会陷入无限重投循环；
     * 期望有限次重试的场景应配合框架的延迟重试机制（RetryMessageManager），而不是盲目 requeue=true。
     * </p>
     *
     * @param channel     RabbitMQ通道，不允许为 null
     * @param deliveryTag 消息投递标签
     * @param requeue     是否重新入队：true-消息重新入队等待再次消费；false-消息被丢弃或进入死信队列
     */
    void basicNack(Channel channel, long deliveryTag, boolean requeue);
}

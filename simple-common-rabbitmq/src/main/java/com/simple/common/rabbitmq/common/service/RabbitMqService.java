package com.simple.common.rabbitmq.common.service;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;

import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 消息队列服务接口。
 * <p>
 * 提供消息发送功能,支持即时消息和延迟消息。
 * 默认实现 {@link com.simple.common.rabbitmq.service.DefaultRabbitMqService} 基于 Spring AMQP 实现。
 * </p>
 *
 * <h3>性能说明：</h3>
 * <p>
 * 在 8核2G 配置下,每秒可处理约 5万条消息,有调优空间。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>异步解耦：订单创建后发送通知、更新库存等</li>
 *   <li>流量削峰：秒杀活动、批量处理等</li>
 *   <li>延迟任务：订单超时取消、定时提醒等(需安装 rabbitmq-delayed-message-exchange 插件)</li>
 * </ul>
 *
 * @author qty
 */
public interface RabbitMqService {

    /**
     * 发送即时消息
     * <p>
     * 消息会立即投递到指定的交换机和队列,消费者会尽快处理。
     * 适用于对实时性要求较高的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * DefaultMessage message = new DefaultMessage();
     * message.setData(orderInfo);
     * message.setBizType("order");
     * rabbitMqService.sendMsg(message, "order.exchange", "order.created");
     * }</pre>
     *
     * @param defaultMessage 消息体,包含业务数据和元信息
     * @param exchangeName   交换机名称,需提前在 RabbitMQ 中配置
     * @param routingKey     路由Key,用于匹配队列
     * @throws RuntimeException 当消息发送失败时抛出异常
     */
    void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey);

    /**
     * 发送延迟消息
     * <p>
     * 消息会在指定延迟时间后才投递到队列,消费者才会收到。
     * 需要安装 rabbitmq-delayed-message-exchange 插件,并将交换机类型设置为 x-delayed-message。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 30分钟后取消未支付订单
     * DefaultMessage message = new DefaultMessage();
     * message.setData(orderId);
     * message.setBizType("order_timeout");
     * rabbitMqService.sendMsg(message, "order.delayed.exchange", 
     *                          "order.timeout", 30, TimeUnit.MINUTES);
     * }</pre>
     *
     * <h3>注意事项：</h3>
     * <ul>
     *   <li>需要提前安装 rabbitmq-delayed-message-exchange 插件</li>
     *   <li>交换机类型必须为 x-delayed-message</li>
     *   <li>需要将当前交换机绑定到目标业务的队列上</li>
     * </ul>
     *
     * @param defaultMessage 消息体,包含业务数据和元信息
     * @param exchangeName   延迟交换机名称(类型为 x-delayed-message)
     * @param routingKey     路由Key,用于匹配队列
     * @param time           延迟时间
     * @param timeUnit       时间单位
     * @throws RuntimeException 当消息发送失败时抛出异常
     */
    void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit);

    /**
     * 将时间和单位转化为毫秒
     *
     * @param time     时间
     * @param timeUnit 时间单位
     */
    default Long toMilliseconds(int time, TimeUnit timeUnit) {
        if (timeUnit == TimeUnit.HOURS) {
            return TimeUnit.HOURS.toMillis(time);
        } else if (timeUnit == TimeUnit.MINUTES) {
            return TimeUnit.MINUTES.toMillis(time);
        } else if (timeUnit == TimeUnit.SECONDS) {
            return TimeUnit.SECONDS.toMillis(time);
        } else if (timeUnit == TimeUnit.MILLISECONDS) {
            return TimeUnit.MILLISECONDS.toMillis(time);
        } else if (timeUnit == TimeUnit.DAYS) {
            return TimeUnit.DAYS.toMillis(time);
        } else {
            return 0L;
        }
    }

}
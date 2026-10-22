package com.simple.common.rabbitmq.common.service;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 消息队列服务接口。
 * <p>
 * 提供消息发送能力，支持即时消息和延迟消息。
 * 默认实现 {@link com.simple.common.rabbitmq.manager.DefaultRabbitMqService} 基于 Spring AMQP 实现。
 * </p>
 *
 * <h3>可靠性说明：</h3>
 * <p>
 * {@code convertAndSend} 正常返回仅表示消息已进入客户端通道，发送的最终结果以
 * 发布确认（Publisher Confirm）回调为准——发送失败会触发
 * SendFailurePersistenceManager 兜底持久化，请勿以"无异常"作为业务成功的依据。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>异步解耦：订单创建后发送通知、更新库存等</li>
 *   <li>流量削峰：秒杀活动、批量处理等</li>
 *   <li>延迟任务：订单超时取消、定时提醒等（需安装 rabbitmq-delayed-message-exchange 插件）</li>
 * </ul>
 *
 * @author qty
 */
public interface RabbitMqService {

    /**
     * 发送即时消息
     * <p>
     * 消息会立即投递到指定的交换机和队列，消费者会尽快处理。
     * 适用于对实时性要求较高的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * DefaultMessage message = new DefaultMessage();
     * message.setMsgData(orderInfo);
     * message.setBizType("order");
     * rabbitMqService.sendMsg(message, "order.exchange", "order.created");
     * }</pre>
     *
     * @param defaultMessage 消息体，包含业务数据和元信息，不允许为 null
     * @param exchangeName   交换机名称，需提前在 RabbitMQ 中配置
     * @param routingKey     路由Key，用于匹配队列
     * @throws IllegalArgumentException defaultMessage 为 null 时抛出
     * @throws RuntimeException         当消息发送失败时抛出异常（含发送前序列化/后置处理器异常）
     */
    void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey);

    /**
     * 发送延迟消息
     * <p>
     * 消息会在指定延迟时间后才投递到队列，消费者才会收到。
     * 需要安装 rabbitmq-delayed-message-exchange 插件，并将交换机类型设置为 x-delayed-message。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 30分钟后取消未支付订单
     * DefaultMessage message = new DefaultMessage();
     * message.setMsgData(orderId);
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
     *   <li>延迟参数契约：time 必须大于 0（0 视为立即投递）；timeUnit 不允许为 null 且换算后
     *       必须不低于 1 毫秒（如 NANOSECONDS 配小值会不足 1 毫秒），否则抛出 IllegalArgumentException；
     *       实际延迟以毫秒精度写入 x-delay 头，插件粒度为毫秒级</li>
     * </ul>
     *
     * @param defaultMessage 消息体，包含业务数据和元信息，不允许为 null
     * @param exchangeName   延迟交换机名称（类型为 x-delayed-message）
     * @param routingKey     路由Key，用于匹配队列
     * @param time           延迟时间，必须大于 0
     * @param timeUnit       时间单位，不允许为 null
     * @throws IllegalArgumentException 消息体为 null、time 非正、timeUnit 为 null 或换算后不足 1 毫秒时抛出
     * @throws RuntimeException         当消息发送失败时抛出异常
     */
    void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit);

    /**
     * 将时间和单位转化为毫秒（严格契约版）。
     * <p>
     * 支持全部 {@link TimeUnit} 枚举值；timeUnit 为 null、time 为负数、或正数 time 换算后不足 1 毫秒时
     * 直接抛出 {@link IllegalArgumentException} 快速失败——禁止静默回退 0ms（会让"延迟消息"立即投递）。
     * </p>
     *
     * @param time     时间，必须大于等于 0
     * @param timeUnit 时间单位，不允许为 null
     * @return 毫秒值（非 null Long）
     * @throws IllegalArgumentException 参数非法时抛出
     */
    default Long toMilliseconds(int time, TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "timeUnit 不允许为 null");
        if (time < 0) {
            throw new IllegalArgumentException("延迟时间不允许为负数: " + time);
        }
        long millis = timeUnit.toMillis(time);
        if (time > 0 && millis <= 0) {
            throw new IllegalArgumentException("延迟时间换算后不足 1 毫秒: " + time + " " + timeUnit
                    + "，请调大数值或改用更大的时间单位");
        }
        return millis;
    }
}

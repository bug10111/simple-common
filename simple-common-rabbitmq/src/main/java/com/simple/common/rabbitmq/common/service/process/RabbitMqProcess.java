package com.simple.common.rabbitmq.common.service.process;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.core.common.service.process.BasProcessService;
import org.springframework.amqp.core.Message;

/**
 * RabbitMQ 消息处理接口，定义消息消费的处理节点。
 * <p>
 * 该接口继承自 {@link BasProcessService}，是 RabbitMQ 消息消费责任链的核心接口。
 * 每个实现类代表一个消息消费的处理节点，如消息解析、业务处理、消息确认等。
 * 通过责任链模式，可以将消息消费的多个步骤串联执行，实现灵活的消息处理流程。
 * </p>
 *
 * <h3>责任链模式说明：</h3>
 * <p>
 * RabbitMQ 消息处理责任链采用枚举驱动的责任链模式，通过 {@link com.simple.common.rabbitmq.common.enums.RMQKindProcess}
 * 枚举定义处理节点的顺序和类型。框架默认提供以下处理节点：
 * <ul>
 *     <li>{@link com.simple.common.rabbitmq.service.process.DefaultRMQProcess}：默认消息处理，执行业务逻辑并确认消息</li>
 * </ul>
 * </p>
 *
 * <h3>扩展指南：</h3>
 * <p>
 * 如需添加自定义的消息处理节点，按以下步骤操作：
 * </p>
 * <pre>{@code
 * // 1. 实现 RabbitMqProcess 接口
 * @Component
 * public class CustomRMQProcess implements RabbitMqProcess {
 *     @Override
 *     public void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption) {
 *         // 自定义消息处理逻辑
 *         String body = new String(message.getBody());
 *         OrderData order = JsonUtils.parse(body, OrderData.class);
 *
 *         // 执行业务处理
 *         orderService.process(order);
 *
 *         // 手动确认消息
 *         try {
 *             channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
 *         } catch (IOException e) {
 *             throw new DefaultException("消息确认失败", e);
 *         }
 *     }
 * }
 *
 * // 2. 在枚举中添加新的处理节点
 * public enum RMQKindProcess implements DefaultKindProcess {
 *     DEFAULT_PROCESS(1, "默认处理", DefaultRMQProcess.class),
 *     CUSTOM_PROCESS(2, "自定义处理", CustomRMQProcess.class); // 新增节点
 * }
 * }</pre>
 *
 * <h3>消息确认机制：</h3>
 * <p>
 * RabbitMQ 消息消费支持手动确认和自动确认两种模式：
 * <ul>
 *     <li>手动确认：消费成功后调用 channel.basicAck()，消费失败调用 channel.basicNack()</li>
 *     <li>自动确认：消息到达消费者后自动确认，不保证消息可靠性</li>
 * </ul>
 * 推荐使用手动确认模式，确保消息可靠性。
 * </p>
 *
 * <h3>消息重试机制：</h3>
 * <p>
 * 消息消费失败后，框架会自动记录重试次数：
 * <ul>
 *     <li>重试次数未达上限：消息重新入队，等待下次消费</li>
 *     <li>重试次数达到上限：消息进入死信队列，等待人工处理</li>
 * </ul>
 * </p>
 *
 * @author qty
 * @see BasProcessService
 * @see com.simple.common.rabbitmq.service.process.DefaultRMQProcess
 * @see com.simple.common.rabbitmq.common.enums.RMQKindProcess
 * @see RabbitMqConsumption
 */
public interface RabbitMqProcess extends BasProcessService {

    /**
     * 执行消息消费处理逻辑。
     *
     * @param message             消息载体，包含消息体和消息属性
     * @param channel             RabbitMQ 通道，用于消息确认
     * @param rabbitMqConsumption 消费注解，包含队列名称、重试次数等配置
     * @throws com.simple.common.core.exception.DefaultException 如果消息处理失败
     */
    void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption);

}
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
 *     <li>{@link com.simple.common.rabbitmq.service.process.DefaultRMQProcess}：默认占位处理节点（枚举 execute=false，默认不参与执行）</li>
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
         * 执行业务处理
         *         orderService.process(order);
         *
         * 注意：消息确认（basicAck/basicNack）由框架切面统一结算，
         * 处理器内禁止调用 channel 的任何应答方法，重复应答会导致通道级错误
 *     }
 * }
 *
     * // 2. 覆写 getProcess() 返回本处理器绑定的枚举节点（BasProcessService 抽象方法，缺省未实现，漏写无法编译）
     * //    所需 import：org.springframework.amqp.core.Message、com.rabbitmq.client.Channel、
     * //    com.simple.common.rabbitmq.annotation.RabbitMqConsumption、RMQKindProcess 枚举类
 * public enum RMQKindProcess implements DefaultKindProcess {
 *     DEFAULT_PROCESS(1, "默认处理", DefaultRMQProcess.class),
 *     CUSTOM_PROCESS(2, "自定义处理", CustomRMQProcess.class); // 新增节点
 * }
 * }</pre>
 *
 * <h3>消息确认机制：</h3>
 * <p>
 * 消息确认由框架统一管理：切面在责任链与业务方法执行后按执行结果统一结算 basicAck/basicNack，
 * 业务监听方法与处理器均不得自行调用 channel.basicAck()/basicNack()（重复应答会使通道报错）。
 * 监听容器需由使用方配置为手动确认模式，该前提见注解类说明。
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
     * 注意：处理器抛出的异常会被切面捕获并仅记录日志（不中断消费链、不触发 nack），不应依赖异常中止消费
     */
    void execution(Message message, Channel channel, RabbitMqConsumption rabbitMqConsumption);

}

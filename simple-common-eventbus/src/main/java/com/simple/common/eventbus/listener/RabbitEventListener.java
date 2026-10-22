package com.simple.common.eventbus.listener;

import com.rabbitmq.client.Channel;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RabbitMQ 事件消息监听器
 * <p>装配条件：仅当 simple.event.type=mq（或缺省默认 mq）时装配，
 * 同步模式（simple.event.type=sync）下不创建监听器，纯同步使用无需 RabbitMQ 依赖与连接</p>
 *
 * @author qty
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = EventConstant.EVENT_TYPE_PREFIX, name = "type", havingValue = EventConstant.EVENT_TYPE_MQ, matchIfMissing = true)
public class RabbitEventListener {

    @Autowired
    private EventHandlerManager eventHandlerManager;

    @RabbitListener(queues = "#{T(com.simple.common.eventbus.util.MqNameUtil).queueName(applicationProperties.getName())}", concurrency = "#{eventProperties.getConcurrency()}")
    @RabbitMqConsumption
    public void receiveMessage(Message message, Channel channel) throws Exception {
        // 报文合法性校验：非法报文统一按失败抛出，交由 @RabbitMqConsumption 切面走重试/死信，
        // 不允许静默 ack 丢弃畸形消息（与 eventData == null 分支的失败语义保持一致）
        EventData eventData;
        try {
            eventData = SerializeUtils.deserialize(message.getBody(), EventData.class);
        } catch (Exception e) {
            // 切面 catch 中已打印完整堆栈，此处只做统一失败语义转换，不重复记录 error 日志
            throw new IllegalArgumentException("事件消息反序列化失败，消息体非法", e);
        }
        if (eventData == null) {
            throw new IllegalArgumentException("事件消息反序列化失败，消息体为空");
        }
        // msgData 是处理器参数还原、eventName 是监听器匹配的必要字段，均不允许为空白（空串与 null 同判）；
        // 缺失时处理器不命中且无任何报错，事件被静默 ack，必须在消费入口拦截
        if (!StringUtils.hasText(eventData.getMsgData()) || !StringUtils.hasText(eventData.getEventName())) {
            throw new IllegalArgumentException("事件消息体非法，msgData/eventName 为空。事件名称: " + eventData.getEventName()
                    + ", 消息ID: " + eventData.getId());
        }

        // ThreadLocal 设置已下沉到 EventHandlerManager 内部统一管理。
        // 上报分发结果：MQ 模式下单个处理器失败为隔离式仅记日志，必须由监听器感知整体失败并抛出，
        // 否则切面按业务成功 ack 消息，@RabbitMqConsumption 的重试/死信链路永远不会生效
        boolean success = eventHandlerManager.handlerWithResult(eventData);
        if (!success) {
            throw new IllegalStateException("事件处理器执行失败，交由消费链路重试。事件名称: " + eventData.getEventName());
        }
    }
}

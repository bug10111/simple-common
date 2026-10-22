package com.simple.common.rabbitmq.service;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.entity.EnhancedCorrelationData;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ 服务默认实现，提供消息发送能力。
 * <p>
 * 包含异常捕获与耗时日志；消息体默认依赖 Jackson2JsonMessageConverter（容器中的
 * {@code RabbitTemplate} 由 RabbitMqConfig 装配该转换器，Content-Type 由转换器写入，
 * 不再在此硬编码覆盖）。发送成功日志仅代表"已投递至通道"，最终结果以发布确认为准。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultRabbitMqService implements RabbitMqService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey) {
        doSend(defaultMessage, exchangeName, routingKey, null);
    }

    @Override
    public void sendMsg(DefaultMessage defaultMessage, String exchangeName, String routingKey, int time, TimeUnit timeUnit) {
        // 参数非法（null 单位/负数/不足1毫秒）在此处快速失败，避免"延迟消息"被静默改为立即投递
        long delayMillis = toMilliseconds(time, timeUnit);
        doSend(defaultMessage, exchangeName, routingKey, delayMillis);
    }

    /**
     * 统一发送逻辑（两个 sendMsg 重载共用，避免错误处理逻辑双份漂移）
     *
     * @param defaultMessage 消息体（非 null）
     * @param exchangeName   交换机
     * @param routingKey     路由键
     * @param delayMillis    延迟毫秒；null 表示即时消息
     */
    private void doSend(DefaultMessage defaultMessage, String exchangeName, String routingKey, Long delayMillis) {
        long start = System.currentTimeMillis();
        // 参数前置校验 + id 兜底前置：保证 catch 中始终有可用的 msgId 可记录，不会因 defaultMessage 为 null 再抛 NPE 掩盖原异常
        Objects.requireNonNull(defaultMessage, "defaultMessage 不允许为 null");
        defaultMessage.initialization();
        String msgId = defaultMessage.getId();
        try {
            // 构建携带消息详情的 CorrelationData（供确认失败回调兜底取回）
            EnhancedCorrelationData data = EnhancedCorrelationData.of(msgId, defaultMessage, exchangeName, routingKey, null);

            rabbitTemplate.convertAndSend(exchangeName, routingKey, defaultMessage, message -> {
                message.getMessageProperties().setCorrelationId(msgId);
                // Content-Type 由 MessageConverter 决定（默认 Jackson 转换器写入 application/json），此处不再硬编码覆盖
                if (delayMillis != null) {
                    message.getMessageProperties().setDelayLong(delayMillis);
                }
                // 保存消息体字节数组供确认回调使用（防御性拷贝，防止回调线程读到被复用的缓冲）
                data.setMessageBody(message.getBody());
                return message;
            }, data);

            if (log.isDebugEnabled()) {
                log.debug("消息已投递至通道（发送结果以发布确认为准），exchange={}, routingKey={}, msgId={}, delayMillis={}, 耗时={}ms",
                        exchangeName, routingKey, msgId, delayMillis, System.currentTimeMillis() - start);
            }
        } catch (AmqpException e) {
            log.error("消息发送失败，exchange={}, routingKey={}, msgId={}, 耗时={}ms",
                    exchangeName, routingKey, msgId, System.currentTimeMillis() - start, e);
            throw e;
        } catch (Exception e) {
            // 自定义 MessageConverter / 消息后置处理器抛出的非 AMQP 运行时异常同样记录后原样抛出，不再无声丢失
            log.error("消息发送前处理异常（转换/后置处理器），exchange={}, routingKey={}, msgId={}, 耗时={}ms",
                    exchangeName, routingKey, msgId, System.currentTimeMillis() - start, e);
            throw e;
        }
    }
}

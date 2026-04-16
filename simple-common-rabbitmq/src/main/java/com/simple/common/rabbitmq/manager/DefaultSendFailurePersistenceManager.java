package com.simple.common.rabbitmq.manager;

import com.simple.common.core.utils.JsonUtils;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.SendFailurePersistenceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

/**
 * 发送失败持久化默认实现，仅打印警告日志。
 * 集成方必须替换此实现，将失败消息存入可靠存储并实现补偿重试。
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSendFailurePersistenceManager implements SendFailurePersistenceManager {

    @Override
    public void saveConfirmFailure(String correlationId, String cause, String receivedExchange, String receivedRoutingKey, DefaultMessage defaultMessage, byte[] messageBody) {
        String bodyStr = defaultMessage != null ? JsonUtils.toJsonStr(defaultMessage) : (messageBody != null ? new String(messageBody) : "无消息体");
        log.error("【严重】发送交换机消息失败！correlationId={}, cause={}, exchange={}, routingKey={}, 消息体={}。请实现 SendFailurePersistenceManager 接口进行持久化！", correlationId, cause,
                  receivedExchange, receivedRoutingKey, bodyStr);
        // 可在此处写入本地文件作为最后兜底
    }

    @Override
    public void saveReturnFailure(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        log.error("【严重】消息路由失败！exchange={}, routingKey={}, replyCode={}, replyText={}, 消息体={}。请实现 SendFailurePersistenceManager 接口进行持久化！", exchange, routingKey,
                  replyCode, replyText, new String(message.getBody()));
    }
}
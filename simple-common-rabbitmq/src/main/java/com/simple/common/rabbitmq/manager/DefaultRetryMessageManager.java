package com.simple.common.rabbitmq.manager;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.common.config.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.RetryCountManager;
import com.simple.common.rabbitmq.common.manager.RetryMessageManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import com.simple.common.rabbitmq.common.service.DeadLetterService;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 重试消息管理器默认实现
 *
 * @author qty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRetryMessageManager implements RetryMessageManager {

    private final RetryCountManager retryCountManager;
    private final RabbitMqProperties properties;
    private final RabbitMqService rabbitMqService;
    private final DeadLetterService deadLetterService;

    /**
     * 处理业务失败的重试逻辑
     *
     * @param retryKey      重试计数Key
     * @param message       原始消息
     * @param channel       信道（未使用）
     * @param maxRetryCount 最大重试次数
     * @param cause         异常原因
     * @param defaultMessage 消息体
     */
    @Override
    public void handleBusinessFailureRetry(String retryKey, Message message, Channel channel,
                                           int maxRetryCount, Exception cause, DefaultMessage defaultMessage) {
        long currentCount = retryCountManager.incrementAndGet(retryKey);
        doRetry(retryKey, message, maxRetryCount, cause, defaultMessage, currentCount, false);
    }

    /**
     * 处理校验失败的重试逻辑
     *
     * @param verifyRetryKey 校验重试计数Key
     * @param message        原始消息
     * @param channel        信道（未使用）
     * @param maxRetryCount  最大重试次数
     * @param cause          异常原因
     * @param defaultMessage 消息体
     */
    @Override
    public void handleVerificationFailureRetry(String verifyRetryKey, Message message, Channel channel,
                                               int maxRetryCount, Exception cause, DefaultMessage defaultMessage) {
        long currentCount = retryCountManager.incrementAndGet(verifyRetryKey);
        doRetry(verifyRetryKey, message, maxRetryCount, cause, defaultMessage, currentCount, true);
    }

    /**
     * 执行重试核心逻辑：判断次数、计算延迟、发送延迟消息或进入死信
     *
     * @param retryKey           重试计数Key
     * @param message            原始消息
     * @param maxRetryCount      最大重试次数
     * @param cause              异常原因
     * @param defaultMessage     消息体
     * @param currentCount       当前重试次数
     * @param isVerificationRetry 是否为校验失败重试
     */
    private void doRetry(String retryKey, Message message, int maxRetryCount, Exception cause,
                         DefaultMessage defaultMessage, long currentCount, boolean isVerificationRetry) {
        MessageProperties props = message.getMessageProperties();
        String consumerQueue = props.getConsumerQueue();
        String routingKey = props.getReceivedRoutingKey();
        String correlationId = props.getCorrelationId();

        if (currentCount <= maxRetryCount) {
            long delayMs = Math.min(
                    properties.getRetryDelayBaseMs() * (1L << (currentCount - 1)),
                    properties.getRetryDelayMaxMs());
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);

            try {
                defaultMessage.setId(correlationId);
                rabbitMqService.sendMsg(defaultMessage, properties.getDelayedExchange(),
                        routingKey, (int) delaySeconds, TimeUnit.SECONDS);
                log.debug("队列[{}] id[{}] {}第{}次重试，延迟{}秒",
                        consumerQueue, correlationId, isVerificationRetry ? "校验失败" : "业务失败", currentCount, delaySeconds);
            } catch (Exception sendEx) {
                log.error("发送延迟重试消息失败，进入死信", sendEx);
                deadLetterService.save(message, cause);
                retryCountManager.delete(retryKey);
            }
        } else {
            log.debug("队列[{}] id[{}] 重试次数已达上限，进入死信", consumerQueue, correlationId);
            deadLetterService.save(message, cause);
            retryCountManager.delete(retryKey);
        }
    }
}
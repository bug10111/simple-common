package com.simple.common.rabbitmq.manager;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
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
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 重试消息管理器默认实现
 * <p>
 * 重试副本投递到延迟交换机做指数退避重投；本实现不操作 channel、不抛出异常：
 * 重试副本发送失败时转入死信兜底并保证计数清理（try/finally）。
 * </p>
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
     * 位移上限：指数退避指数封顶为 30（2^30），避免 long 位移取低 6 位回绕与乘法溢出
     */
    private static final int MAX_BACKOFF_EXPONENT = 30;

    @Override
    public void handleBusinessFailureRetry(String retryKey, Message message, Channel channel,
                                           int maxRetryCount, Exception cause, DefaultMessage defaultMessage) {
        long currentCount = retryCountManager.incrementAndGet(retryKey);
        doRetry(retryKey, message, maxRetryCount, cause, defaultMessage, currentCount, false);
    }

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
     * @param cause              异常原因（可为 null）
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

        // 若路由键为空，则使用队列名作为路由键（延迟交换机需正确绑定）
        if (!StringUtils.hasText(routingKey)) {
            if (!StringUtils.hasText(consumerQueue)) {
                // 路由键与队列名均缺失时无法投递重试副本，直接进入死信，避免以空路由键投递后被静默丢弃
                log.error("消息路由键与消费队列均为空，无法重试，直接进入死信，id[{}]", correlationId);
                saveDeadLetterWithCountCleanup(retryKey, message, cause);
                return;
            }
            routingKey = consumerQueue;
            log.debug("消息无路由键，使用队列名[{}]作为重试路由键", consumerQueue);
        }

        if (currentCount <= maxRetryCount) {
            // 指数封顶，防止位移回绕与溢出为负
            long exponent = Math.min(Math.max(currentCount - 1, 0), MAX_BACKOFF_EXPONENT);
            long base = properties.getRetryDelayBaseMs();
            long backoff = base <= (Long.MAX_VALUE >> exponent) ? base << exponent : Long.MAX_VALUE;
            long delayMs = Math.min(backoff, properties.getRetryDelayMaxMs());
            // 向上取整且至少 1 秒，避免亚秒级延迟被截断为 0 造成立即重投热循环
            long delaySeconds = Math.max(1L, (delayMs + 999L) / 1000L);

            try {
                // 仅在关联ID有效时覆盖消息id，保持重试副本与重试计数Key的一致性
                if (StringUtils.hasText(correlationId)) {
                    defaultMessage.setId(correlationId);
                }
                rabbitMqService.sendMsg(defaultMessage, properties.getDelayedExchange(),
                        routingKey, (int) delaySeconds, TimeUnit.SECONDS);
                log.debug("队列[{}] id[{}] {}第{}次重试，延迟{}秒",
                        consumerQueue, correlationId, isVerificationRetry ? "校验失败" : "业务失败", currentCount, delaySeconds);
            } catch (Exception sendEx) {
                // 发送重试副本失败：死信记录发送异常本身（而非业务cause），并用 try/finally 保证计数清理
                log.error("队列[{}] id[{}] 发送延迟重试消息失败，进入死信", consumerQueue, correlationId, sendEx);
                saveDeadLetterWithCountCleanup(retryKey, message, sendEx);
            }
        } else {
            log.warn("队列[{}] id[{}] 重试次数已达上限（{}次），进入死信", consumerQueue, correlationId, maxRetryCount);
            saveDeadLetterWithCountCleanup(retryKey, message, cause);
        }
    }

    /**
     * 死信兜底 + 计数清理：无论死信持久化是否抛出异常，都保证重试计数被清理（try/finally），
     * 避免脏计数干扰同一 correlationId 的后续消息判定
     */
    private void saveDeadLetterWithCountCleanup(String retryKey, Message message, Exception cause) {
        try {
            deadLetterService.save(message, cause);
        } finally {
            retryCountManager.delete(retryKey);
        }
    }
}

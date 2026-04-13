package com.simple.common.rabbitmq.aspeck;

import com.rabbitmq.client.Channel;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.rabbitmq.common.config.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.AckRMQManager;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationRouterManager;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationStrategyManager;
import com.simple.common.rabbitmq.common.manager.FailedRMQManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import com.simple.common.rabbitmq.common.service.DeadLetterService;
import com.simple.common.rabbitmq.common.service.RabbitMqService;
import com.simple.common.rabbitmq.common.service.process.RabbitMqProcess;
import com.simple.common.rabbitmq.manager.RedisRepeatRMQManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RabbitMQ消息消费处理切面，实现防重、重试、完成校验等横切逻辑
 *
 * @author qty
 */
@Order(1)
@Aspect
@Slf4j
@Component
public class RabbitMqProcessAspect {

    @Autowired
    private List<RabbitMqProcess> listManager;

    @Autowired
    private RedisRepeatRMQManager repeatRMQManager;

    @Autowired
    private FailedRMQManager failedRMQManager;

    @Autowired
    private RabbitMqProperties rabbitMqProperties;

    @Autowired
    private AckRMQManager ackRMQManager;

    @Autowired
    private DeadLetterService deadLetterService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CompletionVerificationRouterManager verificationRouterManager;

    @Autowired
    private RabbitMqService rabbitMqService;

    // 原子递增并设置过期时间的Lua脚本
    private static final String INCR_WITH_EXPIRE_SCRIPT =
                    "local current = redis.call('INCR', KEYS[1]) " + "if current == 1 then " + "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " + "end " + "return current";

    // 原子设置过期时间（仅当值匹配）
    private static final String EXPIRE_IF_VALUE_EQUALS_SCRIPT =
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then " + "    return redis.call('EXPIRE', KEYS[1], ARGV[2]) " + "else " + "    return 0 " + "end";

    private final DefaultRedisScript<Long> incrWithExpireScript = new DefaultRedisScript<>(INCR_WITH_EXPIRE_SCRIPT, Long.class);

    private final DefaultRedisScript<Boolean> expireIfValueEqualsScript = new DefaultRedisScript<>(EXPIRE_IF_VALUE_EQUALS_SCRIPT, Boolean.class);

    /**
     * 环绕通知：处理消息消费的防重、重试、ACK 等横切逻辑
     */
    @SneakyThrows
    @Around("@annotation(com.simple.common.rabbitmq.annotation.RabbitMqConsumption)")
    public void around(ProceedingJoinPoint joinPoint) {
        // 获取注解参数
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RabbitMqConsumption rabbitMqConsumption = method.getAnnotation(RabbitMqConsumption.class);

        // 获取消息参数
        Object[] args = joinPoint.getArgs();
        Message message = (Message) args[0];
        Channel channel = (Channel) args[1];

        MessageProperties props = message.getMessageProperties();
        String correlationId = props.getCorrelationId();
        String consumerQueue = props.getConsumerQueue();
        String exchange = props.getReceivedExchange();
        String routingKey = props.getReceivedRoutingKey();
        long deliveryTag = props.getDeliveryTag();

        // 防重锁 Key
        String lockKey = repeatRMQManager.getRepeatedConsumptionKey(consumerQueue, correlationId);
        // 重试计数 Key
        String retryKey = failedRMQManager.getFailedMqRedisKey(rabbitMqProperties.getCalculation(), consumerQueue, correlationId);

        int businessTime = rabbitMqConsumption.businessTime();
        TimeUnit timeUnit = rabbitMqConsumption.timeUnit();
        int maxRetryCount = rabbitMqConsumption.retryCount();
        long effectiveSeconds = rabbitMqConsumption.effectiveTime();

        // 提前反序列化消息体，避免多处重复解析
        DefaultMessage defaultMessage = SerializeUtils.deserialize(message.getBody(), DefaultMessage.class);
        if (defaultMessage == null) {
            log.error("队列[{}] 消息反序列化失败，消息体无法解析为 DefaultMessage，将拒绝并不放回队列", consumerQueue);
            ackRMQManager.basicNack(channel, deliveryTag, false);
            return;
        }

        // 重试获取锁（避免递归调用）
        boolean lockAcquired = false;
        int maxLockAttempts = 3;
        for (int attempt = 0; attempt < maxLockAttempts; attempt++) {
            lockAcquired = tryAcquireLock(lockKey, businessTime, timeUnit, consumerQueue, exchange, routingKey, correlationId, channel, deliveryTag, retryKey, maxRetryCount, defaultMessage,
                                          message);
            if (lockAcquired) {
                break;
            }
            if (attempt < maxLockAttempts - 1) {
                Thread.sleep(50);
            }
        }
        if (!lockAcquired) {
            log.warn("队列[{}] exchange[{}]=>key[{}]=>id[{}] 获取锁失败，消息将被拒绝并放回队列", consumerQueue, exchange, routingKey, correlationId);
            ackRMQManager.basicNack(channel, deliveryTag, true);
            return;
        }

        try {
            // ========== 前置处理链（异常隔离） ==========
            for (RabbitMqProcess manager : listManager) {
                if (manager.getProcess().isExecute()) {
                    try {
                        manager.execution(message, channel, rabbitMqConsumption);
                        if (log.isTraceEnabled()) {
                            log.trace("队列[{}] exchange[{}]=>key[{}]=>id[{}]执行[{}]成功！", consumerQueue, exchange, routingKey, correlationId, manager.getProcess().getMsg());
                        }
                    } catch (Exception ex) {
                        log.error("前置处理器[{}]执行异常，消息将继续消费", manager.getProcess().getMsg(), ex);
                    }
                }
            }

            // ========== 执行业务并获取返回值 ==========
            Object businessResult = joinPoint.proceed(args);
            String businessId = (businessResult != null) ? businessResult.toString() : "";

            // ========== 成功处理：更新 Redis 状态为 DONE:业务ID，再 ACK ==========
            String doneStatus = rabbitMqProperties.getLockStatusDonePrefix() + businessId;
            redisTemplate.opsForValue().set(lockKey, doneStatus, effectiveSeconds, TimeUnit.SECONDS);

            ackRMQManager.basicAck(channel, deliveryTag);
            if (log.isDebugEnabled()) {
                log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}]==>[{}]消费成功！业务ID:{}", consumerQueue, exchange, routingKey, correlationId, new String(message.getBody()), businessId);
            }

            failedRMQManager.delete(retryKey);

        } catch (Exception e) {
            log.error("队列[{}] exchange[{}]=>key[{}]=>id[{}]==>[{}]消费失败！", consumerQueue, exchange, routingKey, correlationId, new String(message.getBody()), e);
            redisTemplate.delete(lockKey);
            handleRetry(retryKey, message, channel, maxRetryCount, e, defaultMessage);
        }
    }

    /**
     * 尝试获取防重锁，处理状态判断与 TTL 修正逻辑
     *
     * @param message 原始消息对象，用于校验失败重试时传递完整信息
     * @return true-获取锁成功，false-获取失败（消息已被拒绝或 ACK）
     */
    @SneakyThrows
    private boolean tryAcquireLock(String lockKey, int businessTime, TimeUnit timeUnit, String consumerQueue, String exchange, String routingKey, String correlationId, Channel channel,
                                   long deliveryTag, String retryKey, int maxRetryCount, DefaultMessage defaultMessage, Message message) {
        Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(lockKey, rabbitMqProperties.getLockStatusProcessing(), businessTime, timeUnit);
        if (Boolean.TRUE.equals(setSuccess)) {
            return true;
        }

        // 锁已存在，检查状态
        String status = redisTemplate.opsForValue().get(lockKey);
        if (status != null && status.startsWith(rabbitMqProperties.getLockStatusDonePrefix())) {
            String businessId = status.substring(rabbitMqProperties.getLockStatusDonePrefix().length());
            CompletionVerificationStrategyManager strategy = verificationRouterManager.getStrategy(consumerQueue);
            if (strategy != null && !strategy.isCompleted(businessId, defaultMessage)) {
                log.warn("队列[{}] exchange[{}]=>key[{}]=>id[{}] 业务ID[{}]校验未完成，重新消费", consumerQueue, exchange, routingKey, correlationId, businessId);

                // 删除锁，并触发校验失败重试（独立计数）
                redisTemplate.delete(lockKey);
                handleRetryForVerificationFailure(retryKey, message, channel, maxRetryCount, new RuntimeException("业务完成校验未通过，强制重试"));
                return false;
            }
            if (log.isDebugEnabled()) {
                log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 已完成(业务ID:{})，直接ACK", consumerQueue, exchange, routingKey, correlationId, businessId);
            }
            ackRMQManager.basicAck(channel, deliveryTag);
            return false;
        } else {
            // 处理中状态，检查 TTL 是否过长（可能由于服务宕机导致锁未释放）
            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            if (ttl > rabbitMqProperties.getLockTtlRecoveryThresholdSeconds()) {
                // 原子性缩短 TTL：仅当锁的值仍为 PROCESSING 时才执行
                Boolean expireResult = redisTemplate.execute(expireIfValueEqualsScript, Collections.singletonList(lockKey), rabbitMqProperties.getLockStatusProcessing(),
                                                             String.valueOf(rabbitMqProperties.getLockTtlRecoveryThresholdSeconds()));
                if (expireResult) {
                    log.info("队列[{}] exchange[{}]=>key[{}]=>id[{}] 处理中锁剩余{}秒，已缩短至{}秒以加速恢复", consumerQueue, exchange, routingKey, correlationId, ttl,
                             rabbitMqProperties.getLockTtlRecoveryThresholdSeconds());
                } else {
                    log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 缩短锁过期时间时锁状态已变更，放弃缩短", consumerQueue, exchange, routingKey, correlationId);
                }
            }
            log.info("队列[{}] exchange[{}]=>key[{}]=>id[{}] 正在处理中，拒绝并放回队列", consumerQueue, exchange, routingKey, correlationId);
            ackRMQManager.basicNack(channel, deliveryTag, true);
            return false;
        }
    }

    /**
     * 处理消息消费失败的重试逻辑（延迟退避）
     */
    @SneakyThrows
    private void handleRetry(String retryKey, Message message, Channel channel, int maxRetryCount, Exception e, DefaultMessage defaultMessage) {
        MessageProperties props = message.getMessageProperties();
        String consumerQueue = props.getConsumerQueue();
        String exchange = props.getReceivedExchange();
        String routingKey = props.getReceivedRoutingKey();
        String correlationId = props.getCorrelationId();
        long deliveryTag = props.getDeliveryTag();

        Long currentRetryCount = redisTemplate.execute(incrWithExpireScript, Collections.singletonList(retryKey), String.valueOf(rabbitMqProperties.getRetryCountTtlSeconds()));
        if (currentRetryCount == null) {
            currentRetryCount = 1L;
        }

        if (currentRetryCount <= maxRetryCount) {
            ackRMQManager.basicNack(channel, deliveryTag, false);

            long delayMs = Math.min(rabbitMqProperties.getRetryDelayBaseMs() * (1L << (currentRetryCount - 1)), rabbitMqProperties.getRetryDelayMaxMs());
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);

            try {
                defaultMessage.setId(correlationId);
                rabbitMqService.sendMsg(defaultMessage, rabbitMqProperties.getDelayedExchange(), routingKey, (int) delaySeconds, TimeUnit.SECONDS);
                if (log.isDebugEnabled()) {
                    log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 第{}次重试，延迟{}秒后重新投递", consumerQueue, exchange, routingKey, correlationId, currentRetryCount, delaySeconds);
                }
            } catch (Exception sendEx) {
                log.error("发送延迟重试消息失败，将直接进入死信处理。exchange={}, routingKey={}, id={}", rabbitMqProperties.getDelayedExchange(), routingKey, correlationId, sendEx);
                deadLetterService.save(message, e);
                failedRMQManager.delete(retryKey);
            }
        } else {
            ackRMQManager.basicNack(channel, deliveryTag, false);
            if (log.isDebugEnabled()) {
                log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 重试次数已达上限({})，消息被拒绝并执行保存策略！", consumerQueue, exchange, routingKey, correlationId, maxRetryCount);
            }
            deadLetterService.save(message, e);
            failedRMQManager.delete(retryKey);
        }
    }

    /**
     * 专门用于处理业务完成校验失败的重试触发。
     * 使用独立的计数 Key（{@code retryKey + ":verify"}），避免占用业务失败重试配额。
     *
     * @param retryKey      原始重试计数 Key（用于生成校验计数 Key）
     * @param message       原始消息对象
     * @param channel       RabbitMQ Channel
     * @param maxRetryCount 最大重试次数
     * @param e             校验异常（用于死信保存时的异常信息）
     */
    @SneakyThrows
    private void handleRetryForVerificationFailure(String retryKey, Message message, Channel channel, int maxRetryCount, Exception e) {
        MessageProperties props = message.getMessageProperties();
        String consumerQueue = props.getConsumerQueue();
        String exchange = props.getReceivedExchange();
        String routingKey = props.getReceivedRoutingKey();
        String correlationId = props.getCorrelationId();
        long deliveryTag = props.getDeliveryTag();

        // 独立的业务校验重试计数 Key
        String verifyRetryKey = retryKey + ":verify";

        Long currentRetryCount = redisTemplate.execute(incrWithExpireScript, Collections.singletonList(verifyRetryKey), String.valueOf(rabbitMqProperties.getRetryCountTtlSeconds()));
        if (currentRetryCount == null) {
            currentRetryCount = 1L;
        }

        if (currentRetryCount <= maxRetryCount) {
            ackRMQManager.basicNack(channel, deliveryTag, false);

            long delayMs = Math.min(rabbitMqProperties.getRetryDelayBaseMs() * (1L << (currentRetryCount - 1)), rabbitMqProperties.getRetryDelayMaxMs());
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);

            try {
                DefaultMessage defaultMessage = SerializeUtils.deserialize(message.getBody(), DefaultMessage.class);
                if (defaultMessage == null) {
                    log.error("校验重试反序列化失败，消息体无法解析为 DefaultMessage，将直接进入死信处理");
                    deadLetterService.save(message, e);
                    failedRMQManager.delete(verifyRetryKey);
                    return;
                }
                defaultMessage.setId(correlationId);
                rabbitMqService.sendMsg(defaultMessage, rabbitMqProperties.getDelayedExchange(), routingKey, (int) delaySeconds, TimeUnit.SECONDS);
                if (log.isDebugEnabled()) {
                    log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 业务校验失败第{}次重试，延迟{}秒后重新投递", consumerQueue, exchange, routingKey, correlationId, currentRetryCount,
                              delaySeconds);
                }
            } catch (Exception sendEx) {
                log.error("发送延迟校验重试消息失败，将直接进入死信处理。exchange={}, routingKey={}, id={}", rabbitMqProperties.getDelayedExchange(), routingKey, correlationId, sendEx);
                deadLetterService.save(message, e);
                failedRMQManager.delete(verifyRetryKey);
            }
        } else {
            ackRMQManager.basicNack(channel, deliveryTag, false);
            if (log.isDebugEnabled()) {
                log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 业务校验失败已达最大重试次数({})，进入持久化处理！", consumerQueue, exchange, routingKey, correlationId, maxRetryCount);
            }
            deadLetterService.save(message, e);
            failedRMQManager.delete(verifyRetryKey);
        }
    }
}
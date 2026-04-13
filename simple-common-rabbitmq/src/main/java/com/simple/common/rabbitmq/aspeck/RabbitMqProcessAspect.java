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
 * Created with IntelliJ IDEA
 * Description: RabbitMQ消息消费处理
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

    private final DefaultRedisScript<Long> incrWithExpireScript = new DefaultRedisScript<>(INCR_WITH_EXPIRE_SCRIPT, Long.class);

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

        // ========== 第一阶段：防重检查（状态机增强版） ==========
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, rabbitMqProperties.getLockStatusProcessing(), businessTime, timeUnit);
        if (Boolean.FALSE.equals(lockAcquired)) {
            String status = redisTemplate.opsForValue().get(lockKey);
            if (status != null && status.startsWith(rabbitMqProperties.getLockStatusDonePrefix())) {
                String businessId = status.substring(rabbitMqProperties.getLockStatusDonePrefix().length());
                CompletionVerificationStrategyManager strategy = verificationRouterManager.getStrategy(consumerQueue);
                if (strategy != null && !strategy.isCompleted(businessId, message)) {
                    log.warn("队列[{}] exchange[{}]=>key[{}]=>id[{}] 业务ID[{}]校验未完成，重新消费", consumerQueue, exchange, routingKey, correlationId, businessId);
                    redisTemplate.delete(lockKey);
                    around(joinPoint);
                    return;
                }
                if (log.isDebugEnabled()) {
                    log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 已完成(业务ID:{})，直接ACK", consumerQueue, exchange, routingKey, correlationId, businessId);
                }
                ackRMQManager.basicAck(channel, deliveryTag);
                return;
            } else {
                Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
                if (ttl != null && ttl > rabbitMqProperties.getLockTtlRecoveryThresholdSeconds()) {
                    redisTemplate.expire(lockKey, rabbitMqProperties.getLockTtlRecoveryThresholdSeconds(), TimeUnit.SECONDS);
                    log.info("队列[{}] exchange[{}]=>key[{}]=>id[{}] 处理中锁剩余{}秒，已缩短至{}秒以加速恢复", consumerQueue, exchange, routingKey, correlationId, ttl,
                             rabbitMqProperties.getLockTtlRecoveryThresholdSeconds());
                }
                log.info("队列[{}] exchange[{}]=>key[{}]=>id[{}] 正在处理中，拒绝并放回队列", consumerQueue, exchange, routingKey, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, true);
                return;
            }
        }

        try {
            // ========== 第二阶段：前置处理链（异常隔离） ==========
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

            // ========== 第三阶段：执行业务并获取返回值 ==========
            Object businessResult = joinPoint.proceed(args);
            String businessId = (businessResult != null) ? businessResult.toString() : "";

            // ========== 第四阶段：成功处理（先更新 Redis 为 DONE:业务ID，再 ACK） ==========
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
            handleRetry(retryKey, message, channel, maxRetryCount, e);
        }
    }

    /**
     * 处理消息消费失败的重试逻辑（延迟退避）
     * 拒绝原消息，并将消息发送到延迟交换机，实现指数退避重试。
     * 若达到重试上限，则拒绝并不放回，调用 DeadLetterService 持久化。
     *
     * @param retryKey      Redis 重试计数 Key
     * @param message       原始消息
     * @param channel       RabbitMQ Channel
     * @param maxRetryCount 最大重试次数
     * @param e             业务异常
     */
    @SneakyThrows
    private void handleRetry(String retryKey, Message message, Channel channel, int maxRetryCount, Exception e) {
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
            // 拒绝原消息，不重新入队
            ackRMQManager.basicNack(channel, deliveryTag, false);

            // 指数退避延迟
            long delayMs = Math.min(rabbitMqProperties.getRetryDelayBaseMs() * (1L << (currentRetryCount - 1)), rabbitMqProperties.getRetryDelayMaxMs());
            long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(delayMs);

            // 反序列化并发送延迟消息
            DefaultMessage defaultMessage = SerializeUtils.deserialize(message.getBody(), DefaultMessage.class);
            defaultMessage.setId(correlationId);
            rabbitMqService.sendMsg(defaultMessage, rabbitMqProperties.getDelayedExchange(), routingKey, (int) delaySeconds, TimeUnit.SECONDS);

            if (log.isDebugEnabled()) {
                log.debug("队列[{}] exchange[{}]=>key[{}]=>id[{}] 第{}次重试，延迟{}秒后重新投递", consumerQueue, exchange, routingKey, correlationId, currentRetryCount, delaySeconds);
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
}
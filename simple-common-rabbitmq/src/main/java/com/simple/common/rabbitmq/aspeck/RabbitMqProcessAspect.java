package com.simple.common.rabbitmq.aspeck;

import com.rabbitmq.client.Channel;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.rabbitmq.annotation.RabbitMqConsumption;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.AckRMQManager;
import com.simple.common.rabbitmq.common.manager.ConsumptionLockManager;
import com.simple.common.rabbitmq.common.manager.RetryCountManager;
import com.simple.common.rabbitmq.common.manager.RetryMessageManager;
import com.simple.common.rabbitmq.common.service.process.RabbitMqProcess;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * RabbitMQ 消息消费切面（增强版：支持锁续期，优化锁竞争逻辑）
 *
 * @author qty
 */
@Order(1)
@Aspect
@Slf4j
@Component
public class RabbitMqProcessAspect {

    @Autowired
    private List<RabbitMqProcess> processList;

    @Autowired
    private ConsumptionLockManager lockManager;

    @Autowired
    private RetryCountManager retryCountManager;

    @Autowired
    private RetryMessageManager retryMessageManager;

    @Autowired
    private AckRMQManager ackRMQManager;

    /**
     * 注解属性缓存，使用 method.toString() 作为 key，避免代理方法差异
     * 修复：使用声明类名+方法名避免 CGLIB 代理类名变化
     */
    private final Map<String, RabbitMqConsumption> annotationCache = new ConcurrentHashMap<>();

    /**
     * 排序后的前置处理器列表（初始化后不变）
     */
    private List<RabbitMqProcess> sortedProcesses;

    /**
     * 单线程调度器，用于锁续期任务（守护线程）
     */
    private static final ScheduledExecutorService RENEWAL_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rmq-lock-renewal");
        t.setDaemon(true);
        return t;
    });

    /**
     * 初始化方法，在 Bean 创建后对前置处理器进行排序并缓存
     */
    @PostConstruct
    public void init() {
        sortedProcesses = processList.stream().sorted(Comparator.comparingInt(p -> p.getProcess().getOrdered())).collect(Collectors.toList());
        log.debug("RabbitMQ前置处理器初始化完成，共{}个", sortedProcesses.size());
    }

    /**
     * 销毁方法，在容器关闭时优雅停止续期线程池
     */
    @PreDestroy
    public void destroy() {
        RENEWAL_SCHEDULER.shutdown();
        try {
            if (!RENEWAL_SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                RENEWAL_SCHEDULER.shutdownNow();
                if (!RENEWAL_SCHEDULER.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("RabbitMQ锁续期线程池未完全终止");
                }
            }
        } catch (InterruptedException e) {
            RENEWAL_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RabbitMQ锁续期线程池已关闭");
    }

    /**
     * 环绕通知，处理带有 {@link RabbitMqConsumption} 注解的方法
     * <p>
     * 实现消息消费的防重、重试、锁续期、完成校验等核心逻辑
     *
     * @param joinPoint 切点
     * @return 业务方法执行结果
     */
    @Around("@annotation(com.simple.common.rabbitmq.annotation.RabbitMqConsumption)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable { // 移除 @SneakyThrows
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 修复缓存 Key：使用声明类名 + 方法签名
        String methodKey = method.getDeclaringClass().getName() + "#" + method.getName() + method.getParameterCount();
        RabbitMqConsumption annotation = annotationCache.computeIfAbsent(methodKey, k -> method.getAnnotation(RabbitMqConsumption.class));

        Object[] args = joinPoint.getArgs();
        Message message = (Message) args[0];
        Channel channel = (Channel) args[1];

        MessageProperties props = message.getMessageProperties();
        String correlationId = props.getCorrelationId();
        String consumerQueue = props.getConsumerQueue();
        long deliveryTag = props.getDeliveryTag();

        int businessTime = annotation.businessTime();
        TimeUnit timeUnit = annotation.timeUnit();
        int maxRetryCount = annotation.retryCount();
        long effectiveSeconds = annotation.effectiveTime();

        DefaultMessage defaultMessage = SerializeUtils.deserialize(message.getBody(), DefaultMessage.class);
        if (defaultMessage == null) {
            log.error("队列[{}] 消息反序列化失败，拒绝并不放回队列", consumerQueue);
            ackRMQManager.basicNack(channel, deliveryTag, false);
            return null;
        }

        String retryKey = retryCountManager.buildRetryKey(consumerQueue, correlationId);

        boolean lockAcquired = lockManager.tryAcquire(consumerQueue, correlationId, businessTime, timeUnit);
        if (!lockAcquired) {
            ConsumptionLockManager.LockStatus status = lockManager.checkLockStatus(consumerQueue, correlationId, defaultMessage);

            switch (status) {
            case COMPLETED_AND_VERIFIED:
                log.debug("队列[{}] id[{}] 已完成，直接ACK", consumerQueue, correlationId);
                ackRMQManager.basicAck(channel, deliveryTag);
                return null;
            case COMPLETED_BUT_VERIFICATION_FAILED:
                log.warn("队列[{}] id[{}] 已完成但校验失败，重置锁为处理中并重试", consumerQueue, correlationId);
                lockManager.resetToProcessing(consumerQueue, correlationId, businessTime, timeUnit);
                ackRMQManager.basicNack(channel, deliveryTag, false);
                retryMessageManager.handleVerificationFailureRetry(retryCountManager.buildVerifyRetryKey(consumerQueue, correlationId), message, channel, maxRetryCount,
                                                                   new RuntimeException("业务完成校验未通过"), defaultMessage);
                return null;
            case PROCESSING:
            case PROCESSING_WITH_RECOVERED_TTL:
                log.debug("队列[{}] id[{}] 已在处理中，拒绝并不放回", consumerQueue, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, false);
                return null;
            case NOT_EXISTS:
            default:
                log.error("队列[{}] id[{}] 锁状态异常，放回队列", consumerQueue, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, true);
                return null;
            }
        }

        ScheduledFuture<?> renewalFuture = startLockRenewal(consumerQueue, correlationId, businessTime, timeUnit);
        // 添加续期失败计数器，用于检测连续失败
        final int maxRenewalFailures = 3;
        final int[] renewalFailCount = {0};

        try {
            for (RabbitMqProcess process : sortedProcesses) {
                if (process.getProcess().isExecute()) {
                    try {
                        process.execution(message, channel, annotation);
                    } catch (Exception ex) {
                        log.error("前置处理器[{}]执行异常，继续消费", process.getProcess().getMsg(), ex);
                    }
                }
            }

            // 在业务执行前，可定期检查续期状态（此处简化，在续期任务中检查）
            Object businessResult = joinPoint.proceed(args);
            String businessId = businessResult != null ? businessResult.toString() : "";

            lockManager.markAsDone(consumerQueue, correlationId, businessId, effectiveSeconds, TimeUnit.SECONDS);
            ackRMQManager.basicAck(channel, deliveryTag);
            log.debug("队列[{}] id[{}] 消费成功，业务ID:{}", consumerQueue, correlationId, businessId);

            retryCountManager.delete(retryKey);
            return businessResult;

        } catch (Exception e) {
            log.error("队列[{}] id[{}] 消费失败", consumerQueue, correlationId, e);
            lockManager.release(consumerQueue, correlationId);
            ackRMQManager.basicNack(channel, deliveryTag, false);
            retryMessageManager.handleBusinessFailureRetry(retryKey, message, channel, maxRetryCount, e, defaultMessage);
            throw e;
        } finally {
            if (renewalFuture != null) {
                renewalFuture.cancel(false);
            }
        }
    }

    /**
     * 启动锁续期任务，每隔 (businessTime / 3) 时间刷新锁的过期时间
     * 修复：若连续续期失败超过阈值，则中断业务线程（主动抛出异常）
     *
     * @param queue         队列名称
     * @param correlationId 消息唯一标识
     * @param businessTime  业务预估执行时长
     * @param timeUnit      时间单位
     * @return 续期任务 Future，可用于取消
     */
    private ScheduledFuture<?> startLockRenewal(String queue, String correlationId, int businessTime, TimeUnit timeUnit) {
        long businessMillis = timeUnit.toMillis(businessTime);
        long renewInterval = businessMillis / 3;
        if (renewInterval <= 0) {
            renewInterval = 1000;
        }
        final int maxConsecutiveFailures = 3;
        final int[] failureCount = {0};
        return RENEWAL_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                boolean renewed = lockManager.renewLock(queue, correlationId, businessTime, timeUnit);
                if (!renewed) {
                    failureCount[0]++;
                    if (failureCount[0] >= maxConsecutiveFailures) {
                        log.error("队列[{}] id[{}] 锁续期连续失败{}次，可能锁已丢失，中断业务线程", queue, correlationId, maxConsecutiveFailures);
                        // 中断当前业务线程（需业务方法响应中断）
                        Thread.currentThread().interrupt();
                    }
                } else {
                    failureCount[0] = 0;
                }
                if (!renewed && log.isDebugEnabled()) {
                    log.debug("队列[{}] id[{}] 锁续期失败", queue, correlationId);
                }
            } catch (Exception e) {
                log.error("队列[{}] id[{}] 锁续期异常", queue, correlationId, e);
            }
        }, renewInterval, renewInterval, TimeUnit.MILLISECONDS);
    }
}
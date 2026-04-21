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
     * 注解属性缓存，使用 method 的完整签名作为 key，避免代理类差异与重载冲突
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
        sortedProcesses = processList.stream()
                                     .filter(rabbitMqProcess -> rabbitMqProcess.getProcess().isExecute())
                                     .sorted(Comparator.comparingInt(p -> p.getProcess().getOrdered()))
                                     .collect(Collectors.toList());
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
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 使用完整方法签名作为缓存 key，避免 CGLIB 代理与重载混淆
        String methodKey = method.toGenericString();
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
            ConsumptionLockManager.LockStatus status = lockManager.checkLockStatus(consumerQueue, correlationId, defaultMessage, message);

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
                log.debug("队列[{}] id[{}] 已在处理中，拒绝并不放回", consumerQueue, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, false);
                return null;
            case PROCESSING_WITH_RECOVERED_TTL:
                // 僵尸锁恢复：不直接丢弃，而是尝试重新竞争锁
                log.info("队列[{}] id[{}] 检测到僵尸锁（TTL异常长），尝试重新竞争锁", consumerQueue, correlationId);
                boolean reAcquired = lockManager.tryAcquire(consumerQueue, correlationId, businessTime, timeUnit);
                if (reAcquired) {
                    log.info("队列[{}] id[{}] 重新获取锁成功，继续消费", consumerQueue, correlationId);
                    // 锁已重新获取，继续往下执行（注意：此处需要跳出 switch 进入业务执行流程）
                    break; // 跳出 switch，继续执行后续业务逻辑
                } else {
                    // 重新获取失败，说明已有其他消费者接手，放回队列让其他消费者处理
                    log.warn("队列[{}] id[{}] 重新获取锁失败，可能已被其他消费者处理，放回队列", consumerQueue, correlationId);
                    ackRMQManager.basicNack(channel, deliveryTag, true);
                    return null;
                }
            case NOT_EXISTS:
            default:
                log.error("队列[{}] id[{}] 锁状态异常，放回队列", consumerQueue, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, true);
                return null;
            }
        }

        final Thread businessThread = Thread.currentThread();
        ScheduledFuture<?> renewalFuture = startLockRenewal(consumerQueue, correlationId, businessTime, timeUnit, businessThread);

        // 标记业务是否成功完成
        boolean businessSuccess = false;
        Object businessResult = null;
        Exception businessException = null;

        try {
            // 执行前置处理器
            for (RabbitMqProcess process : sortedProcesses) {
                if (process.getProcess().isExecute()) {
                    try {
                        process.execution(message, channel, annotation);
                    } catch (Exception ex) {
                        log.error("前置处理器[{}]执行异常", process.getProcess().getMsg(), ex);
                        // 若处理器异常不应继续，可在此根据配置决定是否抛出
                        // 此处保持原有行为：仅记录日志，继续执行
                    }
                }
            }

            // 定期检查中断标志，若已被续期任务中断则主动抛出异常
            if (Thread.interrupted()) {
                throw new InterruptedException("业务线程已被锁续期任务中断");
            }

            businessResult = joinPoint.proceed(args);

            // 业务方法执行完成后再次检查中断
            if (Thread.interrupted()) {
                throw new InterruptedException("业务线程已被锁续期任务中断");
            }

            // 处理业务ID：void 方法或返回 null 时，使用 correlationId 作为兜底业务ID
            String businessId;
            if (businessResult == null || businessResult.toString().trim().isEmpty()) {
                if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
                    businessId = correlationId;
//                    log.debug("业务方法返回 void，使用 correlationId[{}] 作为业务ID", correlationId);
                } else {
                    throw new IllegalStateException("业务方法返回空业务ID，无法标记完成状态");
                }
            } else {
                businessId = businessResult.toString();
            }

            lockManager.markAsDone(consumerQueue, correlationId, businessId, effectiveSeconds, TimeUnit.SECONDS);
            ackRMQManager.basicAck(channel, deliveryTag);
            log.debug("队列[{}] id[{}] 消费成功，业务ID:{}", consumerQueue, correlationId, businessId);

            retryCountManager.delete(retryKey);
            businessSuccess = true;
            return businessResult;

        } catch (Exception e) {
            businessException = e;
            // 若是中断异常，特别记录
            if (e instanceof InterruptedException) {
                log.warn("队列[{}] id[{}] 业务执行被中断，锁续期可能失败", consumerQueue, correlationId);
                Thread.currentThread().interrupt(); // 恢复中断状态
            } else {
                log.error("队列[{}] id[{}] 消费失败", consumerQueue, correlationId, e);
            }
            // 释放锁，因为业务未成功完成
            lockManager.release(consumerQueue, correlationId);
            // 若未被中断，进行重试处理；若已中断，直接NACK并放回队列
            if (!(e instanceof InterruptedException)) {
                ackRMQManager.basicNack(channel, deliveryTag, false);
                retryMessageManager.handleBusinessFailureRetry(retryKey, message, channel, maxRetryCount, e, defaultMessage);
            } else {
                // 中断情况下放回队列，让其他消费者处理
                ackRMQManager.basicNack(channel, deliveryTag, true);
            }
            throw e;
        } finally {
            if (renewalFuture != null) {
                renewalFuture.cancel(false);
            }
            // 若业务成功但后续标记完成失败？已在catch中处理，此处仅做兜底
            if (!businessSuccess && businessException == null) {
                // 理论上不会进入此分支
                lockManager.release(consumerQueue, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, true);
            }
        }
    }

    /**
     * 启动锁续期任务，每隔 (businessTime / 3) 时间刷新锁的过期时间
     * 若连续续期失败超过阈值，则中断业务线程
     *
     * @param queue          队列名称
     * @param correlationId  消息唯一标识
     * @param businessTime   业务预估执行时长
     * @param timeUnit       时间单位
     * @param businessThread 业务线程，用于中断
     * @return 续期任务 Future，可用于取消
     */
    private ScheduledFuture<?> startLockRenewal(String queue, String correlationId, int businessTime, TimeUnit timeUnit, Thread businessThread) {
        long businessMillis = timeUnit.toMillis(businessTime);
        long renewInterval = businessMillis / 3;
        if (renewInterval <= 0) {
            renewInterval = 1000;
        }
        final int maxConsecutiveFailures = 3;
        final int[] failureCount = { 0 };
        return RENEWAL_SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                boolean renewed = lockManager.renewLock(queue, correlationId, businessTime, timeUnit);
                if (!renewed) {
                    failureCount[0]++;
                    if (failureCount[0] >= maxConsecutiveFailures) {
                        log.error("队列[{}] id[{}] 锁续期连续失败{}次，可能锁已丢失，中断业务线程", queue, correlationId, maxConsecutiveFailures);
                        businessThread.interrupt();
                    }
                } else {
                    failureCount[0] = 0;
                }
                if (!renewed && log.isDebugEnabled()) {
                    log.debug("队列[{}] id[{}] 锁续期失败", queue, correlationId);
                }
            } catch (Exception e) {
                log.error("队列[{}] id[{}] 锁续期异常", queue, correlationId, e);
                // Redis异常不计入连续失败次数，但记录日志
            }
        }, renewInterval, renewInterval, TimeUnit.MILLISECONDS);
    }
}
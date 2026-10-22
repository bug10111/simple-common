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
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * RabbitMQ 消息消费切面（v2：锁所有权语义 + 严格 ack 时序）
 * <p>
 * 核心时序契约：
 * <ul>
 *   <li>业务成功：先 CAS 标记完成（失败视为锁丢失，不 ACK），成功后才 basicAck；</li>
 *   <li>业务失败：先投递延迟重试副本（或死信兜底），投递成功后才 basicNack(requeue=false)，
 *       投递失败则 basicNack(requeue=true) 保底，杜绝"已丢原始消息但重试副本未落"的窗口；</li>
 *   <li>锁丢失：由续期任务的显式 AtomicBoolean 标记（辅以线程中断），不依赖可被业务清除的中断标志；</li>
 *   <li>ack 之后的计数清理完全隔离，清理失败仅记日志，绝不反触发 nack/重试。</li>
 * </ul>
 * </p>
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
     * 注解属性缓存，以 method 完整签名为 key（解析失败会抛异常，不存在缓存 null 的问题）
     */
    private final Map<String, RabbitMqConsumption> annotationCache = new ConcurrentHashMap<>();

    /**
     * 排序后的前置处理器列表（初始化后不变，仅包含启用执行的处理器）
     */
    private List<RabbitMqProcess> sortedProcesses;

    /**
     * 锁续期调度线程池（每 Bean 独立创建并在销毁时关闭：
     * 单线程会被慢续期拖垮所有在途消息；static 池在容器重启时会因已 shutdown 拒绝任务）
     */
    private ScheduledExecutorService renewalScheduler;

    /**
     * 初始化方法，在 Bean 创建后对前置处理器排序并创建续期线程池
     * <p>
     * 仅收集枚举声明为执行的处理器；默认装配下无业务处理器（占位处理器 execute=false 被过滤）时列表为空，
     * 责任链执行环节为无害空转，不影响存量消费方
     * </p>
     */
    @PostConstruct
    public void init() {
        // 按枚举声明的执行顺序升序排列，order 值越小越先执行
        sortedProcesses = processList.stream()
                .filter(rabbitMqProcess -> rabbitMqProcess.getProcess().isExecute())
                .sorted(Comparator.comparingInt(process -> process.getProcess().getOrdered()))
                .collect(Collectors.toList());
        int poolSize = Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
        renewalScheduler = Executors.newScheduledThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "rmq-lock-renewal");
            thread.setDaemon(true);
            return thread;
        });
        log.info("RabbitMQ 前置处理器初始化完成，共{}个；锁续期线程池大小：{}", sortedProcesses.size(), poolSize);
    }

    /**
     * 销毁方法，在容器关闭时优雅停止续期线程池
     */
    @PreDestroy
    public void destroy() {
        if (renewalScheduler == null) {
            return;
        }
        renewalScheduler.shutdown();
        try {
            if (!renewalScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                renewalScheduler.shutdownNow();
                if (!renewalScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.warn("RabbitMQ 锁续期线程池未完全终止");
                }
            }
        } catch (InterruptedException e) {
            renewalScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RabbitMQ 锁续期线程池已关闭");
    }

    /**
     * 环绕通知，处理携带 {@link RabbitMqConsumption} 注解的方法
     * <p>
     * 实现消息消费的防重、重试、锁续期、完成校验等核心逻辑
     * </p>
     *
     * @param joinPoint 切点
     * @return 业务方法执行结果
     * @throws Throwable 业务异常原样向上传播（已在此结算投递）
     */
    @Around("@annotation(com.simple.common.rabbitmq.annotation.RabbitMqConsumption)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 经 AopUtils 取最具体方法：JDK 代理场景下 signature.getMethod() 可能是接口方法（注解仅标在实现类上会丢失）
        Method mostSpecificMethod = AopUtils.getMostSpecificMethod(signature.getMethod(), joinPoint.getTarget().getClass());
        // 使用完整方法签名作为缓存 key；解析失败直接抛出带队列名上下文的明确异常，不缓存 null
        RabbitMqConsumption annotation = annotationCache.computeIfAbsent(mostSpecificMethod.toGenericString(),
                key -> resolveAnnotation(mostSpecificMethod));

        Object[] args = joinPoint.getArgs();
        Message message = (Message) args[0];
        Channel channel = (Channel) args[1];

        MessageProperties props = message.getMessageProperties();
        String correlationId = props.getCorrelationId();
        String consumerQueue = props.getConsumerQueue();
        long deliveryTag = props.getDeliveryTag();

        int businessTime = annotation.businessTime();
        if (businessTime <= 0) {
            throw new IllegalStateException("注解 businessTime 必须大于 0，队列[" + consumerQueue + "]当前值: " + businessTime);
        }
        TimeUnit timeUnit = annotation.timeUnit();
        int maxRetryCount = Math.max(0, annotation.retryCount());
        long effectiveSeconds = annotation.effectiveTime();

        DefaultMessage defaultMessage = SerializeUtils.deserialize(message.getBody(), DefaultMessage.class);
        if (defaultMessage == null) {
            log.error("队列[{}] 消息反序列化失败，拒绝并不放回队列", consumerQueue);
            ackRMQManager.basicNack(channel, deliveryTag, false);
            return null;
        }

        String retryKey = retryCountManager.buildRetryKey(consumerQueue, correlationId);

        // 第一步：竞争锁；失败则按锁状态分流（部分分支可能接管恢复态锁后进入业务执行）
        String ownerToken = lockManager.tryAcquire(consumerQueue, correlationId, businessTime, timeUnit);
        if (ownerToken == null) {
            ownerToken = resolveLockConflict(joinPoint, channel, message, defaultMessage, annotation,
                    consumerQueue, correlationId, deliveryTag, retryKey, businessTime, timeUnit);
            if (ownerToken == null) {
                // 投递已在冲突分支结算
                return null;
            }
        }
        return executeWithLock(joinPoint, channel, message, defaultMessage, retryKey,
                consumerQueue, correlationId, deliveryTag, ownerToken, businessTime, effectiveSeconds, timeUnit, maxRetryCount, annotation);
    }

    /**
     * 持锁执行业务：续期看护 -> 前置责任链 -> 业务执行 -> CAS 标记完成 -> ACK -> 隔离清理
     */
    private Object executeWithLock(ProceedingJoinPoint joinPoint, Channel channel, Message message,
                                   DefaultMessage defaultMessage, String retryKey, String consumerQueue, String correlationId,
                                   long deliveryTag, String ownerToken, int businessTime, long effectiveSeconds,
                                   TimeUnit timeUnit, int maxRetryCount, RabbitMqConsumption annotation) throws Throwable {
        AtomicBoolean lockLost = new AtomicBoolean(false);
        Thread businessThread = Thread.currentThread();
        ScheduledFuture<?> renewalFuture = scheduleLockRenewal(consumerQueue, correlationId, ownerToken,
                businessTime, timeUnit, lockLost, businessThread);

        try {
            // 执行前置处理器责任链，任一处理器异常仅记录日志并继续后续处理器，不阻断消费流程
            for (RabbitMqProcess process : sortedProcesses) {
                try {
                    process.execution(message, channel, annotation);
                } catch (Exception ex) {
                    log.error("前置处理器[{}]执行异常", process.getProcess().getMsg(), ex);
                }
            }

            // 锁丢失以显式标志为准（续期任务设置），中断标志仅作辅助信号
            checkLockLost(lockLost, consumerQueue, correlationId);

            Object businessResult = joinPoint.proceed();

            // 业务方法返回后、标记完成前再次检查锁归属
            checkLockLost(lockLost, consumerQueue, correlationId);

            // 处理业务ID：void 方法或返回 null 时，使用 correlationId 作为兜底业务ID
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            String businessId = resolveBusinessId(businessResult, method, consumerQueue, correlationId);

            // CAS 标记完成：失败说明锁已被他人持有，绝不能 ACK（否则下一个消费者会重复执行业务）
            if (!lockManager.markAsDone(consumerQueue, correlationId, ownerToken, businessId, effectiveSeconds, TimeUnit.SECONDS)) {
                throw new LockLostException("队列[" + consumerQueue + "] id[" + correlationId + "] 标记完成失败，锁已被其他消费者接管");
            }
            ackRMQManager.basicAck(channel, deliveryTag, false);
            log.debug("队列[{}] id[{}] 消费成功，业务ID:{}", consumerQueue, correlationId, businessId);

            // ack 后的计数清理完全隔离：失败仅记日志，绝不反触发 nack/重试/重复业务执行
            try {
                retryCountManager.delete(retryKey);
            } catch (Exception cleanupEx) {
                log.warn("队列[{}] id[{}] 消费成功后清理重试计数失败（不影响本次消费结果，残留计数依赖 TTL 兜底）",
                        consumerQueue, correlationId, cleanupEx);
            }
            return businessResult;

        } catch (LockLostException lostEx) {
            log.warn("队列[{}] id[{}] 锁丢失，放回队列由其他消费者处理: {}", consumerQueue, correlationId, lostEx.getMessage());
            releaseQuietly(consumerQueue, correlationId, ownerToken);
            settleQuietly(channel, deliveryTag, true);
            return null;
        } catch (InterruptedException interrupted) {
            // 中断（含与锁丢失无关的外部中断）：恢复中断位、放回队列，不做重试副本投递（重试语义由 broker 重投完成）
            log.warn("队列[{}] id[{}] 业务执行被中断，放回队列", consumerQueue, correlationId, interrupted);
            Thread.currentThread().interrupt();
            releaseQuietly(consumerQueue, correlationId, ownerToken);
            settleQuietly(channel, deliveryTag, true);
            return null;
        } catch (Exception businessEx) {
            log.error("队列[{}] id[{}] 消费失败", consumerQueue, correlationId, businessEx);
            releaseQuietly(consumerQueue, correlationId, ownerToken);
            // 时序：先投递重试副本（或死信兜底），投递成功后才 nack 丢弃原始投递；
            // 投递失败则 requeue=true 保底，杜绝"原始消息已丢但重试副本未落"的丢失窗口
            try {
                retryMessageManager.handleBusinessFailureRetry(retryKey, message, channel, maxRetryCount, businessEx, defaultMessage);
                ackRMQManager.basicNack(channel, deliveryTag, false);
            } catch (Exception retryEx) {
                log.error("队列[{}] id[{}] 重试副本投递失败，原始消息放回队列保底", consumerQueue, correlationId, retryEx);
                settleQuietly(channel, deliveryTag, true);
            }
            throw businessEx;
        } catch (Throwable fatal) {
            // Error 等非受检 Throwable：显式结算投递并释放锁后再传播，不依赖通道关闭后的 broker 重投兜底
            log.error("队列[{}] id[{}] 非受检 Throwable，释放锁并放回队列后向上传播", consumerQueue, correlationId, fatal);
            releaseQuietly(consumerQueue, correlationId, ownerToken);
            settleQuietly(channel, deliveryTag, true);
            throw fatal;
        } finally {
            if (renewalFuture != null) {
                renewalFuture.cancel(false);
            }
        }
    }

    /**
     * 处理锁竞争：按锁状态分流决策。返回接管到的 ownerToken（进入业务执行），返回 null 表示投递已在本方法内结算
     */
    private String resolveLockConflict(ProceedingJoinPoint joinPoint, Channel channel, Message message,
                                       DefaultMessage defaultMessage, RabbitMqConsumption annotation,
                                       String consumerQueue, String correlationId, long deliveryTag, String retryKey,
                                       int businessTime, TimeUnit timeUnit) {
        ConsumptionLockManager.LockView lockView = lockManager.checkLockStatus(consumerQueue, correlationId, defaultMessage, message);
        switch (lockView.status()) {
            case COMPLETED_AND_VERIFIED:
                log.debug("队列[{}] id[{}] 已完成且校验通过，直接ACK", consumerQueue, correlationId);
                ackRMQManager.basicAck(channel, deliveryTag, false);
                return null;
            case COMPLETED_BUT_VERIFICATION_FAILED:
                log.warn("队列[{}] id[{}] 已完成但校验未通过，释放完成锁并投递延迟重试", consumerQueue, correlationId);
                // 条件删除完成锁（按期望值 CAS）：成功后才投递重试副本，失败说明状态已被其他消费者变更，放回队列保底
                if (!lockManager.releaseIfValue(consumerQueue, correlationId, lockView.value())) {
                    log.warn("队列[{}] id[{}] 完成锁状态已变化，放弃重试并放回队列", consumerQueue, correlationId);
                    settleQuietly(channel, deliveryTag, true);
                    return null;
                }
                try {
                    String verifyRetryKey = retryCountManager.buildVerifyRetryKey(consumerQueue, correlationId);
                    retryMessageManager.handleVerificationFailureRetry(verifyRetryKey, message, channel,
                            Math.max(0, annotation.retryCount()), new RuntimeException("业务完成校验未通过"), defaultMessage);
                    ackRMQManager.basicNack(channel, deliveryTag, false);
                } catch (Exception publishEx) {
                    log.error("队列[{}] id[{}] 校验失败重试投递异常，放回队列保底", consumerQueue, correlationId, publishEx);
                    settleQuietly(channel, deliveryTag, true);
                }
                return null;
            case PROCESSING:
                // 心跳语义下能落到此分支的都是确有消费者在处理的存活锁（僵尸锁已归入恢复态），拒绝重复消费
                log.debug("队列[{}] id[{}] 正在被其他消费者处理，拒绝并不放回", consumerQueue, correlationId);
                ackRMQManager.basicNack(channel, deliveryTag, false);
                return null;
            case PROCESSING_WITH_RECOVERED_TTL:
                // 僵尸锁恢复：原子接管（接管脚本二次校验心跳，原持有者续期会让接管失败），成功则继续消费
                log.info("队列[{}] id[{}] 检测到心跳静默的疑似僵尸锁，尝试原子接管", consumerQueue, correlationId);
                String token = lockManager.takeOverRecoveredLock(consumerQueue, correlationId, businessTime, timeUnit);
                if (token != null) {
                    log.info("队列[{}] id[{}] 接管恢复态锁成功，继续消费", consumerQueue, correlationId);
                    return token;
                }
                log.info("队列[{}] id[{}] 接管失败（锁已消失/被他人接管），放回队列", consumerQueue, correlationId);
                settleQuietly(channel, deliveryTag, true);
                return null;
            case NOT_EXISTS:
            default:
                // 锁在竞争与检查之间过期：再竞争一次，仍失败则放回队列（不放回会造成消息静默丢失）
                log.debug("队列[{}] id[{}] 锁已消失，重新竞争", consumerQueue, correlationId);
                String reAcquired = lockManager.tryAcquire(consumerQueue, correlationId, businessTime, timeUnit);
                if (reAcquired != null) {
                    return reAcquired;
                }
                log.warn("队列[{}] id[{}] 锁重新竞争失败，放回队列", consumerQueue, correlationId);
                settleQuietly(channel, deliveryTag, true);
                return null;
        }
    }

    /**
     * 启动锁续期任务，每隔 (businessTime / 3) 刷新锁的过期时间与心跳
     * <p>
     * 锁丢失判定：连续 {@value #MAX_CONTINUOUS_RENEW_FAILURES} 次"值不匹配/锁不存在"（区别于 Redis 异常）才判定丢失，
     * 并置显式 {@code lockLost} 标志（业务线程以该标志为准，辅以中断）。
     * </p>
     *
     * @param queue          队列名称
     * @param correlationId  消息唯一标识
     * @param ownerToken     锁持有凭证
     * @param businessTime   业务预估执行时长
     * @param timeUnit       时间单位
     * @param lockLost       锁丢失标志（由本任务设置，业务线程检查）
     * @param businessThread 业务线程，用于中断
     * @return 续期任务 Future，可用于取消
     */
    private ScheduledFuture<?> scheduleLockRenewal(String queue, String correlationId, String ownerToken,
                                                   int businessTime, TimeUnit timeUnit, AtomicBoolean lockLost, Thread businessThread) {
        long businessMillis = timeUnit.toMillis(businessTime);
        long renewInterval = Math.max(1000L, businessMillis / 3);
        AtomicInteger continuousFailures = new AtomicInteger(0);
        return renewalScheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean renewed = lockManager.renewLock(queue, correlationId, ownerToken, businessTime, timeUnit);
                if (renewed) {
                    continuousFailures.set(0);
                } else if (continuousFailures.incrementAndGet() >= MAX_CONTINUOUS_RENEW_FAILURES) {
                    // 值不匹配/锁不存在：持有权确已丢失（Redis 抖动走异常分支，不计入），显式置位并中断业务
                    log.error("队列[{}] id[{}] 锁续期连续{}次失败（锁已丢失），中断业务线程", queue, correlationId, MAX_CONTINUOUS_RENEW_FAILURES);
                    lockLost.set(true);
                    businessThread.interrupt();
                } else {
                    log.debug("队列[{}] id[{}] 锁续期未命中（第{}次）", queue, correlationId, continuousFailures.get());
                }
            } catch (Exception e) {
                // Redis 基础设施异常不计入锁丢失（锁可能仍然存活），仅记录日志等待下次续期
                log.warn("队列[{}] id[{}] 锁续期异常（Redis 不可用？），不计入锁丢失", queue, correlationId, e);
            }
        }, renewInterval, renewInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 检查锁丢失标志并抛出 {@link LockLostException}
     */
    private void checkLockLost(AtomicBoolean lockLost, String consumerQueue, String correlationId) {
        if (lockLost.get()) {
            throw new LockLostException("队列[" + consumerQueue + "] id[" + correlationId + "] 锁已丢失（续期连续失败），终止本次消费");
        }
    }

    /**
     * 解析业务ID：void 方法返回 null 时使用 correlationId 兜底；非 void 返回空白视为契约错误
     */
    private String resolveBusinessId(Object businessResult, Method method, String consumerQueue, String correlationId) {
        if (businessResult == null || businessResult.toString().trim().isEmpty()) {
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class || returnType == Void.class) {
                return correlationId;
            }
            throw new IllegalStateException("业务方法返回空业务ID，无法标记完成状态，队列[" + consumerQueue + "] id[" + correlationId + "]");
        }
        return businessResult.toString();
    }

    /**
     * 条件释放锁（best-effort）：释放异常仅记日志，保证后续投递结算一定执行
     */
    private void releaseQuietly(String consumerQueue, String correlationId, String ownerToken) {
        try {
            boolean released = lockManager.release(consumerQueue, correlationId, ownerToken);
            if (!released) {
                log.warn("队列[{}] id[{}] 释放锁未命中（锁已被他人接管或已过期），跳过", consumerQueue, correlationId);
            }
        } catch (Exception releaseEx) {
            log.error("队列[{}] id[{}] 释放锁失败（Redis 异常），锁将依赖 TTL 兜底过期", consumerQueue, correlationId, releaseEx);
        }
    }

    /**
     * 投递结算（nack）best-effort：结算异常仅记日志，不掩盖原始业务异常
     */
    private void settleQuietly(Channel channel, long deliveryTag, boolean requeue) {
        try {
            ackRMQManager.basicNack(channel, deliveryTag, requeue);
        } catch (Exception settleEx) {
            log.error("消息 nack 结算失败，deliveryTag={}，依赖通道恢复后 broker 重投", deliveryTag, settleEx);
        }
    }

    /**
     * 解析注解，找不到时抛出带明确上下文的异常（失败不进入缓存，每条消息都能得到明确报错）
     */
    private RabbitMqConsumption resolveAnnotation(Method method) {
        RabbitMqConsumption annotation = AnnotatedElementUtils.findMergedAnnotation(method, RabbitMqConsumption.class);
        if (annotation == null) {
            throw new IllegalStateException("方法[" + method + "]未找到 @RabbitMqConsumption 注解："
                    + "请确认注解标注在监听方法上（JDK 代理场景下不要仅标注在接口方法）");
        }
        return annotation;
    }

    /**
     * 锁丢失异常：终止本次消费并把消息放回队列（不属于业务失败，不产生重试副本）
     */
    private static class LockLostException extends RuntimeException {
        private LockLostException(String message) {
            super(message);
        }
    }

    /**
     * 判定锁丢失的连续续期失败阈值
     */
    private static final int MAX_CONTINUOUS_RENEW_FAILURES = 3;
}

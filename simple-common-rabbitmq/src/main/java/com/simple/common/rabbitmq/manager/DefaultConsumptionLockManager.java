package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationRouterManager;
import com.simple.common.rabbitmq.common.manager.CompletionVerificationStrategyManager;
import com.simple.common.rabbitmq.common.manager.ConsumptionLockManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 消费防重锁管理器默认实现（支持续期）
 *
 * @author qty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultConsumptionLockManager implements ConsumptionLockManager {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqProperties properties;
    private final CompletionVerificationRouterManager verificationRouterManager;

    /**
     * Lua脚本：仅当值等于期望值时设置过期时间
     */
    private static final String EXPIRE_IF_VALUE_EQUALS_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
            "else " +
            "    return 0 " +
            "end";

    private final DefaultRedisScript<Long> expireIfValueEqualsScript =
            new DefaultRedisScript<>(EXPIRE_IF_VALUE_EQUALS_SCRIPT, Long.class);

    /**
     * 构建防重锁的 Redis Key
     *
     * @param queue         队列名称
     * @param correlationId 消息唯一标识
     * @return Redis Key
     */
    @Override
    public String buildLockKey(String queue, String correlationId) {
        return properties.getWhetherToConsume() + ":" + queue + ":" + correlationId;
    }

    /**
     * 尝试获取消费锁，使用 Redis SET NX EX 原子操作
     *
     * @param queue         队列名称
     * @param correlationId 消息唯一标识
     * @param businessTime  业务预估执行时长
     * @param timeUnit      时间单位
     * @return true-获取成功，false-锁已存在
     */
    @Override
    public boolean tryAcquire(String queue, String correlationId, int businessTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, properties.getLockStatusProcessing(), businessTime, timeUnit);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 续期锁的过期时间，仅当锁的值仍为 PROCESSING 时成功
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param businessTime  业务预估时长
     * @param timeUnit      时间单位
     * @return true-续期成功，false-锁不存在或已变更
     */
    @Override
    public boolean renewLock(String queue, String correlationId, int businessTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        long expireSeconds = timeUnit.toSeconds(businessTime);
        try {
            Long result = redisTemplate.execute(expireIfValueEqualsScript,
                    Collections.singletonList(lockKey),
                    properties.getLockStatusProcessing(),
                    String.valueOf(expireSeconds));
            return result != null && result == 1L;
        } catch (Exception e) {
            log.error("Redis续期操作异常，锁[{}]续期失败", lockKey, e);
            return false;
        }
    }

    /**
     * 检查已存在的锁状态，返回处理建议
     *
     * @param queue          队列名
     * @param correlationId  消息ID
     * @param defaultMessage 消息体
     * @param message        原始Message（用于校验策略）
     * @return 锁状态及建议操作
     */
    @Override
    public LockStatus checkLockStatus(String queue, String correlationId, DefaultMessage defaultMessage, Message message) {
        return checkLockStatusWithLoop(queue, correlationId, defaultMessage, message);
    }

    /**
     * 有限循环检查锁状态，避免递归深度过大或状态变化导致的误判
     */
    private LockStatus checkLockStatusWithLoop(String queue, String correlationId,
                                               DefaultMessage defaultMessage, Message message) {
        int maxAttempts = 2;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String lockKey = buildLockKey(queue, correlationId);
            String status = redisTemplate.opsForValue().get(lockKey);

            if (status == null) {
                return LockStatus.NOT_EXISTS;
            }

            if (status.startsWith(properties.getLockStatusDonePrefix())) {
                String businessId = status.substring(properties.getLockStatusDonePrefix().length());
                CompletionVerificationStrategyManager strategy = verificationRouterManager.getStrategy(queue);
                if (strategy != null && !strategy.isCompleted(businessId, defaultMessage, message)) {
                    return LockStatus.COMPLETED_BUT_VERIFICATION_FAILED;
                }
                return LockStatus.COMPLETED_AND_VERIFIED;
            }

            Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            if (ttl != null && ttl > properties.getLockTtlRecoveryThresholdSeconds()) {
                Long shortened = redisTemplate.execute(expireIfValueEqualsScript,
                        Collections.singletonList(lockKey),
                        properties.getLockStatusProcessing(),
                        String.valueOf(properties.getLockTtlRecoveryThresholdSeconds()));
                if (shortened != null && shortened == 1L) {
                    return LockStatus.PROCESSING_WITH_RECOVERED_TTL;
                } else {
                    // 缩短失败，说明锁值已变化，重新检查
                    log.debug("锁[{}]缩短TTL失败，值已变更，重新检查状态 (尝试次数 {})", lockKey, attempt + 1);
                    continue;
                }
            }
            return LockStatus.PROCESSING;
        }
        // 多次尝试后仍不稳定，保守返回 PROCESSING
        log.warn("队列[{}] id[{}] 锁状态多次检查仍不稳定，返回PROCESSING", queue, correlationId);
        return LockStatus.PROCESSING;
    }

    /**
     * 删除锁
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     */
    @Override
    public void release(String queue, String correlationId) {
        redisTemplate.delete(buildLockKey(queue, correlationId));
    }

    /**
     * 将锁标记为已完成
     *
     * @param queue           队列名
     * @param correlationId   消息ID
     * @param businessId      业务返回的唯一标识
     * @param effectiveSeconds 完成状态有效时长
     * @param timeUnit        时间单位
     */
    @Override
    public void markAsDone(String queue, String correlationId, String businessId, long effectiveSeconds,
                           TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        String doneStatus = properties.getLockStatusDonePrefix() + businessId;
        redisTemplate.opsForValue().set(lockKey, doneStatus, effectiveSeconds, timeUnit);
    }

    /**
     * 重置锁为处理中状态（用于校验失败时重新进入处理状态）
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param businessTime  业务预估时长
     * @param timeUnit      时间单位
     */
    @Override
    public void resetToProcessing(String queue, String correlationId, int businessTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        long expireSeconds = timeUnit.toSeconds(businessTime);
        redisTemplate.opsForValue().set(lockKey, properties.getLockStatusProcessing(), expireSeconds, TimeUnit.SECONDS);
    }
}
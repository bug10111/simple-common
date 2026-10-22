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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 消费防重锁管理器默认实现（v2：ownerToken + 心跳 + Lua CAS 原子操作）。
 * <p>
 * 锁值格式：处理中为 {@code <status>:<心跳毫秒>:<token>}，完成为 {@code <donePrefix><businessId>}；
 * 全部状态迁移均以 Lua 脚本原子完成，杜绝"检查-决策-执行"窗口内的 TOCTOU 竞态。
 * v1 存量键的兼容语义见接口 javadoc。
 * </p>
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
     * 检查锁状态原子脚本。ARGV: [donePrefix, nowMillis, 心跳静默阈值millis, 恢复态缩短TTL millis]
     * 返回 {code, aux, value}：0-不存在；2-完成锁；3-无法解析的旧格式处理锁（视为存活）；4-恢复态（已缩短TTL）；
     * 5-正常处理中
     */
    private static final String CHECK_LOCK_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) " +
            "if not v then return {0, '0', ''} end " +
            "if string.sub(v, 1, string.len(ARGV[1])) == ARGV[1] then return {2, tostring(redis.call('PTTL', KEYS[1])), v} end " +
            "local heartbeat = string.match(v, '^[^:]*:(%d+):') " +
            "if not heartbeat then return {3, tostring(redis.call('PTTL', KEYS[1])), v} end " +
            "local age = tonumber(ARGV[2]) - tonumber(heartbeat) " +
            "if age > tonumber(ARGV[3]) then " +
            "    redis.call('PEXPIRE', KEYS[1], ARGV[4]) " +
            "    return {4, tostring(age), v} " +
            "end " +
            "return {5, tostring(redis.call('PTTL', KEYS[1])), v}";

    /**
     * token 校验续期脚本（同时刷新心跳）。ARGV: [ownerToken, 新锁值, TTL millis]
     */
    private static final String RENEW_IF_OWNER_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) " +
            "if not v then return 0 end " +
            "local token = string.match(v, '^[^:]*:%d+:(.+)$') " +
            "if not token or token ~= ARGV[1] then return 0 end " +
            "redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]) " +
            "return 1";

    /**
     * token 校验标记完成脚本。ARGV: [ownerToken, 完成值, TTL millis]
     */
    private static final String MARK_DONE_IF_OWNER_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) " +
            "if not v then return 0 end " +
            "local token = string.match(v, '^[^:]*:%d+:(.+)$') " +
            "if not token or token ~= ARGV[1] then return 0 end " +
            "redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3]) " +
            "return 1";

    /**
     * token 校验释放脚本（compare-and-delete，绝不误删他人锁）。ARGV: [ownerToken]
     */
    private static final String RELEASE_IF_OWNER_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) " +
            "if not v then return 0 end " +
            "local token = string.match(v, '^[^:]*:%d+:(.+)$') " +
            "if not token or token ~= ARGV[1] then return 0 end " +
            "return redis.call('DEL', KEYS[1])";

    /**
     * 期望值条件删除脚本。ARGV: [expectedValue]
     */
    private static final String RELEASE_IF_VALUE_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) " +
            "if not v or v ~= ARGV[1] then return 0 end " +
            "return redis.call('DEL', KEYS[1])";

    /**
     * 恢复态接管脚本（接管前二次校验心跳，原持有者续期会令接管失败）。
     * ARGV: [nowMillis, donePrefix, 心跳静默阈值millis, 新锁值, TTL millis]
     */
    private static final String TAKE_OVER_SCRIPT =
            "local v = redis.call('GET', KEYS[1]) " +
            "if not v then return 0 end " +
            "if string.sub(v, 1, string.len(ARGV[2])) == ARGV[2] then return 0 end " +
            "local heartbeat = string.match(v, '^[^:]*:(%d+):') " +
            "if not heartbeat then return 0 end " +
            "if tonumber(ARGV[1]) - tonumber(heartbeat) <= tonumber(ARGV[3]) then return 0 end " +
            "redis.call('SET', KEYS[1], ARGV[4], 'PX', ARGV[5]) " +
            "return 1";

    private final DefaultRedisScript<List> checkLockScript = new DefaultRedisScript<>(CHECK_LOCK_SCRIPT, List.class);
    private final DefaultRedisScript<Long> renewIfOwnerScript = new DefaultRedisScript<>(RENEW_IF_OWNER_SCRIPT, Long.class);
    private final DefaultRedisScript<Long> markDoneIfOwnerScript = new DefaultRedisScript<>(MARK_DONE_IF_OWNER_SCRIPT, Long.class);
    private final DefaultRedisScript<Long> releaseIfOwnerScript = new DefaultRedisScript<>(RELEASE_IF_OWNER_SCRIPT, Long.class);
    private final DefaultRedisScript<Long> releaseIfValueScript = new DefaultRedisScript<>(RELEASE_IF_VALUE_SCRIPT, Long.class);
    private final DefaultRedisScript<Long> takeOverScript = new DefaultRedisScript<>(TAKE_OVER_SCRIPT, Long.class);

    @Override
    public String buildLockKey(String queue, String correlationId) {
        if (!StringUtils.hasText(queue) || !StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException("queue 和 correlationId 不能为空，queue=" + queue + ", correlationId=" + correlationId);
        }
        return properties.getWhetherToConsume() + ":" + queue + ":" + correlationId;
    }

    @Override
    public String tryAcquire(String queue, String correlationId, long businessTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        long ttlMillis = requirePositiveTtl(businessTime, timeUnit, lockKey);
        String ownerToken = UUID.randomUUID().toString();
        String value = processingValue(ownerToken);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, value, ttlMillis, TimeUnit.MILLISECONDS);
        return Boolean.TRUE.equals(success) ? ownerToken : null;
    }

    @Override
    public boolean renewLock(String queue, String correlationId, String ownerToken, long businessTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        long ttlMillis = timeUnit.toMillis(businessTime);
        if (ttlMillis <= 0) {
            log.warn("续期时长非法[businessTime={} {}]，锁[{}]续期失败", businessTime, timeUnit, lockKey);
            return false;
        }
        if (!StringUtils.hasText(ownerToken)) {
            throw new IllegalArgumentException("ownerToken 不能为空");
        }
        String newValue = processingValue(ownerToken);
        // 仅脚本内部异常向上抛（Redis 不可用），值不匹配/锁不存在返回 false（锁丢失）
        Long result = redisTemplate.execute(renewIfOwnerScript, List.of(lockKey), ownerToken, newValue, String.valueOf(ttlMillis));
        return result != null && result == 1L;
    }

    @Override
    public LockView checkLockStatus(String queue, String correlationId, DefaultMessage defaultMessage, Message message) {
        String lockKey = buildLockKey(queue, correlationId);
        long nowMillis = System.currentTimeMillis();
        long silenceThresholdMillis = TimeUnit.SECONDS.toMillis(properties.getLockTtlRecoveryThresholdSeconds());
        long recoveredTtlMillis = Math.max(1L, silenceThresholdMillis);
        List<String> result = redisTemplate.execute(checkLockScript, List.of(lockKey),
                properties.getLockStatusDonePrefix(), String.valueOf(nowMillis),
                String.valueOf(silenceThresholdMillis), String.valueOf(recoveredTtlMillis));
        if (result == null || result.isEmpty()) {
            return new LockView(LockStatus.NOT_EXISTS, null, null);
        }
        int code = Integer.parseInt(result.get(0));
        String value = result.size() > 2 ? result.get(2) : null;
        if (code == 0) {
            return new LockView(LockStatus.NOT_EXISTS, null, null);
        }
        if (code == 2) {
            // 完成锁：取 businessId 交由校验策略判定；策略异常向上传播（不吞，防止误判为已完成丢消息）
            String businessId = value != null && value.length() > properties.getLockStatusDonePrefix().length()
                    ? value.substring(properties.getLockStatusDonePrefix().length()) : "";
            CompletionVerificationStrategyManager strategy = verificationRouterManager.getStrategy(queue);
            if (strategy != null && !strategy.isCompleted(businessId, defaultMessage, message)) {
                return new LockView(LockStatus.COMPLETED_BUT_VERIFICATION_FAILED, businessId, value);
            }
            return new LockView(LockStatus.COMPLETED_AND_VERIFIED, businessId, value);
        }
        if (code == 3) {
            // 旧版本纯 PROCESSING 值（无心跳段）：保守视为存活处理中，只等 TTL 过期
            log.debug("锁[{}]为旧格式处理值，视为存活处理中", lockKey);
            return new LockView(LockStatus.PROCESSING, null, value);
        }
        if (code == 4) {
            log.info("锁[{}]心跳静默超过阈值({}ms)，判定为疑似僵尸锁，已缩短TTL等待接管", lockKey, silenceThresholdMillis);
            return new LockView(LockStatus.PROCESSING_WITH_RECOVERED_TTL, null, value);
        }
        return new LockView(LockStatus.PROCESSING, null, value);
    }

    @Override
    public String takeOverRecoveredLock(String queue, String correlationId, long businessTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        long ttlMillis = requirePositiveTtl(businessTime, timeUnit, lockKey);
        long nowMillis = System.currentTimeMillis();
        long silenceThresholdMillis = TimeUnit.SECONDS.toMillis(properties.getLockTtlRecoveryThresholdSeconds());
        String newToken = UUID.randomUUID().toString();
        Long result = redisTemplate.execute(takeOverScript, List.of(lockKey),
                String.valueOf(nowMillis), properties.getLockStatusDonePrefix(),
                String.valueOf(silenceThresholdMillis), processingValue(newToken), String.valueOf(ttlMillis));
        return result != null && result == 1L ? newToken : null;
    }

    @Override
    public boolean release(String queue, String correlationId, String ownerToken) {
        String lockKey = buildLockKey(queue, correlationId);
        if (!StringUtils.hasText(ownerToken)) {
            throw new IllegalArgumentException("ownerToken 不能为空");
        }
        Long result = redisTemplate.execute(releaseIfOwnerScript, List.of(lockKey), ownerToken);
        return result != null && result == 1L;
    }

    @Override
    public boolean releaseIfValue(String queue, String correlationId, String expectedValue) {
        String lockKey = buildLockKey(queue, correlationId);
        if (expectedValue == null) {
            throw new IllegalArgumentException("expectedValue 不能为 null");
        }
        Long result = redisTemplate.execute(releaseIfValueScript, List.of(lockKey), expectedValue);
        return result != null && result == 1L;
    }

    @Override
    public boolean markAsDone(String queue, String correlationId, String ownerToken, String businessId, long effectiveTime, TimeUnit timeUnit) {
        String lockKey = buildLockKey(queue, correlationId);
        long ttlMillis = requirePositiveTtl(effectiveTime, timeUnit, lockKey);
        if (!StringUtils.hasText(ownerToken)) {
            throw new IllegalArgumentException("ownerToken 不能为空");
        }
        if (businessId == null) {
            throw new IllegalArgumentException("businessId 不能为 null");
        }
        String doneValue = properties.getLockStatusDonePrefix() + businessId;
        Long result = redisTemplate.execute(markDoneIfOwnerScript, List.of(lockKey), ownerToken, doneValue, String.valueOf(ttlMillis));
        return result != null && result == 1L;
    }

    /**
     * 组装处理中锁值：{@code <status>:<当前心跳毫秒>:<token>}
     */
    private String processingValue(String ownerToken) {
        return properties.getLockStatusProcessing() + ":" + System.currentTimeMillis() + ":" + ownerToken;
    }

    /**
     * TTL 下限保护：换算后必须大于 0，否则 Redis PX 0 会直接报错或锁立即过期
     */
    private long requirePositiveTtl(long time, TimeUnit timeUnit, String lockKey) {
        if (timeUnit == null) {
            throw new IllegalArgumentException("timeUnit 不能为 null，lockKey=" + lockKey);
        }
        long ttlMillis = timeUnit.toMillis(time);
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("锁时长换算后必须大于 0，time=" + time + ", timeUnit=" + timeUnit + ", lockKey=" + lockKey);
        }
        return ttlMillis;
    }
}

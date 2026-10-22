package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.manager.RetryCountManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * 基于 Redis 的重试计数管理器
 *
 * @author qty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRetryCountManager implements RetryCountManager {

    private final StringRedisTemplate redisTemplate;
    private final RabbitMqProperties properties;

    // Lua脚本：INCR并设置过期时间（每次调用都刷新TTL，滑动窗口；原子操作）
    private static final String INCR_WITH_EXPIRE_SCRIPT =
            "local current = redis.call('INCR', KEYS[1])\n" +
            "redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
            "return current";

    private final DefaultRedisScript<Long> incrScript = new DefaultRedisScript<>(INCR_WITH_EXPIRE_SCRIPT, Long.class);

    @Override
    public long incrementAndGet(String retryKey) {
        Long current = redisTemplate.execute(incrScript,
                Collections.singletonList(retryKey),
                String.valueOf(properties.getRetryCountTtlSeconds()));
        if (current == null) {
            // 脚本无返回值说明计数未持久化（管道/事务或连接异常），伪造计数会导致消息被无限重试，必须快速失败
            throw new IllegalStateException("Redis 重试计数脚本未返回结果，key=" + retryKey);
        }
        return current;
    }

    @Override
    public void delete(String retryKey) {
        Boolean removed = redisTemplate.delete(retryKey);
        if (!Boolean.TRUE.equals(removed)) {
            // 删除失败或 Key 已不存在：残留计数靠 TTL 兜底过期，但需可观测，避免同 id 消息被提前判死
            log.warn("清理重试计数 Key 失败或 Key 不存在，key={}", retryKey);
        }
    }

    @Override
    public String buildRetryKey(String queue, String correlationId) {
        if (!StringUtils.hasText(queue) || !StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException("queue 和 correlationId 不能为空，queue=" + queue + ", correlationId=" + correlationId);
        }
        return properties.getCalculation() + ":" + queue + ":" + correlationId;
    }

    @Override
    public String buildVerifyRetryKey(String queue, String correlationId) {
        if (!StringUtils.hasText(queue) || !StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException("queue 和 correlationId 不能为空，queue=" + queue + ", correlationId=" + correlationId);
        }
        // 独立固定前缀段，避免与业务 Key 空间重叠（旧格式 ":verify" 后缀存在拼接碰撞风险）
        return properties.getCalculation() + ":verify:" + queue + ":" + correlationId;
    }
}

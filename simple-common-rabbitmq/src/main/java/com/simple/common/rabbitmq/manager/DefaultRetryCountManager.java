package com.simple.common.rabbitmq.manager;

import com.simple.common.rabbitmq.common.manager.RetryCountManager;
import com.simple.common.rabbitmq.common.properties.RabbitMqProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 基于 Redis 的重试计数管理器
 *
 * @author qty
 */
@Component
@RequiredArgsConstructor
public class DefaultRetryCountManager implements RetryCountManager {

    private final StringRedisTemplate redisTemplate;

    private final RabbitMqProperties properties;

    private static final String INCR_WITH_EXPIRE_SCRIPT =
                    "local current = redis.call('INCR', KEYS[1]) " + "if current == 1 then " + "    redis.call('EXPIRE', KEYS[1], ARGV[1]) " + "end " + "return current";

    private final DefaultRedisScript<Long> incrScript = new DefaultRedisScript<>(INCR_WITH_EXPIRE_SCRIPT, Long.class);

    @Override
    public long incrementAndGet(String retryKey) {
        Long current = redisTemplate.execute(incrScript, Collections.singletonList(retryKey), String.valueOf(properties.getRetryCountTtlSeconds()));
        return current != null ? current : 1L;
    }

    @Override
    public void delete(String retryKey) {
        redisTemplate.delete(retryKey);
    }

    @Override
    public String buildRetryKey(String queue, String correlationId) {
        return properties.getCalculation() + ":" + queue + ":" + correlationId;
    }

    @Override
    public String buildVerifyRetryKey(String queue, String correlationId) {
        return buildRetryKey(queue, correlationId) + ":verify";
    }
}
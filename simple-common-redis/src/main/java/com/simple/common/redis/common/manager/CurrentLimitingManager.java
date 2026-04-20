package com.simple.common.redis.common.manager;

import com.simple.common.redis.annotation.CurrentLimiting;

/**
 * 限流管理器接口。
 * <p>
 * 用于实现接口级别的访问限流，支持多种限流策略。
 * 默认实现 {@link com.simple.common.redis.manager.RedissonCurrentLimitingManager} 基于
 * Redisson的RateLimiter实现分布式限流。
 * </p>
 *
 * @author qty
 */
public interface CurrentLimitingManager {

    /**
     * 执行限流检查。
     * <p>
     * 根据限流注解配置的策略和key，检查当前请求是否允许通过。
     * </p>
     *
     * @param currentLimiting 限流注解配置，包含限流规则和阈值
     * @return true-请求通过限流检查；false-请求被限流拦截
     */
    Boolean execute(CurrentLimiting currentLimiting);

}
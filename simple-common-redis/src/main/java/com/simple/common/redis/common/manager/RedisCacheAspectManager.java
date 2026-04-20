package com.simple.common.redis.common.manager;

import com.simple.common.redis.annotation.RedisCache;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Redis缓存切面管理器接口。
 * <p>
 * 用于管理 {@link RedisCache} 注解的缓存key构建和缓存策略处理。
 * 默认实现 {@link com.simple.common.redis.manager.DefaultRedisCacheAspectManager} 提供了
 * 基于方法参数的缓存key生成逻辑。
 * </p>
 *
 * @author qty
 */
public interface RedisCacheAspectManager {

    /**
     * 获取缓存的hash名称，在此处同时用作锁的key
     *
     * @param redisCache 注解对象
     */
    String getCacheKey(RedisCache redisCache, ProceedingJoinPoint joinPoint);

    /**
     * 获取缓存时间
     *
     * @param cacheTime            缓存时间
     * @param appendRandomDuration 追加随机值，防止雪崩
     */
    Integer getCacheTime(Integer cacheTime, Integer appendRandomDuration);

}
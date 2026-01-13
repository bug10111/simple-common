package com.simple.common.redis.common.manager;

import com.simple.common.redis.annotation.RedisCache;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Created by IntelliJ IDEA
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

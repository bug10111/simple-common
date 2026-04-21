package com.simple.common.auth.client.common.manager.cache;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存管理器工厂。
 *
 * @author qty
 */
public class CacheManagerFactory {

    /**
     * 创建缓存管理器。
     *
     * @param cacheType     缓存类型
     * @param redisTemplate Redis 模板（REDIS 模式必须提供，LOCAL 模式可为 null）
     * @return 缓存管理器实例
     * @throws IllegalArgumentException 当 cacheType 为 REDIS 但 redisTemplate 为 null 时抛出
     */
    public static CacheManager createCacheManager(CacheTypeEnum cacheType, StringRedisTemplate redisTemplate) {
        if (cacheType == CacheTypeEnum.LOCAL) {
            return new LocalCacheManager();
        } else {
            if (redisTemplate == null) {
                throw new IllegalArgumentException("Redis没有正确初始化！");
            }
            return new RedisCacheManager(redisTemplate);
        }
    }
}
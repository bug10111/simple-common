package com.simple.common.auth.client.common.manager.cache;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 缓存管理器工厂
 *
 * @author Admin
 */
public class CacheManagerFactory {

    /**
     * 创建缓存管理器
     *
     * @param cacheType        缓存类型
     * @param redisTemplate    Redis模板
     * @param localCacheFactory 本地缓存工厂
     * @return 缓存管理器
     */
    public static CacheManager createCacheManager(CacheTypeEnum cacheType,
                                                   StringRedisTemplate redisTemplate,
                                                   LocalCacheFactory localCacheFactory) {
        if (cacheType == CacheTypeEnum.LOCAL) {
            return new LocalCacheManager(localCacheFactory);
        } else {
            return new RedisCacheManager(redisTemplate);
        }
    }
}
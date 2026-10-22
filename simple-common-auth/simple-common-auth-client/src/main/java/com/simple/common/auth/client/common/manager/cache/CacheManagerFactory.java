package com.simple.common.auth.client.common.manager.cache;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import com.simple.common.auth.client.manager.cache.LocalCacheManager;
import com.simple.common.auth.client.manager.cache.RedisCacheManager;
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
        // 空类型不再静默回落 Redis：配置缺失必须在装配期显式失败
        if (cacheType == null) {
            throw new IllegalArgumentException("缓存类型(cacheType)不能为空，请检查 simple.auth.cache-type 等配置");
        }
        if (cacheType == CacheTypeEnum.LOCAL) {
            return new LocalCacheManager();
        }
        if (cacheType == CacheTypeEnum.REDIS) {
            if (redisTemplate == null) {
                throw new IllegalArgumentException("缓存类型为 REDIS 但 StringRedisTemplate 未装配：请引入并配置 spring-boot-starter-data-redis，或将缓存类型改为 LOCAL");
            }
            return new RedisCacheManager(redisTemplate);
        }
        throw new IllegalArgumentException("不支持的缓存类型: " + cacheType);
    }
}
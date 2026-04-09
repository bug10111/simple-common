package com.simple.common.auth.server.common.config;

import com.simple.common.auth.server.common.enums.CacheTypeEnum;
import com.simple.common.auth.server.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.server.common.manager.cache.LocalCacheManager;
import com.simple.common.auth.server.common.manager.cache.RedisCacheManager;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@ComponentScan(basePackages = "com.simple.common.auth.server")
@Configuration
@EnableConfigurationProperties
public class ServerAuthConfig {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private LocalCacheFactory localCacheFactory;

    /**
     * 创建缓存管理器
     * 默认使用Redis缓存，如果Redis不可用则使用本地缓存
     *
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CacheTypeEnum cacheType = determineCacheType();
        return CacheManagerFactory.createCacheManager(cacheType, redisTemplate, localCacheFactory);
    }

    /**
     * 确定缓存类型
     * 优先使用Redis，如果Redis不可用则使用本地缓存
     *
     * @return 缓存类型
     */
    private CacheTypeEnum determineCacheType() {
        // 如果Redis模板可用，优先使用Redis
        if (redisTemplate != null) {
            return CacheTypeEnum.REDIS;
        }
        // 否则使用本地缓存
        return CacheTypeEnum.LOCAL;
    }
}

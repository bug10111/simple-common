package com.simple.common.auth.server.common.config;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.server.common.properties.AuthServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 服务端认证模块配置。
 * <p>
 * 通过 AuthServerProperties 统一管理缓存类型，支持按模块独立配置。
 *
 * @author qty (优化版本)
 */
@ComponentScan(basePackages = "com.simple.common.auth.server")
@Configuration
public class ServerAuthConfig {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AuthServerProperties authServerProperties;

    /**
     * 创建认证缓存管理器（用于用户信息、权限、客户端等）。
     *
     * @return 认证专用 CacheManager
     */
    @Bean
    @Qualifier("authCacheManager")
    public CacheManager authCacheManager() {
        CacheTypeEnum cacheType = authServerProperties.getCacheType();
        return CacheManagerFactory.createCacheManager(cacheType, redisTemplate);
    }

    /**
     * 创建签名缓存管理器（用于防重放 nonce 等，服务端可能用到）。
     *
     * @return 签名专用 CacheManager
     */
    @Bean
    @Qualifier("signCacheManager")
    public CacheManager signCacheManager() {
        CacheTypeEnum cacheType = authServerProperties.getCacheType();
        return CacheManagerFactory.createCacheManager(cacheType, redisTemplate);
    }
}
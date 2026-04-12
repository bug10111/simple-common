package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 认证模块缓存配置类。
 * <p>
 * 为不同类型缓存创建独立的 CacheManager Bean，并通过 @Qualifier 区分，
 * 避免因类型相同导致的注入歧义。
 *
 * @author qty (修复版本)
 */
@Configuration
@ComponentScan(basePackages = "com.simple.common.auth.client")
public class ClientAuthConfig {

    /**
     * 创建认证缓存管理器（用于用户信息、权限等）。
     *
     * @param authProperties    认证配置
     * @param redisTemplate     Redis 模板
     * @return 认证专用 CacheManager
     */
    @Bean
    @Qualifier("authCacheManager")
    public CacheManager authCacheManager(AuthProperties authProperties, StringRedisTemplate redisTemplate) {
        return CacheManagerFactory.createCacheManager(authProperties.getCacheType(), redisTemplate, LocalCacheFactory.getInstance());
    }

    /**
     * 创建签名缓存管理器（用于防重放 nonce 等）。
     *
     * @param signProperties    签名配置
     * @param redisTemplate     Redis 模板
     * @return 签名专用 CacheManager
     */
    @Bean
    @Qualifier("signCacheManager")
    public CacheManager signCacheManager(SignProperties signProperties, StringRedisTemplate redisTemplate) {
        return CacheManagerFactory.createCacheManager(signProperties.getCacheType(), redisTemplate, LocalCacheFactory.getInstance());
    }

    /**
     * 创建 CSRF 缓存管理器（用于存储 CSRF Token）。
     *
     * @param csrfProperties    CSRF 配置
     * @param redisTemplate     Redis 模板
     * @return CSRF 专用 CacheManager
     */
    @Bean
    @Qualifier("csrfCacheManager")
    public CacheManager csrfCacheManager(CsrfProperties csrfProperties, StringRedisTemplate redisTemplate) {
        return CacheManagerFactory.createCacheManager(csrfProperties.getCacheType(), redisTemplate, LocalCacheFactory.getInstance());
    }
}
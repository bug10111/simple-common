package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.properties.SignProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 认证模块缓存配置类。
 * <p>
 * 为不同类型缓存创建独立的 CacheManager Bean，并通过 @Qualifier 区分，
 * 避免因类型相同导致的注入歧义。
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = "com.simple.common.auth.client")
public class ClientAuthConfig {

    /**
     * Redis 模板，非必需（仅当使用 REDIS 缓存类型时需要）
     */
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 创建认证缓存管理器（用于用户信息、权限等）。
     * <p>
     * 标记 @Primary：使用方按类型直接注入 CacheManager 时默认获得认证缓存；
     * 需要其他实例时用 @Qualifier("signCacheManager")/@Qualifier("csrfCacheManager") 区分。
     * </p>
     *
     * @param authProperties 认证配置
     * @return 认证专用 CacheManager
     */
    @Primary
    @Bean
    @Qualifier("authCacheManager")
    public CacheManager authCacheManager(AuthProperties authProperties) {
        return CacheManagerFactory.createCacheManager(authProperties.getCacheType(), redisTemplate);
    }

    /**
     * 创建签名缓存管理器（用于防重放 nonce 等）。
     * <p>
     * 已接入 {@link com.simple.common.auth.client.manager.sign.DefaultSignManager}，
     * simple.sign.cache-type 由此生效；REDIS 类型要求已装配 StringRedisTemplate。
     * </p>
     *
     * @param signProperties 签名配置
     * @return 签名专用 CacheManager
     */
    @Bean
    @Qualifier("signCacheManager")
    public CacheManager signCacheManager(SignProperties signProperties) {
        return CacheManagerFactory.createCacheManager(signProperties.getCacheType(), redisTemplate);
    }

    /**
     * 创建 CSRF 缓存管理器（用于存储 CSRF Token）。
     *
     * @param csrfProperties CSRF 配置
     * @return CSRF 专用 CacheManager
     */
    @Bean
    @Qualifier("csrfCacheManager")
    public CacheManager csrfCacheManager(CsrfProperties csrfProperties) {
        return CacheManagerFactory.createCacheManager(csrfProperties.getCacheType(), redisTemplate);
    }
}
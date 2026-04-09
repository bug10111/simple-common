package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.properties.SignProperties;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Created with IntelliJ IDEA
 * <p>
 * auth配置类
 *
 * @author qty
 */
public class ClientAuthConfig {

    /**
     * 创建认证缓存管理器
     */
    @Bean
    public CacheManager authCacheManager(AuthProperties authProperties,
                                         StringRedisTemplate redisTemplate,
                                         LocalCacheFactory localCacheFactory) {
        return CacheManagerFactory.createCacheManager(
                authProperties.getCacheType(),
                redisTemplate,
                localCacheFactory
        );
    }

    /**
     * 创建签名缓存管理器
     */
    @Bean
    public CacheManager signCacheManager(SignProperties signProperties,
                                         StringRedisTemplate redisTemplate,
                                         LocalCacheFactory localCacheFactory) {
        return CacheManagerFactory.createCacheManager(
                signProperties.getCacheType(),
                redisTemplate,
                localCacheFactory
        );
    }

    /**
     * 创建CSRF缓存管理器
     */
    @Bean
    public CacheManager csrfCacheManager(CsrfProperties csrfProperties,
                                         StringRedisTemplate redisTemplate,
                                         LocalCacheFactory localCacheFactory) {
        return CacheManagerFactory.createCacheManager(
                csrfProperties.getCacheType(),
                redisTemplate,
                localCacheFactory
        );
    }
}
}
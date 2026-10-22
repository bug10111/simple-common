package com.simple.common.auth.server.common.config;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.server.common.properties.AuthServerProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 服务端认证模块配置。
 * <p>
 * 通过 {@link AuthServerProperties#getCacheType()} 统一管理缓存类型——认证缓存与签名缓存
 * 共用同一 cacheType 配置（AuthServerProperties 仅提供单一 cacheType 项，如需按模块独立
 * 配置需先在该属性类中扩展对应配置项）；缓存后端为 REDIS 时依赖容器内
 * {@link StringRedisTemplate}（由 simple-common-redis 自动装配提供）。
 * </p>
 * <p>
 * 本模块与 auth-client 的自动配置同容器加载且 Bean 名一致（authCacheManager/signCacheManager），
 * 服务端 Bean 以 {@code @ConditionalOnMissingBean} 声明：客户端已注册同名 Bean 时服务端定义
 * 让位，避免 allow-bean-definition-overriding 未开启时的 BeanDefinitionOverrideException。
 * </p>
 *
 * @author qty
 */
@ComponentScan(basePackages = "com.simple.common.auth.server")
@Configuration
public class ServerAuthConfig {

    @Autowired
    private AuthServerProperties authServerProperties;

    /**
     * 创建认证缓存管理器（用于用户信息、权限、客户端等）。
     *
     * @param redisTemplateProvider Redis 模板提供者（可缺省，由 createCacheManager 按需校验）
     * @return 认证专用 CacheManager
     */
    @Bean
    @ConditionalOnMissingBean(name = "authCacheManager")
    @Qualifier("authCacheManager")
    public CacheManager authCacheManager(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        CacheTypeEnum cacheType = authServerProperties.getCacheType();
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        // REDIS 后端必须在装配期校验模板存在性，把“CacheManagerFactory 深处 IllegalArgumentException”
        // 提前为可诊断的启动期错误
        if (cacheType == CacheTypeEnum.REDIS && redisTemplate == null) {
            throw new IllegalStateException("authServerProperties.cache-type=REDIS 但容器中不存在"
                    + " StringRedisTemplate Bean：请确认已引入 simple-common-redis 依赖或改配 LOCAL 缓存类型");
        }
        return CacheManagerFactory.createCacheManager(cacheType, redisTemplate);
    }

    /**
     * 创建签名缓存管理器（用于防重放 nonce 等，服务端可能用到）。
     *
     * @param redisTemplateProvider Redis 模板提供者（可缺省，由 createCacheManager 按需校验）
     * @return 签名专用 CacheManager
     */
    @Bean
    @ConditionalOnMissingBean(name = "signCacheManager")
    @Qualifier("signCacheManager")
    public CacheManager signCacheManager(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        CacheTypeEnum cacheType = authServerProperties.getCacheType();
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (cacheType == CacheTypeEnum.REDIS && redisTemplate == null) {
            throw new IllegalStateException("authServerProperties.cache-type=REDIS 但容器中不存在"
                    + " StringRedisTemplate Bean：请确认已引入 simple-common-redis 依赖或改配 LOCAL 缓存类型");
        }
        return CacheManagerFactory.createCacheManager(cacheType, redisTemplate);
    }
}

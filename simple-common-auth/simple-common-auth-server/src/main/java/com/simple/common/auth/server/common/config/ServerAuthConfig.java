package com.simple.common.auth.server.common.config;

import com.simple.common.auth.server.common.manager.cache.CacheManagerFactory;
import com.simple.common.auth.server.common.properties.AuthServerProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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


}
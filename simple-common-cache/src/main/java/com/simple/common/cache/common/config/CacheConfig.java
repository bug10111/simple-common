package com.simple.common.cache.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * 缓存模块自动配置。
 * <p>
 * 本模块当前未声明任何需要注册的 Spring Bean（工具类为静态门面、缓存工厂为静态单例），
 * 保留该自动配置类作为模块的装配入口，供后续按需注册 Bean；
 * 不再使用包级 {@code @ComponentScan}，避免把本模块未来新增的组件无条件注册进使用方容器，
 * 同时满足 Spring Boot 对 AutoConfiguration.imports 清单类的规范要求。
 *
 * @author qty
 */
@AutoConfiguration
public class CacheConfig {

}

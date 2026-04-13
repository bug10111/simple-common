package com.simple.common.auth.server.common.properties;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 服务端认证配置属性。
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth.server")
public class AuthServerProperties {

    /**
     * 缓存类型，默认使用 Redis。
     */
    private CacheTypeEnum cacheType = CacheTypeEnum.REDIS;

}
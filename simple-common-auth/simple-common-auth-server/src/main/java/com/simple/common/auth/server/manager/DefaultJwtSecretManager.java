package com.simple.common.auth.server.manager;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.manager.secret.JwtSecretManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 默认 JWT 密钥管理器。
 *
 * @author Admin (修复版本)
 */
@Component
public class DefaultJwtSecretManager implements JwtSecretManager {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    private static final String SECRET_KEY = "auth:jwt:secret:current";

    private static final String SECRET_PREFIX = "auth:jwt:secret:";

    @Override
    public void addSecret(String secret) {
        cacheManager.set(SECRET_KEY, secret);
        cacheManager.set(SECRET_PREFIX + secret, "1");
    }

    @Override
    public void updateSecret(String oldSecret, String newSecret) {
        cacheManager.delete(SECRET_PREFIX + oldSecret);
        cacheManager.set(SECRET_KEY, newSecret);
        cacheManager.set(SECRET_PREFIX + newSecret, "1");
    }

    @Override
    public String getCurrentSecret() {
        return cacheManager.get(SECRET_KEY);
    }

    @Override
    public boolean existsSecret(String secret) {
        return cacheManager.hasKey(SECRET_PREFIX + secret);
    }
}
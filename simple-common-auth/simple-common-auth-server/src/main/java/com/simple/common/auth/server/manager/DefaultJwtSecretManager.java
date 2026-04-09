package com.simple.common.auth.server.manager;

import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.manager.secret.JwtSecretManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 默认JWT密钥管理器
 *
 * @author Admin
 */
@Component
public class DefaultJwtSecretManager implements JwtSecretManager {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void saveSecret(String userId, TokenData tokenData) {
        String key = SECRET_KEY_PREFIX + userId;
        cacheManager.set(key, tokenData.getSecret(), tokenData.getExpire());
    }

    @Override
    public String getSecret(String userId) {
        String key = SECRET_KEY_PREFIX + userId;
        return cacheManager.get(key);
    }

    @Override
    public void deleteSecret(String userId) {
        String key = SECRET_KEY_PREFIX + userId;
        cacheManager.delete(key);
    }
}
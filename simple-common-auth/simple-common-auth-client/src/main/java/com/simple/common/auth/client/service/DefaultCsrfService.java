package com.simple.common.auth.client.service;

import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.service.CsrfService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 默认CSRF服务实现
 *
 * @author qty
 */
@Service
public class DefaultCsrfService implements CsrfService {

    private final CacheManager cacheManager;
    private final CsrfProperties csrfProperties;

    public DefaultCsrfService(CacheManager csrfCacheManager, CsrfProperties csrfProperties) {
        this.cacheManager = csrfCacheManager;
        this.csrfProperties = csrfProperties;
    }

    @Override
    public String createToken(String userId, String path) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = csrfProperties.getCsrfKey() + userId + ":" + path;
        cacheManager.set(key, token, csrfProperties.getCacheTime());
        return token;
    }

    @Override
    public void checkToken(String userId, String path, String token) {
        String key = csrfProperties.getCsrfKey() + userId + ":" + path;
        String cachedToken = cacheManager.get(key);
        if (cachedToken == null || !cachedToken.equals(token)) {
            throw new RuntimeException("CSRF token验证失败");
        }
        // 验证通过后删除token
        cacheManager.delete(key);
    }
}
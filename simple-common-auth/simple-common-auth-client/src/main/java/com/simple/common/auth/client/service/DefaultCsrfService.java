package com.simple.common.auth.client.service;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.core.utils.AssertUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 默认 CSRF 服务实现。
 * <p>
 * 修复：通过 @Qualifier 注入 csrfCacheManager，避免歧义。
 *
 * @author qty (修复版本)
 */
@Service
public class DefaultCsrfService implements CsrfService {

    private final CacheManager cacheManager;
    private final CsrfProperties csrfProperties;

    public DefaultCsrfService(@Qualifier("csrfCacheManager") CacheManager cacheManager,
                              CsrfProperties csrfProperties) {
        this.cacheManager = cacheManager;
        this.csrfProperties = csrfProperties;
    }

    @Override
    public void saveToken(String userId, String path, String token) {
        String key = csrfProperties.getKey(path, userId);
        cacheManager.set(key, token, csrfProperties.getCacheTime());
    }

    @Override
    public String getToken(String userId, String path) {
        String key = csrfProperties.getKey(path, userId);
        return cacheManager.get(key);
    }

    @Override
    public void removeToken(String userId, String path) {
        String key = csrfProperties.getKey(path, userId);
        cacheManager.delete(key);
    }

    @Override
    public void checkToken(String userId, String path, String token) {
        String key = csrfProperties.getKey(path, userId);
        String cachedToken = cacheManager.get(key);
        AssertUtils.isTrue(cachedToken != null && cachedToken.equals(token),
                           "非法操作", "用户[{}]==>[{}] CSRF token 校验失败", userId, path);
        // 验证通过后删除 token，防止重复使用
        cacheManager.delete(key);
    }
}
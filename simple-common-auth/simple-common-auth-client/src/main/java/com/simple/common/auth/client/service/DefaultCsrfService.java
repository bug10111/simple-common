package com.simple.common.auth.client.service;

import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.service.CsrfService;
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

    /**
     * 构造器注入，指定 CSRF 专用的缓存管理器。
     *
     * @param cacheManager   CSRF 缓存管理器
     * @param csrfProperties CSRF 配置
     */
    public DefaultCsrfService(@Qualifier("csrfCacheManager") CacheManager cacheManager,
                              CsrfProperties csrfProperties) {
        this.cacheManager = cacheManager;
        this.csrfProperties = csrfProperties;
    }

    /**
     * 保存 CSRF Token。
     *
     * @param userId 用户ID
     * @param path   请求路径
     * @param token  CSRF Token
     */
    @Override
    public void saveToken(String userId, String path, String token) {
        String key = csrfProperties.getKey(path, userId);
        cacheManager.set(key, token, csrfProperties.getCacheTime());
    }

    /**
     * 获取 CSRF Token。
     *
     * @param userId 用户ID
     * @param path   请求路径
     * @return 存储的 Token，若不存在返回 null
     */
    @Override
    public String getToken(String userId, String path) {
        String key = csrfProperties.getKey(path, userId);
        return cacheManager.get(key);
    }

    /**
     * 删除 CSRF Token。
     *
     * @param userId 用户ID
     * @param path   请求路径
     */
    @Override
    public void removeToken(String userId, String path) {
        String key = csrfProperties.getKey(path, userId);
        cacheManager.delete(key);
    }

    /**
     * 校验 CSRF Token 是否正确，校验通过后删除 Token（一次性使用）。
     *
     * @param userId 用户ID
     * @param path   请求路径
     * @param token  待校验的 Token
     */
    @Override
    public void checkToken(String userId, String path, String token) {
        String key = csrfProperties.getKey(path, userId);
        String cachedToken = cacheManager.get(key);
        if (cachedToken == null || !cachedToken.equals(token)) {
            throw new RuntimeException("CSRF token验证失败");
        }
        // 验证通过后删除 token，防止重复使用
        cacheManager.delete(key);
    }
}
package com.simple.common.auth.client.service;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.CsrfProperties;
import com.simple.common.auth.client.common.service.CsrfService;
import com.simple.common.core.utils.AssertUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 默认 CSRF 服务实现。
 * <p>
 * 校验语义：null/空白 token 直接拒绝（fail-closed），不得因比较对象顺序差异产生 NPE 或静默通过；
 * consume=true 时走 getAndDelete 原子消费（Redis GETDEL / Local 原子移除），
 * 并发重复请求只有一个通过——一次性承诺真实成立；比较使用恒定时间算法防时序侧信道。
 * </p>
 *
 * @author qty
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
    public void checkToken(String userId, String path, String token, boolean consume) {
        String key = csrfProperties.getKey(path, userId);

        // 原子消费：getAndDelete 保证并发重复请求只有一个读到有效 token（check-then-delete 竞态已消除）
        String cachedToken = consume ? cacheManager.getAndDelete(key) : cacheManager.get(key);

        // 恒定时间比较（MessageDigest.isEqual）：逐字符提前返回会泄漏长度/前缀信息
        boolean matched = isNotEmpty(token) && isNotEmpty(cachedToken)
                && MessageDigest.isEqual(
                        cachedToken.getBytes(StandardCharsets.UTF_8),
                        token.getBytes(StandardCharsets.UTF_8));
        AssertUtils.isTrue(matched, "用户[{}]==>[{}] CSRF token 校验失败", userId, path);
    }

    private static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}

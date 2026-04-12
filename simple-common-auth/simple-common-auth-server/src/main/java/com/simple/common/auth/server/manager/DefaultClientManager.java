package com.simple.common.auth.server.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.manager.client.ClientManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 默认客户端管理器，增加本地缓存优化。
 *
 * @author Admin (优化版本)
 */
@Component
public class DefaultClientManager implements ClientManager {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    private static final String CLIENT_KEY_PREFIX = "auth:client:";

    /**
     * 本地客户端缓存（clientId -> clientSecret）
     */
    private final Cache<String, String> clientSecretCache = Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(30, TimeUnit.MINUTES).build();

    /**
     * 保存客户端信息（同时写入 Redis 和本地缓存）。
     *
     * @param clientId      客户端ID
     * @param clientDetails 客户端详情
     */
    public void saveClient(String clientId, ClientDetails clientDetails) {
        String key = CLIENT_KEY_PREFIX + clientId;
        cacheManager.set(key, clientDetails.getClientSecret(), 1800);
        clientSecretCache.put(clientId, clientDetails.getClientSecret());
    }

    /**
     * 获取客户端密钥，优先本地缓存。
     *
     * @param clientId 客户端ID
     * @return 客户端密钥，不存在返回 null
     */
    public String getClientSecret(String clientId) {
        String secret = clientSecretCache.getIfPresent(clientId);
        if (secret != null) return secret;
        secret = cacheManager.get(CLIENT_KEY_PREFIX + clientId);
        if (secret != null) clientSecretCache.put(clientId, secret);
        return secret;
    }

    /**
     * 删除客户端信息。
     *
     * @param clientId 客户端ID
     */
    public void deleteClient(String clientId) {
        cacheManager.delete(CLIENT_KEY_PREFIX + clientId);
        clientSecretCache.invalidate(clientId);
    }

    /**
     * 加密生成客户端认证头（Basic Auth）。
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return Basic 认证字符串
     */
    @Override
    public String encrypt(String clientId, String clientSecret) {
        String combined = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(combined.getBytes());
    }

    /**
     * 解密 Basic Auth 头，提取客户端ID和密钥。
     *
     * @param header Authorization 头完整字符串
     * @return Map 包含 ClientId 和 ClientSecret
     */
    @Override
    public Map<com.simple.common.auth.server.common.enums.ClientAttribute, String> decryptStr(String header) {
        if (header == null || !header.startsWith("Basic ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String base64 = header.substring(6);
        byte[] decoded = Base64.getDecoder().decode(base64);
        String[] parts = new String(decoded).split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Basic auth format");
        }
        return Map.of(com.simple.common.auth.server.common.enums.ClientAttribute.ClientId, parts[0], com.simple.common.auth.server.common.enums.ClientAttribute.ClientSecret, parts[1]);
    }
}
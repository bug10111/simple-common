package com.simple.common.auth.server.manager;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.ClientAttribute;
import com.simple.common.auth.server.common.manager.client.ClientManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 默认客户端管理器。
 * <p>
 * 客户端密钥仅以 Redis（authCacheManager）为缓存层，不维护 JVM 本地副本：
 * 密钥撤销/轮换删除 Redis 键后，同集群所有授权中心实例立即生效，
 * 避免多实例部署下本地缓存继续放行已撤销密钥（本地缓存曾达 30 分钟滞后窗口）。
 * </p>
 *
 * @author qty
 */
@Component
public class DefaultClientManager implements ClientManager {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    private static final String CLIENT_KEY_PREFIX = "auth:client:";

    /**
     * 客户端密钥缓存 TTL（秒）；Redis 缓存与本管理器的撤销语义共用同一契约值
     */
    private static final long CLIENT_SECRET_TTL_SECONDS = 1800;

    /**
     * 保存客户端信息（写入 Redis，TTL 见 {@link #CLIENT_SECRET_TTL_SECONDS}）。
     *
     * @param clientId      客户端ID
     * @param clientDetails 客户端详情
     */
    public void saveClient(String clientId, ClientDetails clientDetails) {
        String key = CLIENT_KEY_PREFIX + clientId;
        cacheManager.set(key, clientDetails.getClientSecret(), CLIENT_SECRET_TTL_SECONDS);
    }

    /**
     * 获取客户端密钥（经 Redis 缓存）。
     *
     * @param clientId 客户端ID
     * @return 客户端密钥，不存在返回 null
     */
    public String getClientSecret(String clientId) {
        return cacheManager.get(CLIENT_KEY_PREFIX + clientId);
    }

    /**
     * 删除客户端信息（删除 Redis 键即全实例生效，本地无副本）。
     *
     * @param clientId 客户端ID
     */
    public void deleteClient(String clientId) {
        cacheManager.delete(CLIENT_KEY_PREFIX + clientId);
    }

    /**
     * 编码客户端认证凭证（Basic Auth 的 Base64 载荷）。
     * <p>
     * 注意：仅是编码而非加密——Base64 任何人可逆，机密性依赖传输层 TLS/HTTPS；
     * 返回值不含 {@code Basic } 前缀，使用前需自行拼接。
     * </p>
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return Base64(clientId:clientSecret)，不含 "Basic " 前缀
     */
    @Override
    public String encrypt(String clientId, String clientSecret) {
        String combined = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(combined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解码 Basic Auth 头，提取客户端ID和密钥。
     * <p>
     * 契约（RFC 7617）：scheme 名大小写不敏感（{@code basic x} 合法）；
     * 返回不可变 Map，恒含 {@link ClientAttribute#ClientId} 与 {@link ClientAttribute#ClientSecret} 两个键。
     * </p>
     *
     * @param header Authorization 头完整字符串
     * @return 不可变 Map，包含 ClientId 与 ClientSecret
     * @throws IllegalArgumentException header 为 null、缺少 Basic 前缀、Base64 非法或缺少 ":" 分隔符时抛出
     */
    @Override
    public Map<ClientAttribute, String> decryptStr(String header) {
        if (header == null || header.length() <= 6 || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        String base64 = header.substring(6).trim();
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Basic auth base64", e);
        }
        String[] parts = new String(decoded, StandardCharsets.UTF_8).split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Basic auth format");
        }
        return Map.of(ClientAttribute.ClientId, parts[0], ClientAttribute.ClientSecret, parts[1]);
    }
}

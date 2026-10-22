package com.simple.common.auth.server.manager.secret;

import com.simple.common.auth.server.common.manager.secret.UnifiedSecretManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认统一密钥管理器实现（服务端）。
 * <p>
 * 使用 CSPRNG（{@link SecureRandom}）随机生成 JWT 和 SIGN 签名密钥。
 * 采用内存缓存存储各项目的密钥，支持多项目隔离。
 * </p>
 *
 * <h3>特点：</h3>
 * <ul>
 *   <li>每个项目独立密钥，互不影响</li>
 *   <li>首次访问时自动生成并缓存密钥（computeIfAbsent 原子生成，并发首访不产生双份密钥）</li>
 *   <li>密钥强度符合安全要求（JWT: 64字符/384位熵，SIGN: 32字符/192位熵，SecureRandom + Base64URL）</li>
 *   <li>返回的密钥 Map 不可变，调用方无法改写缓存中的密钥</li>
 *   <li>适用于开发环境和快速部署场景</li>
 * </ul>
 *
 * <h3>生产环境建议：</h3>
 * <p>
 * 如果需要在多实例间共享密钥或从数据库加载密钥，请自定义实现 {@link UnifiedSecretManager} 接口，
 * 并配置 {@code simple.auth.default-secret.enabled=false} 关闭本默认实现，避免双 Bean 冲突。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "simple.auth.default-secret", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultUnifiedSecretManager implements UnifiedSecretManager {

    /**
     * 项目密钥缓存：key=projectCode, value={jwt=xxx, sign=xxx}（值 Map 不可变）
     */
    private final Map<String, Map<String, String>> projectSecretCache = new ConcurrentHashMap<>();

    /**
     * 密钥熵源（CSPRNG）；RandomUtil/RandomStringUtils 基于 ThreadLocalRandom/Random，非加密安全，禁止用于密钥
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * JWT 密钥字节数（48 字节 → Base64URL 编码 64 字符）
     */
    private static final int JWT_SECRET_BYTES = 48;

    /**
     * SIGN 密钥字节数（24 字节 → Base64URL 编码 32 字符）
     */
    private static final int SIGN_SECRET_BYTES = 24;

    /**
     * 根据项目编码获取密钥
     * <p>
     * 项目不存在时自动生成默认密钥并缓存（原子 computeIfAbsent：并发首访仅生成一份密钥，
     * 杜绝“双请求各生成一套、败者返回的密钥随即失效”的竞态）。
     * </p>
     *
     * @param projectCode 项目编码（spring.application.name），不能为 null 或空白
     * @return 不可变 Map，恒含 {@link #KEY_JWT} 与 {@link #KEY_SIGN} 两个非空 key
     * @throws IllegalArgumentException projectCode 为 null 或空白时抛出（杜绝空/同值 projectCode
     *                                  塌缩到共享键导致的跨项目密钥串用）
     */
    @Override
    public Map<String, String> getSecrets(String projectCode) {
        if (projectCode == null || projectCode.isBlank()) {
            throw new IllegalArgumentException("获取密钥时 projectCode 不能为空");
        }
        // computeIfAbsent 原子生成，返回不可变快照防止调用方篡改缓存中的密钥
        return projectSecretCache.computeIfAbsent(projectCode, code -> {
            Map<String, String> generated = new HashMap<>();
            generated.put(KEY_JWT, generateJwtSecret());
            generated.put(KEY_SIGN, generateSignSecret());
            log.info("为项目 [{}] 生成新密钥", code);
            return Collections.unmodifiableMap(generated);
        });
    }

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 生成 64 字符随机密钥（SecureRandom 熵源 + Base64URL 编码）。
     * </p>
     *
     * @return 64位随机密钥字符串
     */
    @Override
    public String generateJwtSecret() {
        return generateSecret(JWT_SECRET_BYTES);
    }

    /**
     * 生成新的API签名密钥
     * <p>
     * 生成 32 字符随机密钥（SecureRandom 熵源 + Base64URL 编码）。
     * </p>
     *
     * @return 32位随机密钥字符串
     */
    @Override
    public String generateSignSecret() {
        return generateSecret(SIGN_SECRET_BYTES);
    }

    /**
     * 以 CSPRNG 生成指定字节数的 Base64URL 随机密钥
     *
     * @param bytes 密钥字节数
     * @return Base64URL（无填充）随机密钥
     */
    private static String generateSecret(int bytes) {
        byte[] raw = new byte[bytes];
        SECURE_RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}

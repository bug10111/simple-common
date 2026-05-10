package com.simple.common.auth.server.manager.secret;

import cn.hutool.core.util.RandomUtil;
import com.simple.common.auth.server.common.manager.secret.UnifiedSecretManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认统一密钥管理器实现（服务端）
 * <p>
 * 使用随机生成的方式产生JWT和SIGN签名密钥。
 * 采用内存缓存存储各项目的密钥，支持多项目隔离。
 * </p>
 *
 * <h3>特点：</h3>
 * <ul>
 *   <li>每个项目独立密钥，互不影响</li>
 *   <li>首次访问时自动生成并缓存密钥</li>
 *   <li>密钥强度符合安全要求（JWT: 64位, SIGN: 32位）</li>
 *   <li>适用于开发环境和快速部署场景</li>
 * </ul>
 *
 * <h3>生产环境建议：</h3>
 * <p>
 * 如果需要在多实例间共享密钥或从数据库加载密钥，
 * 建议自定义实现 {@link UnifiedSecretManager} 接口。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultUnifiedSecretManager implements UnifiedSecretManager {

    /**
     * 项目密钥缓存：key=projectCode, value={jwt=xxx, sign=xxx}
     */
    private final Map<String, Map<String, String>> projectSecretCache = new ConcurrentHashMap<>();

    /**
     * 根据项目编码获取密钥
     * <p>
     * 如果项目不存在，自动生成默认密钥并缓存。
     * </p>
     *
     * @param projectCode 项目编码（spring.application.name）
     * @return 包含"jwt"和"sign"两个key的Map
     */
    @Override
    public Map<String, String> getSecrets(String projectCode) {
        // 从缓存中获取
        Map<String, String> secrets = projectSecretCache.get(projectCode);
        
        if (secrets == null) {
            // 生成新密钥
            secrets = new HashMap<>();
            secrets.put("jwt", generateJwtSecret());
            secrets.put("sign", generateSignSecret());
            
            // 存入缓存
            projectSecretCache.put(projectCode, secrets);
            
            log.info("为项目 [{}] 生成新密钥", projectCode);
        }
        
        return secrets;
    }

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 生成64位随机字符串作为JWT密钥。
     * </p>
     *
     * @return 64位随机密钥字符串
     */
    @Override
    public String generateJwtSecret() {
        return RandomUtil.randomString(64);
    }

    /**
     * 生成新的API签名密钥
     * <p>
     * 生成32位随机字符串作为SIGN密钥。
     * </p>
     *
     * @return 32位随机密钥字符串
     */
    @Override
    public String generateSignSecret() {
        return RandomUtil.randomString(32);
    }
}

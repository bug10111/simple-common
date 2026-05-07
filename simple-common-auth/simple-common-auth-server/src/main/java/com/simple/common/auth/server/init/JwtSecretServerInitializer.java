package com.simple.common.auth.server.init;

import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.server.common.manager.secret.SecretKeyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * JWT密钥初始化器（服务端）
 * <p>
 * 应用启动时通过SecretKeyManager生成JWT密钥并缓存，然后发布事件通知所有客户端。
 * 仅在服务端模式下执行。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class JwtSecretServerInitializer implements ApplicationRunner {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private SecretKeyManager secretKeyManager;

    /**
     * 应用启动后执行
     *
     * @param args 应用参数
     */
    @Override
    public void run(ApplicationArguments args) {

        try {
            String newSecret = secretKeyManager.generate();
            tokenManager.addSecret(newSecret);
            log.info("JWT密钥初始化成功: {}", maskSecret(newSecret));
        } catch (Exception e) {
            log.error("JWT密钥初始化失败", e);
        }
    }

    /**
     * 脱敏显示密钥
     *
     * @param secret 原始密钥
     * @return 脱敏后的密钥
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }
}

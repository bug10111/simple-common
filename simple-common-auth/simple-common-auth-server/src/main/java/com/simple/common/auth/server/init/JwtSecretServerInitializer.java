package com.simple.common.auth.server.init;

import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.server.common.manager.secret.UnifiedSecretManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 密钥初始化器（服务端）
 * <p>
 * 应用启动时通过UnifiedSecretManager为默认项目生成JWT和SIGN密钥并缓存，
 * 然后发布事件通知所有客户端。
 * 仅在服务端模式下执行。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class JwtSecretServerInitializer implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private SignManager signManager;

    @Autowired
    private UnifiedSecretManager unifiedSecretManager;

    /**
     * 应用完全就绪后执行（确保事件处理器已注册）
     *
     * @param event 应用就绪事件
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        try {
            // 为默认项目生成双密钥
            Map<String, String> secrets = unifiedSecretManager.getSecrets("default");
            
            String jwtSecret = secrets.get("jwt");
            String signSecret = secrets.get("sign");
            
            // 加载JWT密钥（不广播）
            tokenManager.addSecret(jwtSecret, false);
            log.info("JWT密钥初始化成功: {}", maskSecret(jwtSecret));
            
            // 加载SIGN密钥（不广播）
            signManager.addSecret(signSecret, false);
            log.info("SIGN密钥初始化成功: {}", maskSecret(signSecret));
            
        } catch (Exception e) {
            log.error("密钥初始化失败", e);
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

    @Override
    public int getOrder() {
        // 在 EventHandlerInit 之后执行，确保事件处理器已注册
        return Ordered.LOWEST_PRECEDENCE;
    }
}

package com.simple.common.auth.server.init;

import cn.hutool.core.util.StrUtil;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.server.common.manager.secret.UnifiedSecretManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 密钥初始化器（服务端）。
 * <p>
 * 应用启动时通过 {@link UnifiedSecretManager} 为默认项目生成JWT和SIGN密钥并写入本地缓存
 * （仅本地缓存，不涉及远端广播；客户端密钥由各客户端自举或密钥轮换投递获得）。
 * 初始化失败（密钥缺失/加载异常）时直接中止启动——密钥缺失的认证服务无法完成任何
 * 令牌签名/验签，带病启动只会让故障以更隐蔽的方式暴露。
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

    @Autowired
    private AuthProperties authProperties;

    /**
     * 应用完全就绪后执行（确保事件处理器已注册）
     *
     * @param event 应用就绪事件
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 项目编码取自配置（simple.auth.project-code，AuthProperties 装配期已强校验非空），
        // 与客户端约定同一来源，禁止硬编码字面量——否则真实项目的密钥永远不会被预热
        String projectCode = authProperties.getProjectCode();

        try {
            // 为默认项目生成双密钥
            Map<String, String> secrets = unifiedSecretManager.getSecrets(projectCode);
            if (secrets == null) {
                throw new IllegalStateException("密钥初始化失败: UnifiedSecretManager 返回为空");
            }

            String jwtSecret = secrets.get(UnifiedSecretManager.KEY_JWT);
            String signSecret = secrets.get(UnifiedSecretManager.KEY_SIGN);
            if (StrUtil.isEmpty(jwtSecret) || StrUtil.isEmpty(signSecret)) {
                throw new IllegalStateException("密钥初始化失败: jwt/sign 密钥缺失");
            }

            // 加载JWT密钥（仅本地缓存）
            tokenManager.addSecret(jwtSecret);
            log.info("JWT密钥初始化成功: {}", maskSecret(jwtSecret));

            // 加载SIGN密钥（仅本地缓存）
            signManager.addSecret(signSecret);
            log.info("SIGN密钥初始化成功: {}", maskSecret(signSecret));

        } catch (IllegalStateException e) {
            // 快速失败：密钥缺失/密钥源不可用属致命错误，带病启动只会让所有验签静默失败
            throw e;
        } catch (Exception e) {
            log.error("密钥初始化失败", e);
            throw new IllegalStateException("密钥初始化失败，认证服务无法正常工作", e);
        }
    }

    /**
     * 脱敏显示密钥（仅暴露首尾各2字符的指纹，密钥材料不落日志）
     *
     * @param secret 原始密钥
     * @return 脱敏后的密钥
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 4) {
            return "****";
        }
        return secret.substring(0, 2) + "****" + secret.substring(secret.length() - 2);
    }

    @Override
    public int getOrder() {
        // 在 EventHandlerInit 之后执行，确保事件处理器已注册
        return Ordered.LOWEST_PRECEDENCE;
    }
}

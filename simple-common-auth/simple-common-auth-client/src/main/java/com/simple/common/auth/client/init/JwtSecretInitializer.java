package com.simple.common.auth.client.init;

import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.exchange.AuthCenterHttpClient;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * JWT密钥初始化器（客户端）
 * <p>
 * 应用启动时从授权中心远程拉取JWT密钥并缓存。
 * 仅在客户端模式下执行。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class JwtSecretInitializer implements ApplicationRunner {

    @Autowired
    private TokenManager tokenManager;

    @Autowired(required = false)
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private AuthCenterHttpClient authCenterHttpClient;

    /**
     * 应用启动后执行
     *
     * @param args 应用参数
     */
    @Override
    public void run(ApplicationArguments args) {
        // 仅客户端模式执行
        if (clientAuthInfo == null || !clientAuthInfo.getClient()) {
            log.debug("非客户端模式，跳过JWT密钥初始化");
            return;
        }

        try {
            log.info("开始从授权中心拉取JWT密钥...");
            
            HttpResponse response = authCenterHttpClient.getJwtSecret();
            String body = response.body();
            
            R<?> r = JsonUtils.toJsonObj(body, R.class);
            if (!DefaultExceptionEnum.OK.getCode().equals(r.getCode())) {
                String errorMsg = String.format("从授权中心获取JWT密钥失败: %s", r.getMessage());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            Map<String, String> data = JsonUtils.toJsonObj(r.getData().toString(), Map.class);
            String secret = data.get("secret");
            
            if (secret == null || secret.isEmpty()) {
                String errorMsg = "授权中心返回的JWT密钥为空";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }
            
            tokenManager.addSecret(secret);
            log.info("JWT密钥初始化成功");
            
        } catch (IllegalStateException e) {
            // 重新抛出业务异常，终止应用启动
            throw e;
        } catch (Exception e) {
            String errorMsg = "JWT密钥初始化失败，无法连接授权中心";
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }
}

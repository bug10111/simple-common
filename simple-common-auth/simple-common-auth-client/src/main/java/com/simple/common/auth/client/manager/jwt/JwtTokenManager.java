package com.simple.common.auth.client.manager.jwt;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.event.SecretEvent;
import com.simple.common.auth.client.common.manager.token.AbsTokenManager;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Hutool JWT Token管理器实现
 *
 * @author qty
 */
@Slf4j
//@Primary
@Component
public class JwtTokenManager extends AbsTokenManager {

    @Autowired(required = false)
    private EventBusService eventBusService;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public String create(Map<String, Object> headers, Map<String, Object> payload) {
        return JwtUtils.createJwt(headers, payload);
    }

    @Override
    public Map<String, Object> check(String token, boolean isRefresh) {
        if (!JwtUtils.verify(token)) {
            String tokenDigest = token == null ? "null" : (token.length() <= 10 ? token : token.substring(0, 10)) + "...[len=" + token.length() + "]";
            log.warn("Token 验签失败，摘要: {}", tokenDigest);
            AssertUtils.error(LoginException.RE_LOGIN_EXPIRED, "验签失败");
        } else {
            Map<String, Object> payload = JwtUtils.getPayload(token);
            checkTime(payload, isRefresh);
            return payload;
        }

        //代码不会执行到这里来，这里是编译器报错
        return null;
    }

    @Override
    public void addSecret(String secret, boolean broadcast) {
        AssertUtils.notEmpty(secret, "JWT密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 32, "JWT密钥长度至少为32位，当前长度: " + secret.length());

        // 缓存到本地
        JwtUtils.saveSecret(secret);

        // 发布事件，通知所有客户端同步（仅当broadcast=true且为服务端模式时）
        if (broadcast && !clientAuthInfo.getClient() && eventBusService != null) {
            String projectCode = applicationProperties.getName();
            SecretEvent event = new SecretEvent();
            event.setProjectCode(projectCode); // 设置项目编码
            event.setSecret(secret);
            event.setOperation(SecretEvent.Operation.ADD);
            event.setSecretType(SecretEvent.SecretType.JWT); // 指定为JWT密钥
            eventBusService.push(event);
            log.info("Hutool JWT密钥已添加并发布事件 [{}]，密钥长度: {}", projectCode, secret.length());
        } else {
            log.debug("JWT密钥已加载到本地，不广播");
        }
    }

    @Override
    public String generateSecret() {
        return JwtUtils.createJWTSignerStr();
    }
}

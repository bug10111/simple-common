package com.simple.common.auth.client.manager.jwt;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.event.SecretEvent;
import com.simple.common.auth.client.common.manager.token.AbsTokenManager;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * JJWT Token管理器实现
 *
 * @author qty
 */
@Slf4j
@Primary
@Component
public class JJwtTokenManager extends AbsTokenManager {

    @Autowired(required = false)
    private EventBusService eventBusService;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public String create(Map<String, Object> headers, Map<String, Object> payload) {
        return JJwtUtils.createToken(headers, payload);
    }

    @Override
    public Map<String, Object> check(String token, boolean isRefresh) {
        Map<String, Object> verify = Map.of();
        try {
            verify = JJwtUtils.verify(token);
        } catch (Exception e) {
            String tokenDigest = token == null ? "null" : (token.length() <= 10 ? token : token.substring(0, 10)) + "...[len=" + token.length() + "]";
            log.warn("Token 验签失败，摘要: {}", tokenDigest);
            AssertUtils.error(LoginException.RE_LOGIN_EXPIRED, "验签失败");
        }
        checkTime(verify, isRefresh);
        return verify;
    }

    @Override
    public void addSecret(String secret, String targetProjectCode, boolean broadcast) {
        // 将单个projectCode转换为集合
        List<String> targetCodes = (targetProjectCode != null && !targetProjectCode.isEmpty()) ? List.of(targetProjectCode) : null;
        addSecret(secret, targetCodes, broadcast);
    }

    @Override
    public void addSecret(String secret, List<String> targetProjectCodes, boolean broadcast) {
        AssertUtils.notEmpty(secret, "JWT密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 64, "JWT密钥长度至少为64位，当前长度: " + secret.length());

        // 缓存到本地
        JJwtUtils.saveSecret(secret);

        // 发布事件，通知所有客户端同步（仅当broadcast=true且为服务端模式时）
        if (broadcast && !clientAuthInfo.getClient()) {
            SecretEvent event = new SecretEvent();
            event.setTargetProjectCodes(targetProjectCodes); // 设置目标项目编码集合
            event.setSecret(secret);
            event.setOperation(SecretEvent.Operation.ADD);
            event.setSecretType(SecretEvent.SecretType.JWT); // 指定为JWT密钥
            eventBusService.push(event);

            String targetDesc = (targetProjectCodes != null && !targetProjectCodes.isEmpty()) ? targetProjectCodes.toString() : applicationProperties.getName();
            log.info("JJWT密钥已添加并发布事件，目标项目: {}，密钥长度: {}", targetDesc, secret.length());
        }
    }

    @Override
    public String generateSecret() {
        return JJwtUtils.createSecret();
    }
}

package com.simple.common.auth.client.manager.jwt;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.token.AbsTokenManager;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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
    public void addSecret(String secret) {
        AssertUtils.notEmpty(secret, "JWT密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 64, "JWT密钥长度至少为64位，当前长度: " + secret.length());

        // 仅缓存到本地，不涉及远程广播
        JJwtUtils.saveSecret(secret);
        log.debug("JJWT密钥已缓存");
    }

    @Override
    public String generateSecret() {
        return JJwtUtils.createSecret();
    }
}

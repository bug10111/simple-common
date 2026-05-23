package com.simple.common.auth.client.manager.jwt;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.token.AbsTokenManager;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
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
    public void addSecret(String secret) {
        AssertUtils.notEmpty(secret, "JWT密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 32, "JWT密钥长度至少为32位，当前长度: " + secret.length());

        // 仅缓存到本地，不涉及远程广播
        JwtUtils.saveSecret(secret);
        log.debug("Hutool JWT密钥已缓存");
    }

    @Override
    public String generateSecret() {
        return JwtUtils.createJWTSignerStr();
    }
}

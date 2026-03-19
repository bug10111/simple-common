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
            String tokenDigest = (token == null || token.length() < 10) ? "null" : token.substring(0, Math.min(10, token.length())) + "...[len=" + token.length() + "]";
            log.warn("Token 验签失败，摘要: {}", tokenDigest);
            AssertUtils.error(LoginException.RE_LOGIN_EXPIRED, "验签失败");
        }
        checkTime(verify, isRefresh);
        return verify;
    }
}

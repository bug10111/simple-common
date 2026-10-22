package com.simple.common.auth.client.manager.jwt;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.token.AbsTokenManager;
import com.simple.common.auth.client.util.JJwtUtils;
import com.simple.common.core.utils.AssertUtils;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * JJWT Token管理器实现
 * <p>
 * 异常分型契约：过期（ExpiredJwtException）按 isRefresh 区分 LOGIN_EXPIRED/RE_LOGIN_EXPIRED
 * （access 过期应走刷新链路而非强制重登）；其余验签/格式/密钥异常统一 RE_LOGIN_EXPIRED，
 * 异常根因记入日志（含堆栈），token 摘要不携带可重放凭证。
 * </p>
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
        Map<String, Object> verify;
        try {
            verify = JJwtUtils.verify(token);
            // 过期校验并入 try：exp 缺失/非数值抛出的受控异常不逃逸为 500
            checkTime(verify, isRefresh);
        } catch (ExpiredJwtException e) {
            // 过期与验签失败分型：access 过期引导客户端刷新，refresh 过期才要求重新登录
            log.warn("Token 已过期，摘要: {}", tokenDigest(token), e);
            AssertUtils.error(isRefresh ? LoginException.RE_LOGIN_EXPIRED : LoginException.LOGIN_EXPIRED);
            return null;
        } catch (Exception e) {
            log.warn("Token 验签失败，摘要: {}", tokenDigest(token), e);
            AssertUtils.error(LoginException.RE_LOGIN_EXPIRED, "验签失败");
            return null;
        }
        return verify;
    }

    @Override
    public void addSecret(String secret) {
        AssertUtils.notEmpty(secret, "JWT密钥不能为空");
        AssertUtils.isTrue(secret.length() >= 64, "JWT密钥长度至少为64位（HS512要求），当前长度: " + secret.length());

        // 仅缓存到本地，不涉及远程广播
        JJwtUtils.saveSecret(secret);
        log.debug("JJWT密钥已缓存");
    }

    @Override
    public String generateSecret() {
        return JJwtUtils.createSecret();
    }

    /**
     * 非可逆摘要：仅记录长度信息用于日志排查，不输出任何凭证原文片段
     */
    private String tokenDigest(String token) {
        return token == null ? "null" : "[len=" + token.length() + "]";
    }
}

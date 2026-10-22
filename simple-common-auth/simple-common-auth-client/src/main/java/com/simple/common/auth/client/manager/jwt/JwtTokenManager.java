package com.simple.common.auth.client.manager.jwt;

import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.token.AbsTokenManager;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.core.utils.AssertUtils;
import cn.hutool.core.util.ObjUtil;
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
        // 空/空白 token 显式拦截（JwtUtils.verify 对非法入参直接返回 false，此处给出明确业务语义）
        if (ObjUtil.isEmpty(token)) {
            log.warn("Token 校验失败：token为空");
            AssertUtils.error(LoginException.RE_LOGIN_EXPIRED, "Token不能为空");
        }
        if (!JwtUtils.verify(token)) {
            log.warn("Token 验签失败，摘要: {}", tokenDigest(token));
            AssertUtils.error(LoginException.RE_LOGIN_EXPIRED, "验签失败");
        }
        Map<String, Object> payload = JwtUtils.getPayload(token);
        checkTime(payload, isRefresh);
        return payload;
    }

    @Override
    public void addSecret(String secret) {
        AssertUtils.notEmpty(secret, "JWT密钥不能为空");
        // HS512 要求 ≥64 字节密钥（RFC 7518），32 位密钥弱化签名强度
        AssertUtils.isTrue(secret.length() >= 64, "JWT密钥长度至少为64位（HS512要求），当前长度: " + secret.length());

        // 仅缓存到本地，不涉及远程广播
        JwtUtils.saveSecret(secret);
        log.debug("Hutool JWT密钥已缓存");
    }

    @Override
    public String generateSecret() {
        return JwtUtils.createJWTSignerStr();
    }

    /**
     * 非可逆摘要：仅记录长度信息用于日志排查，不输出任何凭证原文片段
     */
    private String tokenDigest(String token) {
        return "[len=" + token.length() + "]";
    }
}

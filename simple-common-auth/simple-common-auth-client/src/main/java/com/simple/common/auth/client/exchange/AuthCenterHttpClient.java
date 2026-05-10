package com.simple.common.auth.client.exchange;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.AuthEndpointEnum;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.core.utils.HttpServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 授权中心 HTTP 客户端默认实现
 * <p>
 * 使用 Hutool HttpRequest 发起 HTTP 请求，统一管理所有向授权中心的调用。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class AuthCenterHttpClient {

    @Autowired
    private AuthProperties authProperties;

    /**
     * 获取用户内省信息
     *
     * @return HTTP 响应
     */
    public HttpResponse getUserInfo() {
        String url = AuthEndpointEnum.USER_INFO.buildUrl(authProperties.getServerUrl());
        log.debug("请求用户信息: {}", url);
        String authorization = HttpServletUtils.getRequest().getHeader(TokenConstant.Authorization);
        return HttpRequest.get(url).header("Authorization", authorization).execute();
    }

    /**
     * 获取当前签名密钥
     *
     * @return HTTP 响应
     * @deprecated 已废弃，请使用 {@link #getUnifiedSecrets(String)} 统一获取双密钥
     */
    @Deprecated
    public HttpResponse getSignSecret() {
        String url = AuthEndpointEnum.SIGN_SECRET.buildUrl(authProperties.getServerUrl());
        log.debug("请求签名密钥: {}", url);
        return HttpRequest.get(url).execute();
    }

    /**
     * 获取当前 JWT 密钥
     *
     * @return HTTP 响应
     * @deprecated 已废弃，请使用 {@link #getUnifiedSecrets(String)} 统一获取双密钥
     */
    @Deprecated
    public HttpResponse getJwtSecret() {
        String url = AuthEndpointEnum.JWT_SECRET.buildUrl(authProperties.getServerUrl());
        log.debug("请求JWT密钥: {}", url);
        String authorization = HttpServletUtils.getRequest().getHeader(TokenConstant.Authorization);
        return HttpRequest.get(url).header("Authorization", authorization).execute();
    }

    /**
     * 统一获取双密钥（JWT + SIGN）
     *
     * @param projectCode 项目编码（spring.application.name）
     * @return HTTP 响应
     */
    public HttpResponse getUnifiedSecrets(String projectCode) {
        String url = AuthEndpointEnum.UNIFIED_SECRETS.buildUrl(authProperties.getServerUrl()) + "?projectCode=" + projectCode;
        log.debug("请求统一密钥 [{}]: {}", projectCode, url);
        return HttpRequest.get(url).execute();
    }
}

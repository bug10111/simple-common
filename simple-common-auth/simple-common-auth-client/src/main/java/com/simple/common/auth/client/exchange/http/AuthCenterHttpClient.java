package com.simple.common.auth.client.exchange.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.core.utils.Base64Utils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 授权中心 HTTP 客户端默认实现
 * <p>
 * 使用 Hutool HttpRequest 发起 HTTP 请求，统一管理所有向授权中心的调用。
 * 旧统一密钥拉取（/auth/api/secrets）已随守卫化退役，密钥获取收敛到
 * 实例注册自举（注册响应携带当前密钥）。
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
     * 实例注册（注册自举）
     * <p>
     * 携带 Basic 客户端凭证上报实例身份与接收回调地址，注册响应携带当前密钥。
     * </p>
     *
     * @param projectCode 项目编码（即客户端编码）
     * @param clientSecret 客户端凭证
     * @param instanceIp   实例本机 IP
     * @param callbackUrl  实例接收回调地址
     * @return HTTP 响应（body 为 R，data 含 jwt/sign 密钥）
     */
    public HttpResponse registerInstance(String projectCode, String clientSecret, String instanceIp, String callbackUrl) {
        String url = AuthEndpointEnum.REGISTER_INSTANCE.buildUrl(authProperties.getServerUrl());
        String authorization = TokenConstant.basic + Base64Utils.encode((projectCode + ":" + clientSecret).getBytes());

        Map<String, String> body = new HashMap<>();
        body.put("projectCode", projectCode);
        body.put("instanceIp", instanceIp);
        body.put("callbackUrl", callbackUrl);

        log.debug("实例注册: url={} projectCode={}", url, projectCode);
        return HttpRequest.post(url)
                          .header("Content-Type", "application/json")
                          .header("Authorization", authorization)
                          .body(JsonUtils.toJsonStr(body))
                          .timeout(authProperties.getTimeOut())
                          .execute();
    }
}

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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 授权中心 HTTP 客户端默认实现
 * <p>
 * 使用 Hutool HttpRequest 发起 HTTP 请求，统一管理所有向授权中心的调用。
 * 旧统一密钥拉取（/auth/api/secrets）已随守卫化退役，密钥获取收敛到
 * 实例注册自举（注册响应携带当前密钥）。
 * </p>
 * <p>
 * 响应体契约：返回的 {@link HttpResponse} 持有未关闭的连接流，<b>调用方必须 try-with-resources 关闭</b>；
 * 未关闭将按请求泄漏连接（本模块内调用方均已按此契约处理）。
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
     * @return HTTP 响应（调用方负责关闭）
     * @throws IllegalStateException 当前线程无请求上下文或未携带 Authorization 头
     *                               （内省必须透传调用方凭证，静默去掉凭证只会让失败延后且更难排查）
     */
    public HttpResponse getUserInfo() {
        String url = AuthEndpointEnum.USER_INFO.buildUrl(authProperties.getServerUrl());
        log.debug("请求用户信息: {}", url);
        String authorization = HttpServletUtils.getRequest().getHeader(TokenConstant.Authorization);
        if (authorization == null || authorization.isEmpty()) {
            throw new IllegalStateException("缺少 Authorization 请求头，无法向授权中心内省用户信息");
        }
        // 必须设置超时：授权中心无响应时不得无限阻塞调用线程
        return HttpRequest.get(url).header("Authorization", authorization)
                .timeout(authProperties.getTimeOut()).execute();
    }

    /**
     * 实例注册（注册自举）
     * <p>
     * 携带 Basic 客户端凭证（clientId + clientSecret）上报实例身份与接收回调地址，
     * 请求体携带 projectCode（服务端约定 clientId 即项目编码，两者同值校验），
     * 注册响应携带当前密钥。
     * </p>
     * <p>
     * 传输安全：serverUrl 为明文 http 时凭证与下发密钥均明文传输，
     * 此处记录告警（生产环境应配置 https）；本机/内网调试形态保留 http 可用。
     * </p>
     *
     * @param clientId     客户端编码（即项目编码，simple.auth.client-id 优先，缺省 spring.application.name）
     * @param clientSecret 客户端凭证
     * @param instanceIp   实例本机 IP
     * @param callbackUrl  实例接收回调地址
     * @return HTTP 响应（body 为 R，data 含 jwt/sign 密钥；调用方负责关闭）
     */
    public HttpResponse registerInstance(String clientId, String clientSecret, String instanceIp, String callbackUrl) {
        String serverUrl = authProperties.getServerUrl();
        if (serverUrl != null && serverUrl.startsWith("http://")) {
            log.warn("授权中心地址使用明文 HTTP（{}）：clientSecret 与下发密钥将明文传输，生产环境必须使用 https", serverUrl);
        }
        String url = AuthEndpointEnum.REGISTER_INSTANCE.buildUrl(serverUrl);
        // 显式 UTF-8：JVM 默认字符集随环境漂移（Windows GBK/Linux UTF-8），非 ASCII 凭证跨环境 Basic 解码不一致
        String authorization = TokenConstant.basic + Base64Utils.encode((clientId + ":" + clientSecret)
                .getBytes(StandardCharsets.UTF_8));

        Map<String, String> body = new HashMap<>();
        body.put("projectCode", clientId);
        body.put("instanceIp", instanceIp);
        body.put("callbackUrl", callbackUrl);

        log.debug("实例注册: url={} clientId={}", url, clientId);
        return HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", authorization)
                .body(JsonUtils.toJsonStr(body))
                .timeout(authProperties.getTimeOut())
                .execute();
    }
}

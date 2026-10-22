package com.simple.common.auth.client.init;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.exchange.http.AuthCenterHttpClient;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Map;

/**
 * 实例注册自举初始化器（客户端）
 * <p>
 * 客户端模式应用启动时向授权中心注册实例（Basic 客户端凭证 + projectCode==clientId），
 * 注册响应同步携带当前密钥（注册自举），实例即刻获得当前密钥态；
 * 注册身份= simple.auth.client-id（显式配置优先，缺省回落 spring.application.name），
 * Basic 凭证= client-id + simple.auth.client-secret；
 * 未注册客户端被明确拒绝并引导控制台注册，注册失败即终止应用启动（密钥缺失无法验签，带病启动无意义）。
 * 旧 HTTP 密钥拉取路径（/auth/api/secrets）已随守卫化整体退役。
 * 投递引擎后续变更（密钥轮换/权限/登出）经 /internal/sync/** 回调本实例。
 * </p>
 * <p>
 * 生命周期说明：挂载于 ApplicationReadyEvent（Web 已就绪后执行）——注册失败经
 * SpringApplication.run 的失败处理关闭上下文，标准 Boot 部署不会带病存活；
 * 自举需要完整装配（配置/客户端/事件链路），迁前置钩子会破坏既有启动时序契约，保持现状。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class JwtSecretClientInitializer implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private SignManager signManager;

    @Autowired(required = false)
    private ClientAuthInfo clientAuthInfo;

    @Autowired
    private AuthCenterHttpClient authCenterHttpClient;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 应用启动后执行
     *
     * @param event 应用就绪事件
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 仅客户端模式执行
        if (clientAuthInfo == null || !clientAuthInfo.getClient()) {
            return;
        }

        // 注册身份解析（clientId 显式配置优先，缺省回落 spring.application.name；服务端约定 clientId 即项目编码）
        String clientId = resolveClientId();
        // 身份空值在本地显式失败：远端 401/400 掩盖根因，必须给明确配置指引
        if (StrUtil.isBlank(clientId)) {
            throw new IllegalStateException("客户端编码未配置，请在 application.yml 配置 simple.auth.client-id 或 spring.application.name");
        }

        // 注册入参校验（凭证/回调地址缺失明确指引配置项）
        String clientSecret = authProperties.getClientSecret();
        if (StrUtil.isEmpty(clientSecret)) {
            throw new IllegalStateException("客户端凭证未配置，请在 application.yml 配置 simple.auth.client-secret（实例注册自举依赖）");
        }
        String callbackUrl = authProperties.getInstanceCallbackUrl();
        if (StrUtil.isEmpty(callbackUrl)) {
            throw new IllegalStateException("实例回调地址未配置，请在 application.yml 配置 simple.auth.instance-callback-url（授权中心投递引擎按此回调本实例）");
        }
        String instanceIp = resolveInstanceIp();

        try {
            log.info("开始向授权中心注册实例 [{}]...", clientId);

            // 实例注册（Basic 客户端凭证 + projectCode，注册自举：响应携带当前密钥）
            int status;
            String body;
            try (HttpResponse response = authCenterHttpClient.registerInstance(clientId, clientSecret, instanceIp, callbackUrl)) {
                status = response.getStatus();
                body = response.body();
            }

            // 注册失败分型引导：未注册（客户端未在控制台登记）与其它失败给出不同处置指引
            if (status != DefaultExceptionEnum.OK.getCodeInt()) {
                throw new IllegalStateException(String.format(
                        "实例注册失败: HTTP %s（未注册请先在授权中心控制台完成项目客户端注册后再接入）", status));
            }

            R<?> r = null;
            try {
                r = JsonUtils.toJsonObj(body, R.class);
            } catch (Exception e) {
                log.warn("注册响应非合法 JSON：status={}", status);
            }
            if (r == null || !DefaultExceptionEnum.OK.getCode().equals(r.getCode())) {
                String errorMsg = String.format("实例注册失败: %s",
                        r == null ? "响应格式非法(HTTP " + status + ")" : r.getMessage());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // 解析自举密钥（data/解析产物逐级校验：JSON 序列化 "null" 或空 Map 都不能静默流入密钥装载）
            if (r.getData() == null) {
                throw new IllegalStateException("实例注册响应缺少密钥数据");
            }
            Map<String, String> secrets = JsonUtils.toJsonObj(JsonUtils.toJsonStr(r.getData()), Map.class);
            if (secrets == null || secrets.isEmpty()) {
                throw new IllegalStateException("实例注册响应密钥数据格式非法");
            }
            String jwtSecret = secrets.get("jwt");
            String signSecret = secrets.get("sign");

            if (StrUtil.isEmpty(jwtSecret)) {
                throw new IllegalStateException("注册响应携带的JWT密钥为空");
            }
            if (StrUtil.isEmpty(signSecret)) {
                throw new IllegalStateException("注册响应携带的SIGN密钥为空");
            }

            // 加载JWT密钥（仅本地缓存）
            tokenManager.addSecret(jwtSecret);
            log.info("JWT密钥自举成功: {}", maskSecret(jwtSecret));

            // 加载SIGN密钥（仅本地缓存）
            signManager.addSecret(signSecret);
            log.info("SIGN密钥自举成功: {}", maskSecret(signSecret));

        } catch (IllegalStateException e) {
            // 重新抛出业务异常，终止应用启动
            throw e;
        } catch (Exception e) {
            // 连接/IO 失败保持原异常链（原实现吞掉真实根因，全部误报为连接失败）
            String errorMsg = "实例注册失败";
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * 解析注册客户端编码（simple.auth.client-id 显式配置优先，缺省回落 spring.application.name）
     *
     * @return 注册用客户端编码（服务端约定 clientId 即项目编码，配置后须与 project-code 同值）
     */
    private String resolveClientId() {
        String configured = authProperties.getClientId();
        if (StrUtil.isNotEmpty(configured)) {
            return configured;
        }
        return applicationProperties.getName();
    }

    /**
     * 解析实例本机 IP（配置优先，缺省探测本机地址）
     *
     * @return 实例本机 IP
     */
    private String resolveInstanceIp() {
        String configured = authProperties.getInstanceIp();
        if (StrUtil.isNotEmpty(configured)) {
            return configured;
        }

        // 探测本机地址（容器部署建议显式配置 simple.auth.instance-ip）
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            String errorMsg = "实例 IP 探测失败，请在 application.yml 显式配置 simple.auth.instance-ip";
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    /**
     * 脱敏显示密钥
     *
     * @param secret 原始密钥
     * @return 脱敏后的密钥
     */
    private String maskSecret(String secret) {
        if (secret == null || secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
    }

    @Override
    public int getOrder() {
        // 在 EventHandlerInit 之后执行，确保事件处理器已注册
        return Ordered.LOWEST_PRECEDENCE;
    }
}

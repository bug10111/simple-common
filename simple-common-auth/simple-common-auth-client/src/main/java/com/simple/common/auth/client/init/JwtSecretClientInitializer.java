package com.simple.common.auth.client.init;

import cn.hutool.http.HttpResponse;
import cn.hutool.core.util.StrUtil;
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
 * 未注册客户端被明确拒绝并引导控制台注册，注册失败即终止应用启动（密钥缺失无法验签，带病启动无意义）。
 * 旧 HTTP 密钥拉取路径（/auth/api/secrets）已随守卫化整体退役。
 * 投递引擎后续变更（密钥轮换/权限/登出）经 /internal/sync/** 回调本实例。
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
     * @param args 应用参数
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent args) {
        // 仅客户端模式执行
        if (clientAuthInfo == null || !clientAuthInfo.getClient()) {
            return;
        }

        // 注册入参校验（凭证/回调地址缺失明确指引配置项）
        String projectCode = applicationProperties.getName();
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
            log.info("开始向授权中心注册实例 [{}]...", projectCode);

            // 实例注册（注册自举：响应携带当前密钥）
            HttpResponse response = authCenterHttpClient.registerInstance(projectCode, clientSecret, instanceIp, callbackUrl);
            R<?> r = JsonUtils.toJsonObj(response.body(), R.class);
            if (!DefaultExceptionEnum.OK.getCode().equals(r.getCode())) {
                String errorMsg = String.format("实例注册失败: %s（未注册请先在授权中心控制台完成项目客户端注册后再接入）", r.getMessage());
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            // 解析自举密钥
            Map<String, String> secrets = JsonUtils.toJsonObj(JsonUtils.toJsonStr(r.getData()), Map.class);
            String jwtSecret = secrets.get("jwt");
            String signSecret = secrets.get("sign");

            if (jwtSecret == null || jwtSecret.isEmpty()) {
                String errorMsg = "注册响应携带的JWT密钥为空";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            if (signSecret == null || signSecret.isEmpty()) {
                String errorMsg = "注册响应携带的SIGN密钥为空";
                log.error(errorMsg);
                throw new IllegalStateException(errorMsg);
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
            String errorMsg = "实例注册失败，无法连接授权中心";
            log.error(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
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

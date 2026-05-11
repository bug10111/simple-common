package com.simple.common.auth.client.common.properties;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * <p>
 * auth配置类
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth")
public class AuthProperties {

    //服务端地址
    private String serverUrl = "http://localhost:8000";

    //客户端获取服务端用户信息的超时时间
    private Long timeOut = 1000 * 10L;

    //解密是否校验有效时间
    private Boolean decryptCheckValidityPeriod = false;

    //解密字符串分割符
    private String decryptSplitStr = ":";

    //解密字符串随机字符串标志
    private String nonce = "nonce:";

    //解密字符串有效期,单位分钟
    private Integer decryptValidityPeriod = 2;

    //单位时间内，登录失败的最大次数的key
    private String loginErrorKey = "loginError:";

    //单位时间内，登录失败的最大次数
    private int loginErrorNumber = 5;

    //登陆失败计次的单位时间
    private int loginErrorTime = 60 * 60 * 24;

    //IP登录失败key前缀
    private String loginIpErrorKey = "login:ip:error:";

    //缓存类型，默认使用Redis缓存
    private CacheTypeEnum cacheType = CacheTypeEnum.REDIS;

    //权限缓存过期时间（单位：秒），默认24小时
    private Integer permissionCacheExpire = 60 * 60 * 24;

    //项目编码（可选），用于权限隔离。如果不配置，将自动使用 Spring Application Name
    private String projectCode;

    @Autowired(required = false)
    private com.simple.common.core.common.properties.ApplicationProperties applicationProperties;

    /**
     * 获取项目编码
     * <p>
     * 优先使用配置的 projectCode，如果未配置则自动使用 Spring Application Name。
     * </p>
     *
     * @return 项目编码
     */
    public String getProjectCode() {
        if (projectCode != null && !projectCode.isEmpty()) {
            return projectCode;
        }
        // 自动使用 Spring Application Name
        if (applicationProperties != null && applicationProperties.getName() != null) {
            return applicationProperties.getName();
        }
        throw new IllegalStateException("项目编码未配置，请在 application.yml 中配置 simple.auth.project-code 或 spring.application.name");
    }

    /**
     * 获取登录失败保存的key
     */
    public String getKey(String key) {
        return loginErrorKey + key;
    }

    /**
     * 获取IP登录失败保存的key
     */
    public String getIpErrorKey(String ip) {
        return loginIpErrorKey + ip;
    }
}

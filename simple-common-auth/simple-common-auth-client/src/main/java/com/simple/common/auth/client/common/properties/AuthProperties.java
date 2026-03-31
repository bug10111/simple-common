package com.simple.common.auth.client.common.properties;

import lombok.Getter;
import lombok.Setter;
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
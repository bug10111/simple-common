package com.simple.common.auth.client.common.properties;

import com.simple.common.auth.client.common.entity.decry.ClientDecry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.security.Policy;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * <p>
 * auth配置类
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth")
public class AuthProperties {

    //jwt密钥
    private String JWTSigner = "V4O2UvwbrPKoH3fdyMpdczDTgnd0O5zgYAYrrTBXsPmgUrxhMpUj2a0Cz8OQMGub";

    //服务端地址
    private String serverUrl = "http://localhost:8000";

    //签名token类型
    private String decryptSign = "X-SIGN-TOKEN";

    //客户端获取服务端用户信息的超时时间
    private Long timeOut = 1000 * 10L;

    //解密是否校验有效时间
    private Boolean decryptCheckValidityPeriod = false;

    //解密字符串时间分割符
    private String decryptSplitStr = "&&time=";

    //解密字符串有效期,单位分钟
    private Integer decryptValidityPeriod = 2;

    //单位时间内，登录失败的最大次数的key
    private String loginErrorKey = "loginError:";

    //单位时间内，登录失败的最大次数
    private int loginErrorNumber = 5;

    //登陆失败计次的单位时间
    private int loginErrorTime = 60 * 60 * 24;

    /**
     * 获取登录失败保存的key
     */
    public String getKey(String key) {
        return loginErrorKey + key;
    }
}

package com.simple.common.oauth.start.common.properties;

import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.oauth.start.common.enums.OauthUrl;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth.oauth.start")
public class OauthStartProperties {

    //超时时间 毫秒
    private Integer timeOut = 20000;

    //创建用户默认密码
    private String defaultPwd = "123456";

    @Autowired
    private AuthProperties authProperties;

    /**
     * 获取请求地址
     * @param oauthUrl 枚举
     */
    public String getUrl(OauthUrl oauthUrl) {
        return authProperties.getServerUrl() + oauthUrl.getUrl();
    }
}

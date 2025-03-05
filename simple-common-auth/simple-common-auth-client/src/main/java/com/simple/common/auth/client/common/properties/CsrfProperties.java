package com.simple.common.auth.client.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 跨站请求伪造配置
 *
 * @author 兄台丶请冷静
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.auth.csrf")
public class CsrfProperties {

    //是否开启csrf校验
    private boolean csrfDefense = true;

    //前端请求值
    private String csrfTokenHeader = "X-CSRF-TOKEN";

    //缓存标志
    private String xssKey = "xss:";

    //缓存时间
    private int cacheTime = 60 * 10;

    /**
     * 获取xss短token key
     *
     * @param url    请求路径
     * @param userId 用户Id
     */
    public String getKey(String url, String userId) {
        return xssKey + userId + "&&" + url;
    }
}

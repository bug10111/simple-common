package com.simple.common.auth.client.common.properties;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * <p>
 * csrf配置类
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.csrf")
public class CsrfProperties {

    //是否开启csrf防御
    private boolean csrfDefense = true;

    //请求头中的csrf token名称
    private String csrfHeader = "X-CSRF-TOKEN";

    //缓存标志
    private String csrfKey = "csrf:";

    //缓存时间单位秒
    private int cacheTime = 60 * 30;

    //缓存类型，默认使用Redis缓存
    private CacheTypeEnum cacheType = CacheTypeEnum.REDIS;

    /**
     * 获取短token key
     *
     * @param url    请求路径
     * @param userId 用户Id
     */
    public String getKey(String url, String userId) {
        return csrfKey + userId + "&&" + url;
    }
}
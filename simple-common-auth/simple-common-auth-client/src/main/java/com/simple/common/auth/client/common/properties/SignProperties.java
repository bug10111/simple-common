package com.simple.common.auth.client.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * <p>
 * sign配置类
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.sign")
public class SignProperties {

    //是否开启sign校验
    private boolean signDefense = true;

    //签名token类型
    private String sign = "X-SIGN";

    //时间戳
    private String timestamp = "X-TIMESTAMP";

    //唯一标志
    private String nonce = "X-NONCE";

    //缓存标志
    private String signKey = "sign:";

    //缓存时间单位秒
    private int cacheTime = 60 * 5;

    //默认时间窗口（毫秒），超出此范围的时间戳视为无效
    private long defaultTimeWindowMs = 5 * 60 * 1000;

    /**
     * 获取短sign key
     *
     * @param userId 用户Id
     */
    public String getKey(String userId) {
        return signKey + userId;
    }
}

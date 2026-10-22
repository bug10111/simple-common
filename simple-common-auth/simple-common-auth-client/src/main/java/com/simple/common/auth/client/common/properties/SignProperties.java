package com.simple.common.auth.client.common.properties;

import com.simple.common.auth.client.common.enums.CacheTypeEnum;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * <p>
 * sign配置类
 * </p>
 *
 * @author qty
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.sign")
public class SignProperties {

    //是否开启sign校验（false 时 @Sign 全部跳过验签，启动生效并有告警日志，属显式降级开关）
    private boolean signDefense = true;

    //签名token类型（签名请求头名称，空白视为误配置回退默认值）
    private String sign = "X-SIGN";

    //时间戳（时间戳请求头名称）
    private String timestamp = "X-TIMESTAMP";

    //唯一标志（nonce 请求头名称）
    private String nonce = "X-NONCE";

    //缓存标志（nonce 防重放键前缀）
    private String signKey = "sign:";

    //缓存时间单位秒（nonce 记录 TTL；实际 TTL 取 max(cacheTime, defaultTimeWindowMs/1000+60)，保证覆盖时间戳窗口）
    private int cacheTime = 60 * 5;

    //默认时间窗口（毫秒），超出此范围的时间戳视为无效
    private long defaultTimeWindowMs = 5 * 60 * 1000;

    //缓存类型，默认使用Redis缓存（多实例部署必须用 REDIS：LOCAL 的 nonce 记录仅本实例可见，跨实例可重放）
    private CacheTypeEnum cacheType = CacheTypeEnum.REDIS;

    /**
     * 配置兜底校验：nonce 缓存时间必须覆盖时间戳窗口，请求头名称不允许空白。
     */
    @PostConstruct
    public void validate() {
        if (cacheTime <= 0) {
            log.warn("simple.sign.cache-time={} 非法（必须>0），已回退默认值 {}s", cacheTime, 60 * 5);
            cacheTime = 60 * 5;
        }
        if (defaultTimeWindowMs <= 0) {
            log.warn("simple.sign.default-time-window-ms={} 非法（必须>0），已回退默认值 {}ms", defaultTimeWindowMs, 5 * 60 * 1000);
            defaultTimeWindowMs = 5 * 60 * 1000;
        }
        sign = defaultHeader(sign, "X-SIGN");
        timestamp = defaultHeader(timestamp, "X-TIMESTAMP");
        nonce = defaultHeader(nonce, "X-NONCE");
        if (signKey == null || signKey.isEmpty()) {
            log.warn("simple.sign.sign-key 为空，已回退默认前缀 sign:");
            signKey = "sign:";
        }
    }

    private String defaultHeader(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("签名请求头名称为空白，已回退默认值 {}", fallback);
            return fallback;
        }
        return value;
    }

    /**
     * 获取短sign key
     * <p>
     * 当前模块内暂无消费方（nonce 键经 DefaultSignManager 以 signKey 前缀自行构建）；
     * 保留 API 供下游扩展使用，空 userId 直接拒绝以防共享键串写。
     * </p>
     *
     * @param userId 用户Id（不允许为空）
     */
    public String getKey(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("用户Id不能为空");
        }
        return signKey + userId;
    }
}

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
 * csrf配置类
 * </p>
 *
 * @author qty
 */
@Slf4j
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

    //缓存时间单位秒（<=0 视为误配置，启动时回退默认值，避免 token 永不生效或 Redis 拒绝非法 TTL）
    private int cacheTime = 60 * 30;

    //缓存类型，默认使用Redis缓存
    private CacheTypeEnum cacheType = CacheTypeEnum.REDIS;

    /**
     * 配置兜底校验：cacheTime <= 0 时回退默认 30 分钟。
     */
    @PostConstruct
    public void normalizeCacheTime() {
        if (cacheTime <= 0) {
            log.warn("simple.csrf.cache-time={} 非法（必须>0），已回退默认值 {}s", cacheTime, 60 * 30);
            cacheTime = 60 * 30;
        }
    }

    /**
     * 获取短token key
     * <p>
     * 注意参数顺序为 (url, userId)：与 CsrfService API 的 (userId, path) 相反，调用方须留意。
     * </p>
     *
     * @param url    请求路径（不允许为空）
     * @param userId 用户Id（不允许为空；为空会退化为 csrf:null&&xxx 之类的共享键，请求间互相覆盖）
     */
    public String getKey(String url, String userId) {
        if (userId == null || userId.isEmpty() || url == null || url.isEmpty()) {
            throw new IllegalArgumentException("生成 CSRF 缓存 key 失败：userId 与 url 均不允许为空");
        }
        return csrfKey + userId + "&&" + url;
    }
}

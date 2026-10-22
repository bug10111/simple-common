package com.simple.common.redis.common.constant;

/**
 * Created by IntelliJ IDEA
 * Description: 缓存要用到的常量
 *
 * @author qty
 */
public final class RedisCacheConstant {

    private RedisCacheConstant() {
        // 常量类禁止实例化
    }

    /**
     * 缓存标识的默认值（哨兵值）：表示「使用当前请求的 URI 作为缓存 Key 前缀」。
     * <p>
     * 注意：该值仅作为 {@code @RedisCache#head()} 的哨兵判断依据
     * （见 {@code DefaultRedisCacheAspectManager#getCacheKey} 中 {@code REQ_URL.equals(head())}），
     * 不是真实的 Redis Key 前缀；业务方显式设置 {@code head = "req_url"} 时将被解读为
     * 「使用请求 URI」而不是字面前缀，请勿以该字面值作为真实业务前缀使用。
     */
    public static final String REQ_URL = "req_url";

}

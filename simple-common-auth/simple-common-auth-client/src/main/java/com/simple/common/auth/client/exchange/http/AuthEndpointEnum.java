package com.simple.common.auth.client.exchange.http;

/**
 * 授权中心 API 端点枚举
 * <p>
 * 统一管理客户端向授权中心发起的所有 HTTP 请求 URL。
 * </p>
 *
 * @author qty
 */
public enum AuthEndpointEnum {

    /**
     * 获取用户内省信息
     */
    USER_INFO("/auth/api/user", "GET"),

    /**
     * 获取当前签名密钥
     * @deprecated 已废弃，请使用 {@link #UNIFIED_SECRETS} 统一获取双密钥
     */
    @Deprecated
    SIGN_SECRET("/auth/api/sign/secret", "GET"),

    /**
     * 获取当前 JWT 密钥
     * @deprecated 已废弃，请使用 {@link #UNIFIED_SECRETS} 统一获取双密钥
     */
    @Deprecated
    JWT_SECRET("/auth/api/jwt/secret", "GET"),

    /**
     * 统一获取双密钥（JWT + SIGN）
     * <p>
     * 推荐使用此接口，一次性获取JWT和SIGN两种密钥。
     * </p>
     */
    UNIFIED_SECRETS("/auth/api/secrets", "GET");

    private final String path;
    private final String method;

    AuthEndpointEnum(String path, String method) {
        this.path = path;
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    /**
     * 拼接完整 URL
     *
     * @param baseUrl 授权中心基础 URL
     * @return 完整 URL
     */
    public String buildUrl(String baseUrl) {
        return baseUrl + path;
    }
}

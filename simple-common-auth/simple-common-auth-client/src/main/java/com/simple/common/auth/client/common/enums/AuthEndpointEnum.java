package com.simple.common.auth.client.common.enums;

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
     */
    SIGN_SECRET("/auth/api/sign/secret", "GET"),

    /**
     * 获取当前 JWT 密钥
     */
    JWT_SECRET("/auth/api/jwt/secret", "GET");

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

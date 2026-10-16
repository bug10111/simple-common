package com.simple.common.auth.client.exchange.http;

/**
 * 授权中心 API 端点枚举
 * <p>
 * 统一管理客户端向授权中心发起的所有 HTTP 请求 URL。
 * 旧密钥拉取端点（/auth/api/secrets 等）已随守卫化整体退役，
 * 客户端密钥获取收敛到实例注册自举（REGISTER_INSTANCE 注册响应携带）。
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
     * 实例注册（注册自举）
     * <p>
     * 实例启动上报身份与本机信息，注册响应同步携带当前密钥；
     * 未注册客户端被明确拒绝并引导控制台注册。
     * </p>
     */
    REGISTER_INSTANCE("/auth/api/instance/register", "POST");

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

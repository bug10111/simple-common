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

    /**
     * 声明的 HTTP 方法（GET/POST）。当前客户端按端点硬编码发起对应动词，
     * 本字段仅作端点自描述元数据；变更本值不会改变实际请求（以免误导，见 {@link com.simple.common.auth.client.exchange.http.AuthCenterHttpClient}）。
     */
    public String getMethod() {
        return method;
    }

    /**
     * 拼接完整 URL
     * <p>
     * baseUrl 非空校验 + 去尾部斜杠归一化：配置尾斜杠会产生 //auth/... 之类的畸形路径，
     * 空值会产生 null/auth/... 并以晦涩的连接错误暴露。
     * </p>
     *
     * @param baseUrl 授权中心基础 URL
     * @return 完整 URL
     * @throws IllegalArgumentException baseUrl 为空/空白
     */
    public String buildUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("授权中心基础 URL 不能为空，请配置 simple.auth.server-url");
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalized + path;
    }
}

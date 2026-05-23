package com.simple.common.auth.client.common.manager.token;

import java.util.Map;

/**
 * Token 管理接口。
 * <p>
 * 提供 Token 的创建、解析、验证等核心功能。
 * 默认实现包括 {@link com.simple.common.auth.client.manager.jwt.JJwtTokenManager}(基于 JJWT)
 * 和 {@link com.simple.common.auth.client.manager.jwt.JwtTokenManager}。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>用户登录后生成 Access Token 和 Refresh Token</li>
 *   <li>请求拦截时验证 Token 有效性</li>
 *   <li>Token 刷新时重新生成新的 Token</li>
 * </ul>
 *
 * @author qty
 */
public interface TokenManager {

    /**
     * 创建 Token
     * <p>
     * 根据头部信息和载荷生成 JWT Token。
     * 头部信息通常包含算法类型、Token类型等元数据。
     * 载荷包含用户ID、角色、权限等业务数据。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * Map<String, Object> headers = new HashMap<>();
     * headers.put("alg", "HS256");
     * headers.put("typ", "JWT");
     * 
     * Map<String, Object> payload = new HashMap<>();
     * payload.put("userId", "123");
     * payload.put("username", "张三");
     * payload.put("roles", Arrays.asList("admin", "user"));
     * 
     * String token = tokenManager.create(headers, payload);
     * }</pre>
     *
     * @param headers Token 头部信息,可为 null 使用默认值
     * @param payload Token 载荷数据,必须包含用户标识等关键信息
     * @return 生成的 JWT Token 字符串
     * @throws RuntimeException 当 Token 生成失败时抛出异常
     */
    String create(Map<String, Object> headers, Map<String, Object> payload);

    /**
     * 验证 Token 合法性并解析载荷
     * <p>
     * 验证 Token 签名、有效期等,通过后返回载荷数据。
     * 支持区分 Access Token 和 Refresh Token 的验证逻辑。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 验证 Access Token
     * Map<String, Object> payload = tokenManager.check(accessToken, false);
     * String userId = (String) payload.get("userId");
     * 
     * // 验证 Refresh Token
     * Map<String, Object> refreshPayload = tokenManager.check(refreshToken, true);
     * }</pre>
     *
     * @param token     待验证的 Token 字符串
     * @param isRefresh 是否为 Refresh Token,true 表示验证刷新 Token
     * @return Token 载荷数据Map,包含用户ID、角色等信息
     * @throws RuntimeException 当 Token 无效、过期或格式错误时抛出异常
     */
    Map<String, Object> check(String token, boolean isRefresh);

    /**
     * 创建 Token(使用默认头部)
     * <p>
     * 便捷方法,自动使用默认头部信息。
     * </p>
     *
     * @param payload Token 载荷数据
     * @return 生成的 JWT Token 字符串
     * @see #create(Map, Map)
     */
    default String create(Map<String, Object> payload) {
        return create(null, payload);
    }

    /**
     * 添加JWT签名密钥（仅本地缓存）
     * <p>
     * 仅将密钥保存到本地缓存，不涉及远程广播。
     * 如需广播到远程客户端，请使用 {@link com.simple.common.auth.client.common.broadcast.SecretBroadcaster}。
     * </p>
     *
     * @param secret JWT签名密钥
     * @throws IllegalArgumentException 当密钥为空、长度不足时抛出异常
     */
    void addSecret(String secret);

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 便捷方法，自动生成符合安全要求的随机密钥字符串。
     * </p>
     *
     * <h3>设计说明：</h3>
     * <p>
     * 此方法保留在 TokenManager 中是为了保持 API 向后兼容。
     * 在服务端应用中，密钥生成逻辑由 SecretKeyManager 统一管理，
     * 通过 EventBus 事件同步到所有客户端。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成新密钥
     * String secret = tokenManager.generateSecret();
     * 
     * // 添加并广播
     * tokenManager.addSecret(secret);
     * }</pre>
     *
     * @return 生成的随机密钥字符串（至少64位）
     */
    String generateSecret();

}

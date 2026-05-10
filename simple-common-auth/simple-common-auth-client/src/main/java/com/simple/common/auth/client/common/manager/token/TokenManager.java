package com.simple.common.auth.client.common.manager.token;

import java.util.List;
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
     * 添加JWT签名密钥（支持指定目标项目）
     * <p>
     * 用于多租户场景，服务端可以为指定的客户端项目添加密钥并广播。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // OAuth Server 为指定客户端项目添加密钥
     * String projectCode = "xiaoyue_app"; // 目标客户端的projectCode
     * String jwtSecret = unifiedSecretManager.getSecrets(projectCode).get("jwt");
     * tokenManager.addSecret(jwtSecret, projectCode, true);
     * 
     * // 或者一次广播给多个客户端
     * List<String> targetCodes = Arrays.asList("xiaoyue_web", "xiaoyue_app", "xiaoyue_admin");
     * tokenManager.addSecret(jwtSecret, targetCodes, true);
     * 
     * // 本地加载密钥（不广播）
     * tokenManager.addSecret(secret, null, false);
     * }</pre>
     *
     * @param secret           JWT签名密钥
     * @param targetProjectCode 目标项目编码（即client_name），用于标识该密钥属于哪个项目；传null表示当前项目
     * @param broadcast        是否广播事件
     * @throws IllegalArgumentException 当密钥为空、长度不足时抛出异常
     */
    void addSecret(String secret, String targetProjectCode, boolean broadcast);

    /**
     * 添加JWT签名密钥（支持指定多个目标项目）
     * <p>
     * 用于多租户场景，一次广播给多个客户端项目，避免重复发送事件。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // OAuth Server 为多个客户端项目添加密钥（一次广播）
     * List<String> targetCodes = Arrays.asList("xiaoyue_web", "xiaoyue_app", "xiaoyue_admin");
     * String jwtSecret = unifiedSecretManager.getSecrets(targetCodes.get(0)).get("jwt");
     * tokenManager.addSecret(jwtSecret, targetCodes, true);
     * }</pre>
     *
     * @param secret            JWT签名密钥
     * @param targetProjectCodes 目标项目编码集合（即client_name列表）
     * @param broadcast         是否广播事件
     * @throws IllegalArgumentException 当密钥为空、长度不足或targetProjectCodes为空时抛出异常
     */
    default void addSecret(String secret, List<String> targetProjectCodes, boolean broadcast) {
        // 默认实现：将List转换为单个projectCode调用
        String targetCode = (targetProjectCodes != null && !targetProjectCodes.isEmpty()) 
            ? targetProjectCodes.get(0) 
            : null;
        addSecret(secret, targetCode, broadcast);
    }

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

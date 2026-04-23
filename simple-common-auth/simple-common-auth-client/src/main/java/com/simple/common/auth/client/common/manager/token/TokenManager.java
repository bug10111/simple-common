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
     * 添加JWT签名密钥
     * <p>
     * 将新的JWT签名密钥添加到本地缓存，并发布事件通知所有客户端同步更新密钥。
     * 此方法用于密钥轮换(Key Rotation)场景，确保新旧密钥平滑过渡。
     * </p>
     *
     * <h3>重要提示：</h3>
     * <ul>
     *   <li><strong>定期轮换密钥</strong>：建议集成方每30天轮换一次密钥，提高系统安全性</li>
     *   <li><strong>保留旧密钥</strong>：新密钥添加后，旧密钥仍会保留一段时间以验证已签发的Token</li>
     *   <li><strong>客户端同步</strong>：调用此方法会自动通过EventBus广播事件，所有客户端会同步更新密钥</li>
     *   <li><strong>密钥强度</strong>：建议使用至少64位的强随机字符串作为密钥</li>
     * </ul>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 定时任务：每月轮换密钥
     * @Scheduled(cron = "0 0 0 1 * ?") // 每月1号零点执行
     * public void rotateJwtSecret() {
     *     // 生成新密钥
     *     String newSecret = tokenManager.generateSecret();
     *     
     *     // 添加新密钥并广播到所有客户端
     *     tokenManager.addSecret(newSecret);
     *     
     *     log.info("JWT密钥已轮换，新密钥: {}", newSecret.substring(0, 8) + "...");
     * }
     * 
     * // 或者手动触发轮换
     * public void manualRotate() {
     *     String secret = JJwtUtils.createSecret(); // 生成64位随机密钥
     *     tokenManager.addSecret(secret);
     * }
     * }</pre>
     *
     * <h3>密钥轮换最佳实践：</h3>
     * <ol>
     *   <li>生成新密钥（至少64位随机字符串）</li>
     *   <li>调用 addSecret() 添加新密钥并广播</li>
     *   <li>等待一段时间（如7天），确保所有旧Token过期</li>
     *   <li>可选：清理过期的旧密钥</li>
     * </ol>
     *
     * @param secret JWT签名密钥，建议使用强随机字符串（至少64字符）
     * @throws IllegalArgumentException 当密钥为空或长度不足时抛出异常
     * @see com.simple.common.auth.client.util.JJwtUtils#createSecret() 生成JJWT密钥
     * @see com.simple.common.auth.client.util.JwtUtils#createJWTSignerStr() 生成Hutool JWT密钥
     */
    void addSecret(String secret);

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 便捷方法，自动生成符合安全要求的随机密钥字符串。
     * 不同实现可能生成不同长度的密钥（JJWT需要64位，Hutool需要64位）。
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
     * @return 生成的随机密钥字符串
     */
    String generateSecret();

}

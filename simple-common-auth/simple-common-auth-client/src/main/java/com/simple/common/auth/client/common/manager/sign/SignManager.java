package com.simple.common.auth.client.common.manager.sign;

/**
 * 签名管理接口。
 * <p>
 * 提供接口签名生成、验证、防重放等安全功能。
 * 默认实现 {@link com.simple.common.auth.client.manager.sign.DefaultSignManager} 基于 HMAC-SHA256 算法。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>API 接口签名验证,防止请求被篡改</li>
 *   <li>防重放攻击,确保请求的唯一性</li>
 *   <li>时效性校验,防止过期请求被执行</li>
 * </ul>
 *
 * <h3>密钥管理：</h3>
 * <p>
 * 签名密钥由 {@link com.simple.common.auth.client.common.manager.secret.SignSecretManager} 统一管理，
 * 支持通过 CacheManager 配置切换 Redis/Local 缓存，应用启动时自动生成初始密钥。
 * </p>
 *
 * @author qty
 */
public interface SignManager {

    /**
     * 校验请求时效性
     * <p>
     * 检查请求时间戳是否在允许的时间窗口内,防止过期请求被执行。
     * 默认时间窗口为 5 分钟。
     * </p>
     *
     * @param timestamp 请求时间戳(毫秒)
     * @throws RuntimeException 当请求已过期时抛出异常
     */
    void checkTimestamp(String timestamp);

    /**
     * 防重放校验
     * <p>
     * 检查 nonce (随机数) 是否已被使用过,防止同一请求被重复提交。
     * nonce 会在 Redis 中缓存一定时间(通常为 5 分钟)。
     * </p>
     *
     * @param nonce 唯一标识符,通常为 UUID 或随机字符串
     * @throws RuntimeException 当 nonce 已存在(重放攻击)时抛出异常
     */
    void checkNonce(String nonce);

    /**
     * 生成 HMAC-SHA256 签名(Web 请求)
     * <p>
     * 对请求内容生成签名,用于 Web API 接口的身份验证。
     * 签名算法: HMAC-SHA256(message, secretKey)
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 客户端生成签名
     * String message = method + path + timestamp + nonce + body;
     * String signature = signManager.signWeb(message);
     * 
     * // 将签名添加到请求头
     * headers.put("X-Signature", signature);
     * }</pre>
     *
     * @param message 待签名的消息内容(通常为请求方法+路径+时间戳+随机数+请求体)
     * @return Base64 编码的签名字符串
     */
    String signWeb(String message);

    /**
     * 验证 HMAC-SHA256 签名(Web 请求)
     * <p>
     * 验证请求签名是否合法,确保请求未被篡改且来自合法客户端。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 服务端验证签名
     * String message = method + path + timestamp + nonce + body;
     * boolean valid = signManager.verifyWeb(message, requestSignature);
     * if (!valid) {
     *     throw new SecurityException("签名验证失败");
     * }
     * }</pre>
     *
     * @param message   原始消息内容(与客户端生成签名时相同)
     * @param signature 客户端传来的签名字符串
     * @return true 表示签名验证通过,false 表示验证失败
     */
    boolean verifyWeb(String message, String signature);

    /**
     * 添加签名密钥
     * <p>
     * 将新的签名密钥添加到本地缓存，并发布事件通知所有客户端同步更新密钥。
     * 此方法用于密钥轮换(Key Rotation)场景。
     * </p>
     *
     * <h3>重要提示：</h3>
     * <ul>
     *   <li><strong>定期轮换密钥</strong>：建议集成方每30天轮换一次密钥，提高系统安全性</li>
     *   <li><strong>客户端同步</strong>：调用此方法会自动通过EventBus广播事件，所有客户端会同步更新密钥</li>
     *   <li><strong>密钥强度</strong>：建议使用至少32位的强随机字符串作为密钥</li>
     * </ul>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * @Autowired
     * private SignSecretManager signSecretManager;
     * 
     * // 定时任务：每月轮换密钥
     * @Scheduled(cron = "0 0 0 1 * ?") // 每月1号零点执行
     * public void rotateSignSecret() {
     *     // 生成新密钥（内部委托给SignSecretManager）
     *     String newSecret = signManager.generateSecret();
     *     
     *     // 添加新密钥并广播到所有客户端
     *     signManager.addSecret(newSecret);
     *     
     *     log.info("签名密钥已轮换");
     * }
     * }</pre>
     *
     * @param secret 签名密钥，建议使用强随机字符串（至少32字符）
     * @throws IllegalArgumentException 当密钥为空时抛出异常
     * @see #generateSecret() 生成新密钥
     */
    void addSecret(String secret);

    /**
     * 生成新的签名密钥
     * <p>
     * 便捷方法，自动生成符合安全要求的随机密钥字符串。
     * </p>
     *
     * <h3>设计说明：</h3>
     * <p>
     * 此方法保留在 SignManager 中是为了保持 API 向后兼容。
     * 在服务端应用中，密钥生成逻辑由 SignSecretManager 统一管理，
     * 通过 EventBus 事件同步到所有客户端。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成新密钥
     * String secret = signManager.generateSecret();
     * 
     * // 添加并广播
     * signManager.addSecret(secret);
     * }</pre>
     *
     * @return 生成的随机密钥字符串（至少32位）
     */
    String generateSecret();

    /**
     * 获取当前签名密钥
     * <p>
     * 用于签名验证时获取当前有效的密钥。
     * </p>
     *
     * @return 当前签名密钥
     * @throws IllegalStateException 当密钥未初始化时抛出异常
     */
    String getKey();

}

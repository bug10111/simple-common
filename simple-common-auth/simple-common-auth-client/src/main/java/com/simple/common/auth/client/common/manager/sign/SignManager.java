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
 * 签名密钥由 {@link com.simple.common.auth.client.util.SignSecretUtils} 统一管理（静态 AtomicReference，进程内可见），
 * 应用启动时经实例注册自举加载初始密钥；密钥同步变更经授权中心投递引擎回调。
 * nonce 防重放经 CacheManager（simple.sign.cache-type 决定 Redis/Local）存储：
 * REDIS 形态集群共享 nonce 记录；nonce 记录为写入型消费（checkNonce 即记录已用 nonce），非只读校验。
 * </p>
 *
 * @author qty
 */
public interface SignManager {

    /**
     * 校验请求时效性
     * <p>
     * 检查请求时间戳是否在允许的时间窗口内,防止过期请求被执行。
     * 默认时间窗口为 5 分钟（simple.sign.default-time-window-ms）。
     * </p>
     *
     * @param timestamp 请求时间戳(毫秒),不允许为空/非数字/非正数
     * @throws com.simple.common.core.exception.DefaultException 当请求已过期时抛出异常
     * @throws IllegalArgumentException 当时间戳为空或不是合法数字/非正数时抛出
     */
    void checkTimestamp(String timestamp);

    /**
     * 防重放校验
     * <p>
     * 检查 nonce (随机数) 是否已被使用过,防止同一请求被重复提交。
     * 通过即原子记录该 nonce（写入型消费，重复出现直接拒绝）；
     * nonce 记录 TTL 严格覆盖时间戳窗口，受 simple.sign.cache-time 与时间窗口共同约束。
     * </p>
     *
     * @param nonce 唯一标识符,通常为 UUID 或随机字符串,不允许为空
     * @throws com.simple.common.core.exception.DefaultException 当 nonce 已存在(重放攻击)时抛出异常
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
     * @param signature 客户端传来的 Base64 签名字符串,为 null/空白时直接返回 false（不抛异常）
     * @return true 表示签名验证通过,false 表示验证失败或入参为空（签名为 URL-safe Base64，非法字符按失败处理）
     */
    boolean verifyWeb(String message, String signature);

    /**
     * 添加签名密钥（仅本地缓存）
     * <p>
     * 仅将密钥保存到本地缓存，不涉及远程广播。
     * 如需投递到远程实例，请使用授权中心的密钥投递链路（ProjectSecretSyncManager → SyncDeliveryProducer）。
     * </p>
     *
     * @param secret 签名密钥
     * @throws IllegalArgumentException 当密钥为空时抛出异常
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
     * // 仅添加到本地缓存（如需分发请使用授权中心的密钥投递链路）
     * signManager.addSecret(secret);
     * }</pre>
     *
     * @return 生成的随机密钥字符串（至少32位；DefaultSignManager 实现生成 32 位 UUID，
     *         客户端侧密钥以授权中心下发为准，不建议本地生成）
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

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
 * @author qty
 */
public interface SignManager {

    /**
     * 生成签名秘钥
     * <p>
     * 生成用于签名的秘钥,通常只调用一次,在应用启动时执行。
     * </p>
     */
    void generated();

    /**
     * 更新签名秘钥
     * <p>
     * 动态更新签名秘钥,用于秘钥轮换场景。
     * </p>
     *
     * @param key 新的签名秘钥
     */
    void putKey(String key);

    /**
     * 获取当前签名秘钥
     *
     * @return 当前使用的签名秘钥
     */
    String getKey();

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

}

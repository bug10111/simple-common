package com.simple.common.auth.server.common.manager.secret;

/**
 * JWT秘钥管理器接口。
 * <p>
 * 用于管理JWT令牌的签名秘钥,包括秘钥的初始化、获取、更新和验证。
 * 支持秘钥轮换(Rotation)机制,确保在秘钥更换期间旧Token仍然有效。
 * 默认实现 {@link com.simple.common.auth.server.manager.DefaultJwtSecretManager} 基于内存存储,支持多秘钥并存。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>秘钥轮换：定期更换JWT签名秘钥,提高安全性</li>
 *   <li>多秘钥支持：同时维护多个有效秘钥,平滑过渡</li>
 *   <li>秘钥验证：校验Token使用的秘钥是否仍然有效</li>
 * </ul>
 *
 * <h3>工作原理：</h3>
 * <ol>
 *   <li>系统启动时生成或加载初始秘钥</li>
 *   <li>签发Token时使用当前秘钥(current secret)</li>
 *   <li>验证Token时遍历所有有效秘钥,找到匹配的秘钥</li>
 *   <li>定期轮换秘钥,旧秘钥保留一段时间以验证旧Token</li>
 * </ol>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class RedisJwtSecretManager implements JwtSecretManager {
 *     @Autowired
 *     private RedisTemplate<String, String> redisTemplate;
 *     
 *     private static final String SECRET_KEY = "jwt:current:secret";
 *     private static final String SECRETS_SET = "jwt:secrets";
 *     
 *     @Override
 *     public void addSecret(String secret) {
 *         // 添加新秘钥到集合
 *         redisTemplate.opsForSet().add(SECRETS_SET, secret);
 *         // 设置为当前秘钥
 *         redisTemplate.opsForValue().set(SECRET_KEY, secret);
 *     }
 *     
 *     @Override
 *     public boolean existsSecret(String secret) {
 *         return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(SECRETS_SET, secret));
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface JwtSecretManager {

    /**
     * 添加新秘钥
     * <p>
     * 将新秘钥添加到有效秘钥集合中,并设置为当前秘钥。
     * 旧秘钥仍然保留在集合中,用于验证已签发的Token。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成新秘钥
     * String newSecret = UUID.randomUUID().toString();
     * 
     * // 添加新秘钥(旧秘钥仍然有效)
     * jwtSecretManager.addSecret(newSecret);
     * 
     * // 后续签发的Token将使用新秘钥
     * String token = tokenManager.create(payload);
     * }</pre>
     *
     * @param secret 新的JWT签名秘钥,建议使用强随机字符串(至少32字符)
     */
    void addSecret(String secret);

    /**
     * 更新秘钥(替换指定秘钥)
     * <p>
     * 将指定的旧秘钥替换为新秘钥。
     * 适用于需要精确控制哪个秘钥被替换的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 替换特定的旧秘钥
     * String oldSecret = "old-secret-key";
     * String newSecret = UUID.randomUUID().toString();
     * jwtSecretManager.updateSecret(oldSecret, newSecret);
     * }</pre>
     *
     * @param oldSecret 待替换的旧秘钥
     * @param newSecret 新的JWT签名秘钥
     * @throws RuntimeException 当旧秘钥不存在时抛出异常
     */
    void updateSecret(String oldSecret, String newSecret);

    /**
     * 获取当前秘钥
     * <p>
     * 获取用于签发新Token的当前秘钥。
     * 该秘钥通常是最新添加的秘钥。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 手动签发Token时使用
     * String currentSecret = jwtSecretManager.getCurrentSecret();
     * String token = Jwts.builder()
     *     .setClaims(claims)
     *     .signWith(SignatureAlgorithm.HS256, currentSecret)
     *     .compact();
     * }</pre>
     *
     * @return 当前用于签发Token的秘钥
     * @throws IllegalStateException 当没有配置任何秘钥时抛出异常
     */
    String getCurrentSecret();

    /**
     * 验证秘钥是否存在
     * <p>
     * 检查指定秘钥是否在有效秘钥集合中。
     * 用于验证Token签名时,判断该Token使用的秘钥是否仍然有效。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 验证Token使用的秘钥是否有效
     * String tokenSecret = extractSecretFromToken(token);
     * if (!jwtSecretManager.existsSecret(tokenSecret)) {
     *     throw new SecurityException("Token使用的秘钥已失效");
     * }
     * }</pre>
     *
     * @param secret 待验证的秘钥
     * @return true 表示秘钥有效,false 表示秘钥已移除或从未添加
     */
    boolean existsSecret(String secret);
}
package com.simple.common.auth.server.common.manager.secret;

/**
 * JWT签名密钥管理器（服务端）
 * <p>
 * 负责JWT签名密钥的生成、加载和管理。
 * 集成方可以通过实现此接口，自定义密钥来源（如从数据库、配置中心加载）。
 * </p>
 *
 * <h3>设计初衷：</h3>
 * <ul>
 *   <li><strong>服务端集中管理</strong>：密钥由服务端统一生成/加载，通过事件同步到客户端</li>
 *   <li><strong>支持自定义实现</strong>：集成方可以重写默认方案，从数据库、配置中心等加载密钥</li>
 *   <li><strong>密钥轮换支持</strong>：提供标准的密钥生成和加载接口</li>
 * </ul>
 *
 * <h3>架构说明：</h3>
 * <pre>
 * ┌─────────────┐         EventBus          ┌─────────────┐
 * │   Server    │ ───────────────────────►  │   Client    │
 * │             │   SecretEvent(密钥更新)    │             │
 * │ SecretKey   │                           │  缓存密钥    │
 * │  Manager    │                           │             │
 * └─────────────┘                           └─────────────┘
 * </pre>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 默认实现：随机生成密钥
 * @Autowired
 * private SecretKeyManager secretKeyManager;
 * 
 * String secret = secretKeyManager.generate();
 * 
 * // 自定义实现：从数据库加载密钥
 * @Component
 * public class DatabaseSecretKeyManager implements SecretKeyManager {
 *     @Autowired
 *     private SecretKeyMapper secretKeyMapper;
 *     
 *     @Override
 *     public String generate() {
 *         // 从数据库查询当前有效的密钥
 *         SecretKeyEntity entity = secretKeyMapper.selectCurrentKey();
 *         return entity.getSecretValue();
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface SecretKeyManager {

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 生成符合安全要求的随机密钥字符串，或从持久化存储加载密钥。
     * 不同实现可能采用不同的生成策略（随机生成、从数据库加载等）。
     * </p>
     *
     * <h3>默认实现：</h3>
     * <p>生成64位随机字符串，适用于JJWT和Hutool JWT</p>
     *
     * <h3>自定义实现示例：</h3>
     * <pre>{@code
     * @Override
     * public String generate() {
     *     // 从数据库加载当前密钥
     *     SecretKey key = secretKeyRepository.findCurrentKey();
     *     if (key == null) {
     *         // 如果不存在，生成新密钥并保存
     *         key = new SecretKey();
     *         key.setSecretValue(RandomStringUtils.randomAlphanumeric(64));
     *         key.setCreateTime(LocalDateTime.now());
     *         secretKeyRepository.save(key);
     *     }
     *     return key.getSecretValue();
     * }
     * }</pre>
     *
     * @return 生成的JWT签名密钥字符串，建议至少64位
     * @throws RuntimeException 当密钥生成失败时抛出异常
     */
    String generate();
}

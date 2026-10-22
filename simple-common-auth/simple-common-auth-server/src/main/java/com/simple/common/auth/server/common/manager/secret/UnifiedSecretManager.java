package com.simple.common.auth.server.common.manager.secret;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * 统一密钥管理器（服务端）。
 * <p>
 * 负责根据项目编码（spring.application.name）获取JWT签名密钥和API签名密钥。
 * 集成方可以通过实现此接口，自定义密钥来源（如从数据库、配置中心加载）。
 * </p>
 *
 * <h3>架构说明：</h3>
 * <pre>
 * ┌─────────────┐         EventBus          ┌─────────────┐
 * │   Server    │ ───────────────────────►  │   Client    │
 * │             │  变更投递引擎(HTTP回调)   │             │
 * │ SecretKey   │                           │  缓存密钥    │
 * │  Manager    │                           │             │
 * └─────────────┘                           └─────────────┘
 * </pre>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 默认实现：CSPRNG 随机生成密钥
 * @Autowired
 * private UnifiedSecretManager secretManager;
 *
 * Map<String, String> secrets = secretManager.getSecrets("my-project");
 * String jwtSecret = secrets.get(UnifiedSecretManager.KEY_JWT);
 * String signSecret = secrets.get(UnifiedSecretManager.KEY_SIGN);
 *
 * // 自定义实现：从数据库加载密钥
 * @Component
 * public class DatabaseUnifiedSecretManager implements UnifiedSecretManager {
 *     @Autowired
 *     private ProjectClientRepository projectClientRepository;
 *
 *     @Override
 *     public Map<String, String> getSecrets(String projectCode) {
 *         // 从数据库查询项目客户端信息
 *         ProjectClient client = projectClientRepository.findByProjectCode(projectCode);
 *         if (client == null) {
 *             // 不存在则生成新密钥并持久化（密钥必须来自 CSPRNG，禁止 java.util.Random/
 *             // RandomStringUtils/RandomUtil 等非加密随机源——输出可预测即可被伪造签名）
 *             SecureRandom random = new SecureRandom();
 *             byte[] jwtBytes = new byte[48];
 *             random.nextBytes(jwtBytes);
 *             client = new ProjectClient();
 *             client.setProjectCode(projectCode);
 *             client.setJwtSecret(Base64.getUrlEncoder().withoutPadding().encodeToString(jwtBytes));
 *             client.setSignSecret(generateSignSecret());
 *             repository.save(client);
 *         }
 *
 *         Map<String, String> secrets = new HashMap<>();
 *         secrets.put(KEY_JWT, client.getJwtSecret());
 *         secrets.put(KEY_SIGN, client.getSignSecret());
 *         return secrets;
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface UnifiedSecretManager {

    /**
     * 密钥 Map 中 JWT 密钥的固定键（实现契约的常量事实源，禁止各实现自造字面量）
     */
    String KEY_JWT = "jwt";

    /**
     * 密钥 Map 中 SIGN 密钥的固定键
     */
    String KEY_SIGN = "sign";

    /**
     * 根据项目编码获取密钥
     * <p>
     * 返回包含JWT密钥和SIGN密钥的Map（恒含 {@link #KEY_JWT} 与 {@link #KEY_SIGN} 两个非空键）。
     * 如果项目不存在，实现必须自动生成默认密钥；同一项目编码在实现生命周期内的多次调用应返回
     * 同一份密钥——禁止在临时性故障（缓存/网络抖动）时重新生成密钥，否则客户端缓存的密钥
     * 将与服务端漂移，造成大面积鉴权失败。
     * </p>
     *
     * <h3>返回值格式：</h3>
     * <pre>{@code
     * {
     *   "jwt":  "64字符随机字符串（≥384位熵，SecureRandom生成）...",
     *   "sign": "32字符随机字符串（≥192位熵，SecureRandom生成）..."
     * }
     * }</pre>
     *
     * <h3>自定义实现示例：</h3>
     * <pre>{@code
     * @Override
     * public Map<String, String> getSecrets(String projectCode) {
     *     // 从数据库加载密钥（生成密钥必须使用 CSPRNG，见类示例）
     *     ProjectClient client = repository.findByProjectCode(projectCode);
     *     if (client == null) {
     *         client = new ProjectClient();
     *         client.setProjectCode(projectCode);
     *         client.setJwtSecret(generateJwtSecret());
     *         client.setSignSecret(generateSignSecret());
     *         repository.save(client);
     *     }
     *
     *     Map<String, String> secrets = new HashMap<>();
     *     secrets.put(KEY_JWT, client.getJwtSecret());
     *     secrets.put(KEY_SIGN, client.getSignSecret());
     *     return secrets;
     * }
     * }</pre>
     *
     * @param projectCode 项目编码（spring.application.name），不能为 null 或空白，
     *                    否则实现必须抛出 {@link IllegalArgumentException}——
     *                    禁止将空编码归一为共享默认项目，那会击穿项目间密钥隔离
     * @return 包含 {@link #KEY_JWT} 与 {@link #KEY_SIGN} 两个key的Map（两键恒存在且非空白）
     * @throws IllegalArgumentException projectCode 为 null/空白时抛出
     * @throws IllegalStateException    密钥源不可用（配置缺失/存储故障等，属可诊断错误而非静默换钥场景）
     */
    Map<String, String> getSecrets(String projectCode);

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 便捷方法，用于单独生成JWT密钥。
     * 强度要求（硬契约）：必须由加密安全随机源（{@link SecureRandom} 或等价 CSPRNG）产生，
     * 熵不低于 384 位（如 48 字节随机数经 Base64 编码为 64 字符）；
     * 禁止 java.util.Random / RandomStringUtils / RandomUtil 等非加密随机源。
     * </p>
     *
     * @return 生成的JWT签名密钥字符串（至少64字符，≥384位熵）
     */
    String generateJwtSecret();

    /**
     * 生成新的API签名密钥
     * <p>
     * 便捷方法，用于单独生成SIGN密钥。
     * 强度要求（硬契约）：必须由加密安全随机源产生，熵不低于 192 位
     * （如 24 字节随机数经 Base64 编码为 32 字符）；随机源限制同 {@link #generateJwtSecret()}。
     * </p>
     *
     * @return 生成的API签名密钥字符串（至少32字符，≥192位熵）
     */
    String generateSignSecret();
}

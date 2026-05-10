package com.simple.common.auth.server.common.manager.secret;

import java.util.Map;

/**
 * 统一密钥管理器（服务端）
 * <p>
 * 负责根据项目编码（spring.application.name）获取JWT签名密钥和API签名密钥。
 * 集成方可以通过实现此接口，自定义密钥来源（如从数据库、配置中心加载）。
 * </p>
 *
 * <h3>设计初衷：</h3>
 * <ul>
 *   <li><strong>统一管理</strong>：一个接口管理JWT和SIGN两种密钥</li>
 *   <li><strong>多项目支持</strong>：根据projectCode区分不同项目的密钥</li>
 *   <li><strong>支持自定义实现</strong>：集成方可以重写默认方案，从数据库等加载密钥</li>
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
 * private UnifiedSecretManager secretManager;
 * 
 * Map<String, String> secrets = secretManager.getSecrets("my-project");
 * String jwtSecret = secrets.get("jwt");
 * String signSecret = secrets.get("sign");
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
 *             // 如果不存在，创建默认客户端并生成密钥
 *             client = createDefaultClient(projectCode);
 *         }
 *         
 *         Map<String, String> secrets = new HashMap<>();
 *         secrets.put("jwt", client.getJwtSecret());
 *         secrets.put("sign", client.getSignSecret());
 *         return secrets;
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface UnifiedSecretManager {

    /**
     * 根据项目编码获取密钥
     * <p>
     * 返回包含JWT密钥和SIGN密钥的Map。
     * 如果项目不存在，应该自动生成默认密钥。
     * </p>
     *
     * <h3>返回值格式：</h3>
     * <pre>{@code
     * {
     *   "jwt": "64位随机字符串...",
     *   "sign": "32位随机字符串..."
     * }
     * }</pre>
     *
     * <h3>自定义实现示例：</h3>
     * <pre>{@code
     * @Override
     * public Map<String, String> getSecrets(String projectCode) {
     *     // 从数据库加载密钥
     *     ProjectClient client = repository.findByProjectCode(projectCode);
     *     if (client == null) {
     *         // 生成新密钥并保存
     *         client = new ProjectClient();
     *         client.setProjectCode(projectCode);
     *         client.setJwtSecret(RandomStringUtils.randomAlphanumeric(64));
     *         client.setSignSecret(RandomStringUtils.randomAlphanumeric(32));
     *         repository.save(client);
     *     }
     *     
     *     Map<String, String> secrets = new HashMap<>();
     *     secrets.put("jwt", client.getJwtSecret());
     *     secrets.put("sign", client.getSignSecret());
     *     return secrets;
     * }
     * }</pre>
     *
     * @param projectCode 项目编码（spring.application.name）
     * @return 包含"jwt"和"sign"两个key的Map
     * @throws RuntimeException 当密钥获取失败时抛出异常
     */
    Map<String, String> getSecrets(String projectCode);

    /**
     * 生成新的JWT签名密钥
     * <p>
     * 便捷方法，用于单独生成JWT密钥。
     * </p>
     *
     * @return 生成的JWT签名密钥字符串，建议至少64位
     */
    String generateJwtSecret();

    /**
     * 生成新的API签名密钥
     * <p>
     * 便捷方法，用于单独生成SIGN密钥。
     * </p>
     *
     * @return 生成的API签名密钥字符串，建议至少32位
     */
    String generateSignSecret();
}

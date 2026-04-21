package com.simple.common.auth.server.common.service.client;

import com.simple.common.auth.server.common.entity.ClientDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * 客户端详情服务接口。
 * <p>
 * 提供OAuth客户端的认证和详情获取功能,包括从请求头中解析客户端信息、生成客户端Token等。
 * 默认抽象实现 {@link com.simple.common.auth.server.service.client.AbsClientDetailsService} 提供了
 * 客户端认证的通用流程框架,具体实现需继承并重写 {@code getClientDetailsFromDB} 方法。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>客户端身份验证：验证 clientId 和 clientSecret 是否合法</li>
 *   <li>客户端配置加载：从数据库或缓存中加载客户端权限配置</li>
 *   <li>Token 生成：为客户端生成访问 Token</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Service
 * public class MyClientDetailsService extends AbsClientDetailsService {
 *     @Autowired
 *     private ClientRepository clientRepository;
 *     
 *     @Override
 *     protected ClientDetails getClientDetailsFromDB(String clientId) {
 *         // 从数据库加载客户端配置
 *         SysClientDetails entity = clientRepository.findByClientId(clientId);
 *         AssertUtils.isTrue(entity != null, "客户端不存在");
 *         return convertToClientDetails(entity);
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface ClientDetailsService {

    /**
     * 从 Authorization 头中获取客户端详情
     * <p>
     * 解析请求头中的客户端凭证,验证合法性后返回客户端详细信息。
     * 支持 Basic 认证格式(如 "Basic base64(clientId:clientSecret)")。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 从请求头中获取客户端信息
     * String authHeader = request.getHeader("Authorization");
     * ClientDetails clientDetails = clientDetailsService.getClientDetails(authHeader);
     * 
     * // 使用客户端信息
     * String clientId = clientDetails.getClientId();
     * List<String> scopes = clientDetails.getScopes();
     * }</pre>
     *
     * @param header Authorization 请求头字符串
     * @return 客户端详情信息,包含 clientId、scopes、authorities 等配置
     * @throws RuntimeException 当客户端不存在、凭证错误或格式不合法时抛出异常
     */
    ClientDetails getClientDetails(String header);

    /**
     * 生成客户端访问 Token
     * <p>
     * 根据客户端ID和密钥生成用于访问资源的 Token。
     * 该 Token 可用于后续的资源访问认证。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成客户端 Token
     * String clientToken = clientDetailsService.getClientToken("my-client", "secret123");
     * 
     * // 用于后续请求
     * headers.put("X-Client-Token", clientToken);
     * }</pre>
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 生成的客户端访问 Token
     * @throws RuntimeException 当客户端凭证无效时抛出异常
     */
    String getClientToken(String clientId, String clientSecret);

    /**
     * 从 HttpServletRequest 中获取客户端详情
     * <p>
     * 便捷方法,自动从请求头中提取 Authorization 并调用 {@link #getClientDetails(String)}。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 客户端详情信息
     * @see #getClientDetails(String)
     */
    default ClientDetails getClientDetails(HttpServletRequest request) {
        // 获取请求头中的客户端信息
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return this.getClientDetails(header);
    }
}
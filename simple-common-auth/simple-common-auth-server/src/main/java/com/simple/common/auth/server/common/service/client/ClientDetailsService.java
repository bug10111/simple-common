package com.simple.common.auth.server.common.service.client;

import com.simple.common.auth.server.common.entity.ClientDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * 客户端详情服务接口。
 * <p>
 * 提供OAuth客户端的认证和详情获取功能。
 * 默认抽象实现 {@link com.simple.common.auth.server.service.client.AbsClientDetailsService} 提供了
 * 客户端认证的通用流程框架，具体实现需继承并重写 {@code getClientDetailsFromDB} 方法。
 * </p>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Service
 * public class MyClientDetailsService extends AbsClientDetailsService {
 *     @Override
 *     protected ClientDetails getClientDetailsFromDB(String clientId) {
 *         // 从数据库加载客户端配置
 *         SysClientDetails entity = clientRepository.findByClientId(clientId);
 *         return convertToClientDetails(entity);
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface ClientDetailsService {

    /**
     * 获取客户端信息
     *
     * @param header
     * @return
     */
    ClientDetails getClientDetails(String header);

    /**
     * 获取客户端token
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     */
    String getClientToken(String clientId, String clientSecret);

    /**
     * 获取客户端信息
     */
    default ClientDetails getClientDetails(HttpServletRequest request) {
        // 获取请求头中的客户端信息
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return this.getClientDetails(header);
    }
}
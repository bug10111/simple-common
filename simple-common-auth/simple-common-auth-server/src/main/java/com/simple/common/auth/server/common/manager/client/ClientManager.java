package com.simple.common.auth.server.common.manager.client;

import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.enums.ClientAttribute;

import java.util.Map;

/**
 * 客户端管理器接口。
 * <p>
 * 用于管理OAuth客户端信息的加密和解密,包括客户端Token的生成和解析。
 * 默认实现 {@link com.simple.common.auth.server.manager.DefaultClientManager} 基于 Base64 编码。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>客户端身份认证：将 clientId 和 clientSecret 编码为 Token</li>
 *   <li>请求头解析：从 Authorization 头中解码客户端信息</li>
 *   <li>安全传输：避免明文传输客户端凭证</li>
 * </ul>
 *
 * @author qty
 */
public interface ClientManager {

    /**
     * 加密客户端凭证为 Token
     * <p>
     * 将 clientId 和 clientSecret 组合并编码为 Token 字符串。
     * 通常用于生成 Authorization 头的值。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 生成客户端 Token
     * String token = clientManager.encrypt("my-client", "secret123");
     * 
     * // 添加到请求头
     * headers.put("Authorization", "Basic " + token);
     * }</pre>
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 编码后的客户端 Token 字符串
     */
    String encrypt(String clientId, String clientSecret);

    /**
     * 解密客户端 Token 获取凭证信息
     * <p>
     * 从 Authorization 头中解析出 clientId 和 clientSecret。
     * 支持 Basic 认证格式的解析(如 "Basic base64(clientId:clientSecret)")。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 从请求头中解析客户端信息
     * String authHeader = request.getHeader("Authorization");
     * Map<ClientAttribute, String> credentials = clientManager.decryptStr(authHeader);
     * 
     * String clientId = credentials.get(ClientAttribute.CLIENT_ID);
     * String clientSecret = credentials.get(ClientAttribute.CLIENT_SECRET);
     * }</pre>
     *
     * @param header Authorization 请求头的完整字符串
     * @return 包含 CLIENT_ID 和 CLIENT_SECRET 的Map
     * @throws RuntimeException 当 Token 格式错误或解码失败时抛出异常
     */
    Map<ClientAttribute, String> decryptStr(String header);

}
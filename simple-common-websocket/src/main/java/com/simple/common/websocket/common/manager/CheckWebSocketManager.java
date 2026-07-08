package com.simple.common.websocket.common.manager;

/**
 * WebSocket鉴权校验管理器接口。
 * <p>
 * 用于在WebSocket连接建立时进行身份验证和权限校验。
 * 默认实现 {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager} 不做任何校验,
 * 所有连接都会通过认证。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 如需启用鉴权(如基于Token的身份验证),请继承 
 * {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager} 并重写 {@link #checkToken} 方法。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>用户身份验证：验证WebSocket连接的Token是否有效</li>
 *   <li>权限校验：检查用户是否有权限访问特定的WebSocket通道</li>
 *   <li>客户端类型识别：区分Web端、移动端等不同客户端类型</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Component
 * public class MyCheckWebSocketManager extends DefaultCheckWebSocketManager {
 *     @Autowired
 *     private TokenManager tokenManager;
 *     
 *     @Override
 *     public boolean checkToken(String token, String type, String cliKey) {
 *         // 验证Token有效性
 *         try {
 *             Map<String, Object> payload = tokenManager.check(token, false);
 *             return payload != null && !payload.isEmpty();
 *         } catch (Exception e) {
 *             log.error("WebSocket Token验证失败", e);
 *             return false;
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
public interface CheckWebSocketManager {

    /**
     * WebSocket握手认证校验
     * <p>
     * 在WebSocket连接建立时调用,验证客户端提供的Token是否合法。
     * 如果返回false,连接将被拒绝。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 客户端连接时传递Token
     * ws://localhost:8080/ws?token=xxx&type=web&cliKey=client123
     * 
     * // 服务端验证
     * boolean valid = checkWebSocketManager.checkToken(token, type, cliKey);
     * if (!valid) {
     *     // 拒绝连接
     *     ctx.close();
     * }
     * }</pre>
     *
     * @param token  认证Token字符串,通常为JWT Token
     * @param type   客户端类型,如 "web"、"app"、"mini-program" 等
     * @param cliKey 客户端唯一标识,用于区分不同的客户端实例
     * @return true 表示认证通过,允许连接;false 表示认证失败,拒绝连接
     */
    boolean checkToken(String token, String type, String cliKey);


}
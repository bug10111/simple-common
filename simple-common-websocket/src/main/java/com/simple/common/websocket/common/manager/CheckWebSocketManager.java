package com.simple.common.websocket.common.manager;

/**
 * WebSocket鉴权校验管理器接口。
 * <p>
 * 用于在WebSocket连接建立时进行身份验证和权限校验。
 * 默认实现 {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager}
 * 为默认安全（fail-closed）模式：未提供业务实现时拒绝所有连接；
 * 仅当显式配置 {@code simple.websocket.auth.permissive=true} 时，
 * 默认实现才放行携带非空 token 的连接（空 token 仍然拒绝）。
 * </p>
 * <p>
 * <b>安全提示：生产环境必须提供真实校验实现</b>，禁止依赖默认实现或宽松模式上线。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 如需启用鉴权(如基于Token的身份验证)，请继承
 * {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager} 并重写 {@link #checkToken} 方法，
 * 并注册为 Spring Bean（如 {@code @Component}）。
 * 默认实现以 {@code @ConditionalOnMissingBean} 注册，业务 Bean 会自动替换默认实现，
 * <b>无需</b> {@code @Primary}，也不会出现双 Bean 冲突。
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
 * @Slf4j  // 注意：本示例使用log，子类需自行添加@Slf4j或声明自己的logger
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
     * WebSocket握手认证校验。
     * <p>
     * 在WebSocket连接建立时调用,验证客户端提供的Token是否合法。
     * 返回false时连接将被拒绝（握手以 HTTP 401 失败）。
     * </p>
     *
     * <h3>实现契约：</h3>
     * <ul>
     *   <li>token 可能为 null：客户端未携带 token 查询参数时由认证处理器传入 null，
     *       实现必须先判空再校验，不得假设参数已被预校验</li>
     *   <li>type / cliKey 为客户端握手参数，未经框架语义校验，实现自行决定如何使用</li>
     *   <li>实现抛出异常时按认证失败处理：异常沿管线传播至 exceptionCaught 并关闭连接（fail-closed），
     *       不会被视为校验通过</li>
     * </ul>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 客户端连接时传递Token
     * ws://localhost:8080/simple?token=xxx&type=web&cliKey=client123
     *
     * // 服务端验证
     * boolean valid = checkWebSocketManager.checkToken(token, type, cliKey);
     * if (!valid) {
     *     // 拒绝连接
     *     ctx.close();
     * }
     * }</pre>
     *
     * @param token  认证Token字符串,通常为JWT Token;客户端未携带时为 null
     * @param type   客户端类型,如 "web"、"app"、"mini-program" 等
     * @param cliKey 客户端唯一标识,用于区分不同的客户端实例
     * @return true 表示认证通过,允许连接;false 表示认证失败,拒绝连接
     */
    boolean checkToken(String token, String type, String cliKey);

}

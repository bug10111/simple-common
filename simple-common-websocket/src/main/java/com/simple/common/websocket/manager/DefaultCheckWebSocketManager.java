package com.simple.common.websocket.manager;

import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket鉴权校验管理器默认实现。
 * <p>
 * 默认安全（fail-closed）：未接入真实鉴权实现时拒绝所有连接，
 * 避免"默认放行"造成的未认证接入。
 * </p>
 * <p>
 * 接入方式：继承本类（或直接实现 {@link CheckWebSocketManager}）并注册为 Spring Bean
 * （如 {@code @Component}），默认实现经 {@code @ConditionalOnMissingBean} 自动让位，
 * 无需 {@code @Primary}。确需放弃鉴权的内部/开发环境，显式配置
 * {@code simple.websocket.auth.permissive=true}，此时携带非空 token 的连接放行
 * （空 token 仍然拒绝）。
 * </p>
 *
 * @author qty
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultCheckWebSocketManager implements CheckWebSocketManager {

    private final WebSocketProperties webSocketProperties;

    /**
     * 宽松模式一次性告警标记：告警只在首个连接时发出一次，避免每个握手都刷日志
     */
    private final AtomicBoolean permissiveWarned = new AtomicBoolean(false);

    /**
     * 启动时一次性输出当前鉴权模式，便于运维识别部署是否处于无鉴权状态
     */
    @PostConstruct
    public void notifyAuthMode() {
        if (webSocketProperties.getAuth().isPermissive()) {
            log.warn("WebSocket鉴权处于宽松模式（simple.websocket.auth.permissive=true），非空token即放行，仅限内部/开发环境使用！");
        } else {
            log.info("WebSocket鉴权为默认安全模式（fail-closed）：未提供真实CheckWebSocketManager实现前，所有连接将被拒绝");
        }
    }

    /**
     * WebSocket握手认证校验（默认实现）。
     * <p>
     * 语义：
     * 1. token 为 null/空白时一律拒绝——客户端未携带凭据即无身份，任何模式下都不存在"校验通过"；
     * 2. 默认模式（fail-closed）拒绝所有连接，直到接入真实实现或显式开启宽松模式；
     * 3. 宽松模式下非空 token 放行（一次性告警提示未做真实校验）。
     * </p>
     * <p>
     * 注意：token 参数可能为 null（客户端未携带 token 查询参数时由 {@code WebSocketAuthHandler} 传入 null），
     * 覆盖实现必须先判空再校验；实现抛出异常时按失败处理（异常沿 pipeline 传播至 exceptionCaught 关闭连接，fail-closed）。
     * </p>
     */
    @Override
    public boolean checkToken(String token, String type, String cliKey) {
        // 无凭据即拒绝：不存在"空 token 视为已认证"
        if (token == null || token.isBlank()) {
            log.warn("拒绝无token的WebSocket连接 [type={}]", type);
            return false;
        }
        if (!webSocketProperties.getAuth().isPermissive()) {
            // 默认安全：未接入真实鉴权前拒绝所有连接
            log.warn("未接入真实鉴权实现，拒绝WebSocket连接。请实现CheckWebSocketManager覆盖默认实现，或显式配置 simple.websocket.auth.permissive=true 放弃鉴权 [type={}]", type);
            return false;
        }
        if (permissiveWarned.compareAndSet(false, true)) {
            log.warn("WebSocket鉴权宽松模式：token未做真实校验即放行，仅适用于内部/开发环境 [type={}]", type);
        }
        log.debug("WebSocket宽松鉴权放行 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
        return true;
    }
}

package com.simple.common.websocket.manager;

import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket鉴权校验管理器默认实现。
 * <p>
 * 默认不做任何校验，允许所有WebSocket连接建立。
 * 如需自定义鉴权逻辑（如校验token、用户权限等），请继承此类并重写 {@link #checkToken} 方法。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultCheckWebSocketManager implements CheckWebSocketManager {
    @Override
    public boolean checkToken(String token, String type, String cliKey) {
        log.warn("当前token校验默认通过，如需自定义，请重写DefaultCheckWebSocketManager-checkToken方法！");
        return true;
    }
}
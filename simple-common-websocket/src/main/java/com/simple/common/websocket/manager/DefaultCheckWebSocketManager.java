package com.simple.common.websocket.manager;

import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA
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

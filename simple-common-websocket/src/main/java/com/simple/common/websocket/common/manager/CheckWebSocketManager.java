package com.simple.common.websocket.common.manager;

/**
 * Created with IntelliJ IDEA
 * Description: websocket 校验接口
 *
 * @author 兄台丶请冷静
 */
public interface CheckWebSocketManager {

    /**
     * websocket 握手校验
     *
     * @param token token
     * @return 客户端标志
     */
    String check(String token);

}

package com.simple.common.websocket.common.manager;

/**
 * WebSocket认证校验接口
 * <p>
 * 实现此接口以自定义Token校验逻辑
 *
 * @author qty
 */
public interface CheckWebSocketManager {

    /**
     * WebSocket握手认证校验
     *
     * @param token  认证Token
     * @param type   客户端类型
     * @param cliKey 客户端标识
     * @return 认证结果，true表示认证通过
     */
    boolean checkToken(String token, String type, String cliKey);

    /**
 * websocket 握手校验
 *
 * @param token token
 * @return 客户端标志
     * @deprecated 请使用 {@link #checkToken(String, String, String)} 方法
     */
    @Deprecated
    default String check(String token) {
        return null;
    }

}

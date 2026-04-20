package com.simple.common.websocket.common.manager;

import com.simple.common.websocket.common.entity.WebSocketRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * WebSocket鉴权校验管理器接口。
 * <p>
 * 用于在WebSocket连接建立时进行身份验证和权限校验。
 * 默认实现 {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager} 不做任何校验，
 * 如需自定义鉴权逻辑，请继承 {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager} 并重写 {@link #check} 方法。
 * </p>
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


}
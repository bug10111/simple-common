package com.simple.common.websocket.common.manager;

import com.simple.common.websocket.common.entity.WebSocketRequest;
import io.netty.channel.ChannelHandlerContext;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * WebSocket消息监听管理器接口。
 * <p>
 * 用于管理WebSocket消息的监听注册和分发。
 * 配合 {@link com.simple.common.websocket.common.annotation.WebSocketListening} 注解使用，
 * 实现消息的自动监听和处理。
 * </p>
 *
 * @author qty
 */
public interface WebSocketListeningManager {

    /**
     * 注册监听器方法
     *
     * @param type   消息类型
     * @param cliKey 客户端标识
     * @param bean   目标Bean
     * @param method 目标方法
     */
    void registerMethod(String type, String cliKey, Object bean, Method method);

    /**
     * 调用监听器方法
     *
     * @param type    消息类型
     * @param cliKey  客户端标识
     * @param request WebSocket请求对象
     * @return 调用结果，如果监听器不存在返回Optional.empty()
     */
    Optional<Object> invoke(String type, String cliKey, WebSocketRequest request);
}
package com.simple.common.websocket.common.manager;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * WebSocket监听器管理器接口
 * <p>
 * 负责注册和调用WebSocket消息监听器
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
     * @param type   消息类型
     * @param cliKey 客户端标识
     * @param msg    消息内容
     * @return 调用结果，如果监听器不存在返回Optional.empty()
     */
    Optional<Object> invoke(String type, String cliKey, String msg);
}
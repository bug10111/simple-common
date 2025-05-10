package com.simple.common.websocket.common.manager;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 * Description: socket监听器注册、执行器
 *
 * @author qty
 */
public interface WebSocketListeningManager {

    /**
     * 注册
     *
     * @param type   消息类型
     * @param cliKey 消息客户端
     * @param bean   当前bean
     * @param method 对应方法
     */
    void registerMethod(String type, String cliKey, Object bean, Method method);

    /**
     * 执行
     *
     * @param type   消息类型
     * @param cliKey 消息客户端
     * @param msg    消息内容
     * @return 回复内容
     */
    Optional<Object> invoke(String type, String cliKey, String msg);

}

package com.simple.common.eventbus.common.manager;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA
 * Description: 事件管理接口
 *
 * @author qty
 */
public interface EventHandlerManager {

    /**
     * 注册事件
     *
     * @param method 事件方法
     */
    void register(Method method);

    /**
     * 取消注册的事件
     *
     * @param method 事件方法
     */
    void unregister(Method method);

    /**
     * 执行事件方法
     *
     * @param event 事件
     */
    void handler(Object event);

}

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
     * 注册事件（通过方法对象）
     * <p>已废弃，建议使用 {@link #register(String, Method)} 以确保 Bean 实例准确获取</p>
     *
     * @param method 事件方法
     * @deprecated 该方法通过 Class 查找 Bean 可能存在歧义，请使用带 beanName 的重载方法
     */
    @Deprecated
    void register(Method method);

    /**
     * 注册事件（推荐使用，精确指定 beanName）
     *
     * @param beanName Spring 容器中的 bean 名称
     * @param method   事件方法
     */
    void register(String beanName, Method method);

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
package com.simple.common.eventbus.common.manager;

import java.lang.reflect.Method;

/**
 * 事件处理器管理器接口。
 * <p>
 * 用于管理事件处理器的注册、注销和事件分发。
 * 默认实现 {@link com.simple.common.eventbus.manager.DefaultEventHandlerManager} 维护了
 * 事件类型与处理器方法的映射关系，支持事件的精准分发。
 * </p>
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
     * 注册事件处理器方法。
     * <p>
     * 将带有 {@link com.simple.common.eventbus.common.annotation.EventHandler} 注解的方法
     * 注册到事件处理器映射表中。
     * </p>
     *
     * @param beanName Spring 容器中的 bean 名称
     * @param method 事件处理器方法，需带有EventHandler注解
     */
    void register(String beanName, Method method);

    /**
     * 取消注册事件处理器方法。
     * <p>
     * 从事件处理器映射表中移除指定的方法，使其不再响应事件。
     * </p>
     *
     * @param method 需要取消注册的事件处理器方法
     */
    void unregister(Method method);

    /**
     * 分发并执行事件处理器。
     * <p>
     * 根据事件类型找到对应的处理器方法并执行，支持同步和异步两种执行模式。
     * </p>
     *
     * @param event 事件对象，需带有 {@link com.simple.common.eventbus.common.annotation.Event} 注解
     */
    void handler(Object event);

}
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
 * <h3>并发契约（实现方必须遵守）：</h3>
 * <ul>
 *   <li>实现内部持有共享处理器状态（如处理器映射表），{@link #handler(Object)} 可能被 MQ 消费线程、
 *       调度线程与业务线程并发调用，同时 {@link #register(String, Method)} / {@code unregister} 也可能并发执行；</li>
 *   <li>所有方法都必须允许并发调用；注册/注销与分发并发进行时，分发过程使用的是调用时刻的监听器快照，
 *       注册完成后的事件才保证被新处理器接收，注销过程中正在分发的事件不保证排除该处理器；</li>
 *   <li>单个事件的一次 {@code handler}/{@code handlerWithResult} 调用内，对同一事件命中的监听器列表以快照方式遍历。</li>
 * </ul>
 *
 * @author qty
 */
public interface EventHandlerManager {

    /**
     * 注册事件处理器方法。
     * <p>
     * 将带有 {@link com.simple.common.eventbus.common.annotation.EventHandler} 注解的方法
     * 注册到事件处理器映射表中。
     * </p>
     *
     * @param beanName Spring 容器中的 bean 名称
     * @param method   事件处理器方法，需带有EventHandler注解，且方法只能允许一个参数
     */
    void register(String beanName, Method method);

    /**
     * 取消注册事件处理器方法。
     * <p>
     * 从事件处理器映射表中移除该方法声明的<strong>全部</strong>处理器：
     * 当多个 bean 的同一方法（同签名）都注册为处理器时，本方法会一并移除所有 bean 的该处理器。
     * 需要仅移除某个 bean 的处理器时，请使用 {@link #unregister(String, Method)}。
     * </p>
     *
     * @param method 需要取消注册的事件处理器方法
     */
    void unregister(Method method);

    /**
     * 取消注册指定 bean 的事件处理器方法（仅移除该 bean 的处理器，不影响其它 bean 的同签名方法）。
     * <p>默认实现不支持按 bean 精确注销，退化为 {@link #unregister(Method)}（移除全部同签名处理器），
     * 并记录告警；支持该语义的实现（如默认实现）应覆写本方法。</p>
     *
     * @param beanName 注册时使用的 bean 名称
     * @param method   需要取消注册的事件处理器方法
     */
    default void unregister(String beanName, Method method) {
        unregister(method);
    }

    /**
     * 分发并执行事件处理器。
     * <p>
     * 根据事件名称找到对应的处理器方法并执行，支持同步和异步两种执行模式。
     * 失败语义按实现区分：MQ 异步实现（MqEventBusService）为尽力而为投递，发布失败仅记录日志不向上抛出；
     * 同步实现（SyncEventBusService）在当前线程执行处理器，处理器异常会向上抛给发布方。
     * </p>
     *
     * @param event 事件数据对象（{@link com.simple.common.eventbus.common.entity.EventData}），
     *              其它类型仅记录错误日志并直接返回
     */
    default void handler(Object event) {
        handlerWithResult(event);
    }

    /**
     * 分发并执行事件处理器，并上报分发结果。
     * <p>
     * 失败语义：同步模式下处理器异常向上抛出，本方法不会返回 false；
     * MQ 异步模式下单个处理器失败仅记录日志并继续其余处理器（失败隔离），存在任一失败时返回 false，
     * 供 MQ 消费链路感知失败以触发消息级重试/死信。
     * </p>
     *
     * @param event 事件数据对象（{@link com.simple.common.eventbus.common.entity.EventData}），
     *              其它类型仅记录错误日志并直接返回（返回 true）
     * @return true 表示全部命中处理器执行成功；false 表示 MQ 异步模式下存在处理器执行失败
     */
    default boolean handlerWithResult(Object event) {
        handler(event);
        return true;
    }

}

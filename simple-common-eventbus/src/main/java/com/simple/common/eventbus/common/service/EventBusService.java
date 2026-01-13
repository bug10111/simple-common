package com.simple.common.eventbus.common.service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 事件发布器
 *
 * @author qty
 */
public interface EventBusService {

    /**
     * 发布事件
     *
     * @param event 事件
     */
    void push(Object event);

    /**
     * 发布事件
     *
     * @param obj        要发送的内容
     * @param eventClass 事件class
     */
    void push(Object obj, Class<?> eventClass);

    /**
     * 发布延迟事件
     *
     * @param event    事件
     * @param time     延迟时间
     * @param timeUnit 时间单位
     */
    void push(Object event, int time, TimeUnit timeUnit);

    /**
     * 发布延迟事件
     *
     * @param obj        要发送的内容
     * @param eventClass 事件class
     * @param time       延迟时间
     * @param timeUnit   时间单位
     */
    void push(Object obj, Class<?> eventClass, int time, TimeUnit timeUnit);

}

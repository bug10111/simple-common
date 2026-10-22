package com.simple.common.eventbus.common.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * 事件处理器注册键（系统名称 + 事件名称）
 * <p>作为监听器映射表（ConcurrentHashMap）的 key 使用，设计为不可变对象：
 * 等值字段一经构造即不可修改，杜绝“插入后修改字段导致 hash 漂移、
 * 映射表条目不可达或被复制”的隐患；两个字段均不允许为空白，
 * 避免空值键与任意其他键碰撞导致事件误分发。</p>
 *
 * @author qty
 */
@Getter
@ToString
@EqualsAndHashCode(of = { "applicationName", "eventName" })//重写equals和HashCode方法，用于对比key是否相等
public final class EventHandlerKey {

    //系统名称
    private final String applicationName;

    //事件名称
    private final String eventName;

    public EventHandlerKey(String applicationName, String eventName) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("EventHandlerKey.applicationName 不能为空");
        }
        if (eventName == null || eventName.trim().isEmpty()) {
            throw new IllegalArgumentException("EventHandlerKey.eventName 不能为空");
        }
        this.applicationName = applicationName;
        this.eventName = eventName;
    }
}

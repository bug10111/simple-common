package com.simple.common.websocket.common.entity;

import lombok.experimental.Accessors;

import java.lang.reflect.Method;

/**
 * 分发器存储key
 *
 * @author qty
 */
@Accessors(chain = true)
public record InvocationTarget(Object bean, Method method, Class<?> dataType) {
    public InvocationTarget(Object bean, Method method, Class<?> dataType) {
        this.bean = bean;
        this.method = method;
        this.dataType = dataType;
        this.method.setAccessible(true);
    }
}
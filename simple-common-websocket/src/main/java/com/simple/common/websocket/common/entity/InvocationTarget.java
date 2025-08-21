package com.simple.common.websocket.common.entity;

import lombok.experimental.Accessors;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA
 * Description: 分发器存储key
 *
 * @author 兄台丶请冷静
 */
@Accessors(chain = true)
public record InvocationTarget(Object bean, Method method) {
    public InvocationTarget(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.method.setAccessible(true);
    }
}

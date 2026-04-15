package com.simple.common.eventbus.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * 事件处理器值对象
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public final class EventHandlerValue {

    /**
     * 事件参数类型
     */
    private Class<?> aClass;

    /**
     * 高效方法调用句柄（Java 17 优化，替代反射 Method.invoke）
     */
    private MethodHandle methodHandle;

    /**
     * Bean实例
     */
    private Object bean;

    /**
     * 保留原始的反射 Method 对象，用于调试、日志输出或注销时匹配
     */
    private Method methodForDebug;
}
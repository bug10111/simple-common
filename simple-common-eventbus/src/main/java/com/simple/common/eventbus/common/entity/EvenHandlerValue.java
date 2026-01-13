package com.simple.common.eventbus.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public final class EvenHandlerValue {

    //事件对象class
    private Class<?> aClass;

    //事件触发的方法
    private Method method;

    //bean
    private Object bean;

}

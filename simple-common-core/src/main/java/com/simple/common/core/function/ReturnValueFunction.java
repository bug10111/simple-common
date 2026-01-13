package com.simple.common.core.function;

/**
 * Created by IntelliJ IDEA
 * Description: 默认函数式接口
 *
 * @author qty
 */
@FunctionalInterface
public interface ReturnValueFunction extends Function {

    /**
     * 无参数，有返回的接口
     */
    Object handler() throws Throwable;

}

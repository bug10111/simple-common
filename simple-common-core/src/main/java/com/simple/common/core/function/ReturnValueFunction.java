package com.simple.common.core.function;

/**
 * Created by 兄台丶请冷静
 * 默认函数式接口
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface ReturnValueFunction extends Function {

    /**
     * 无参数，有返回的接口
     */
    Object handler() throws Throwable;

}

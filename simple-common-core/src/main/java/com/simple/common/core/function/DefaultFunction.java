package com.simple.common.core.function;

/**
 * Created by 兄台丶请冷静
 * 默认函数式接口
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface DefaultFunction extends Function {

    /**
     * 无参数，无返回的逻辑
     */
    void handler() throws Throwable;

}

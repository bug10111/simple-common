package com.simple.common.eventbus.common.function;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface EventHandlerFunction<T, E> {

    void handler(T t, E e);

}

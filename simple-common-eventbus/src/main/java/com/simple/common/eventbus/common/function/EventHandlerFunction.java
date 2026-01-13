package com.simple.common.eventbus.common.function;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@FunctionalInterface
public interface EventHandlerFunction<T, E> {

    void handler(T t, E e);

}

package com.simple.common.cache.common.function;

/**
 * Created with IntelliJ IDEA
 * Description: 获取db数据
 *
 * @author qty
 */
@FunctionalInterface
public interface GetDBFunction<T, R> {

    /**
     * 获取数据
     *
     * @param request 请求参数
     */
    T get(R request);
}

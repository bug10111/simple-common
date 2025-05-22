package com.simple.common.cache.common.function;

/**
 * Created with IntelliJ IDEA
 * Description: 获取redis数据
 *
 * @author qty
 */
public interface GetRedisFunction<T, R> {

    /**
     * 获取数据
     *
     * @param request 请求参数
     */
    T get(R request);

    /**
     * 保存数据
     *
     * @param value 需要保存的值
     */
    void setHandler(T value);

    /**
     * 判断空值的保存数据
     *
     * @param value 需要保存的值
     */
    default void set( T value) {
        if (value != null) {
            setHandler(value);
        } else {
            setHandler(getNullable());
        }
    }

    /**
     * 获取空值
     */
    T getNullable();
}

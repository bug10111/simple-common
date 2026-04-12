package com.simple.common.cache.common.function;

/**
 * Created with IntelliJ IDEA
 * Description: 获取redis数据，并提供缓存回写与空值处理能力
 * <p>
 * 注意：实现 {@link #getNullable()} 时，必须返回一个非 null 的占位对象（如 NullObject），
 * 用于防止缓存穿透。若返回 null，则会导致缓存空值失效。
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
     * <p>
     * 若 value 非 null 则直接保存，若为 null 则保存 {@link #getNullable()} 返回的占位对象。
     *
     * @param value 需要保存的值
     */
    default void set(T value) {
        if (value != null) {
            setHandler(value);
        } else {
            setHandler(getNullable());
        }
    }

    /**
     * 获取空值占位对象
     * <p>
     * 该方法必须返回一个非 null 的标记对象，用于标识“数据库无此数据”的状态，
     * 避免缓存穿透。典型实现为返回一个特定的单例对象（如 NullObject）。
     *
     * @return 非 null 的空值占位对象
     */
    T getNullable();
}
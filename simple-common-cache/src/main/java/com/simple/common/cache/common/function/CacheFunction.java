package com.simple.common.cache.common.function;

/**
 * Created with IntelliJ IDEA
 * Description: 缓存function基类
 *
 * @author qty
 */
public interface CacheFunction<T, R> {

    /**
     * 获取数据
     *
     * @param request 请求参数
     * @param lockKey 锁key
     */
    T get(R request, String lockKey);

    /**
     * 保存数据
     *
     * @param lockKey 锁key
     * @param value   需要保存的值
     */
    void setHandler(String lockKey, T value);

    /**
     * 判断空值的保存数据
     *
     * @param lockKey 锁key
     * @param value   需要保存的值
     */
    default void set(String lockKey, T value) {
        if (value != null) {
            setHandler(lockKey, value);
        } else {
            setHandler(lockKey, getNullable());
        }
    }

    /**
     * 获取空值
     */
    T getNullable();

}

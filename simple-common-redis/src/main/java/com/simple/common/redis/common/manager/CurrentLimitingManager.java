package com.simple.common.redis.common.manager;

/**
 * Created by IntelliJ IDEA
 * Description: 限流接口
 *
 * @author qty
 */
public interface CurrentLimitingManager<T> {

    /**
     * 获取令牌
     *
     * @param t 限流注解
     * @return 获取令牌是否成功
     */
    Boolean execute(T t);

}

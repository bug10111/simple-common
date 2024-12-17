package com.simple.common.core.common.service.lock;

import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.function.ReturnValueFunction;

/**
 * Created by 兄台丶请冷静
 * <p>
 * 统一锁的接口，所有的锁的使用，包括分布式锁，都需要实现这个接口
 *
 * @author 兄台丶请冷静
 */
public interface LockService {

    /**
     * 可重入锁
     * <p>
     * 可重入锁指的是同一个线程里面，可以再次获取锁
     * 分布式锁根据key来加锁，意味着在不同接口，不同线程，只要是同一个锁标志，都会排队执行
     * 案例：扣库存
     *
     * @param key      加锁的key
     * @param function 执行的业务函数
     */
    void lock(String key, DefaultFunction function);

    /**
     * 可重入锁
     * <p>
     * 可重入锁指的是同一个线程里面，可以再次获取锁
     * 分布式锁根据key来加锁，意味着在不同接口，不同线程，只要是同一个锁标志，都会排队执行
     * 案例：扣库存
     *
     * @param key      加锁的key
     * @param function 执行的业务函数
     */
    Object lockHaveValue(String key, ReturnValueFunction function);

    /**
     * 公平锁
     * <p>
     * 公平锁大致和可重入锁相同，只是遵从先进先出原则
     * 分布式锁根据key来加锁，意味着在不同接口，不同线程，只要是同一个锁标志，都会排队执行
     * 案例：春节抢票
     *
     * @param key      加锁的key
     * @param function 执行的业务函数
     */
    void fairLock(String key, DefaultFunction function);

}

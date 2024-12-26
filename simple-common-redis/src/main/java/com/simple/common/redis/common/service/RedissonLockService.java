package com.simple.common.redis.common.service;

import com.simple.common.core.common.service.lock.AbsLockService;

/**
 * Created by IntelliJ IDEA
 * Description: redisson锁
 *
 * @author 兄台丶请冷静
 */
public abstract class RedissonLockService extends AbsLockService {

    /**
     * 闭锁-上锁
     * <p>
     * 闭锁是初始化一个数值并上锁，需要在其他地方将值全部清除，才能解除锁继续执行
     * 案例：假设学校放假了，一个班级有20个同学，需要等到20个同学全部离开教室了，我们才能关门。
     *
     * @param key     加锁的key
     * @param lockSum 案例中的同学数量
     */
    public abstract void countDownLatch(String key, Integer lockSum);

    /**
     * 闭锁-条件触发
     *
     * @param key 加锁的key
     */
    public abstract void decreaseCountDownLatch(String key);

    /**
     * 信号量-上锁
     * <p>
     * 信号量为存储在redis中的一个数字，当这个数字大于0时，才能执行
     * 案例：停车厂三个车位，当车位不够时，其他的车子只能等待车位空闲才能停车，如果车位足够时，车子入库并且空闲车位-1
     *
     * @param key     加锁的key
     * @param lockSum 案例中的车位
     */
    public abstract void semaphoreLock(String key, Integer lockSum);

    /**
     * 信号量-减少
     *
     * @param key     加锁的key
     * @param lockNum 增加的量
     */
    public abstract void decreaseSemaphoreLock(String key, Integer lockNum);

    /**
     * 信号量-增加
     *
     * @param key     加锁的key
     * @param lockNum 增加的量
     */
    public abstract void increaseSemaphoreLock(String key, Integer lockNum);
}

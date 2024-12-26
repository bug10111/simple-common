package com.simple.common.redis.service;

import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.redis.common.service.RedissonLockService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA
 * Description: redisson分布式锁的默认实现
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(prefix = "redisson", name = "open", havingValue = "true", matchIfMissing = true)
public class DefaultRedissonLockService extends RedissonLockService {

    @Autowired
    private Redisson redisson;

    @Override
    @SneakyThrows
    public void lock(String key, DefaultFunction function) {

        key = getKey("lock", key);

        //获取锁
        RLock lock = redisson.getLock(key);

        //        //设置过期时间（时间后自动释放锁）
        //        lock.lock(10, TimeUnit.SECONDS);

        //30秒刷新一次，自动续时间
        lock.lock();

        //只有第一个拿到锁，其他的自旋
        try {
            if (log.isDebugEnabled()) {
                log.debug("key [{}] 获取到锁", key);
            }
            function.handler();
        } finally {

            // 是否还是锁定状态
            if (lock.isLocked()) {

                // 是否是当前执行线程的锁
                if (lock.isHeldByCurrentThread()) {

                    // 释放锁
                    lock.unlock();
                    if (log.isDebugEnabled()) {
                        log.debug("key [{}] 释放锁", key);
                    }
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public Object lockHaveValue(String key, ReturnValueFunction function) {

        key = getKey("lockHaveValue", key);

        //获取锁
        RLock lock = redisson.getLock(key);

        //        //设置过期时间（时间后自动释放锁）
        //        lock.lock(10, TimeUnit.SECONDS);

        //30秒刷新一次，自动续时间
        lock.lock();

        //只有第一个拿到锁，其他的自旋
        try {
            if (log.isDebugEnabled()) {
                log.debug("key [{}] 获取到锁", key);
            }
            return function.handler();
        } finally {

            // 是否还是锁定状态
            if (lock.isLocked()) {

                // 是否是当前执行线程的锁
                if (lock.isHeldByCurrentThread()) {

                    // 释放锁
                    lock.unlock();
                    if (log.isDebugEnabled()) {
                        log.debug("key [{}] 释放锁", key);
                    }
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public void fairLock(String key, DefaultFunction function) {

        key = getKey("fairLock", key);

        //获取锁
        RLock lock = redisson.getFairLock(key);

        //设置过期时间（时间后自动释放锁）
        //        lock.lock(10, TimeUnit.SECONDS);

        //30秒刷新一次，自动续时间
        lock.lock();

        //只有第一个拿到锁，其他的自旋
        try {
            if (log.isDebugEnabled()) {
                log.debug("key [{}] 获取到公平锁", key);
            }
            function.handler();
        } finally {

            // 是否还是锁定状态
            if (lock.isLocked()) {

                // 是否是当前执行线程的锁
                if (lock.isHeldByCurrentThread()) {

                    // 释放锁
                    lock.unlock();
                    if (log.isDebugEnabled()) {
                        log.debug("key [{}] 释放公平锁", key);
                    }
                }
            }
        }
    }

    @Override
    @SneakyThrows
    public void countDownLatch(String key, Integer lockSum) {
        key = getKey("downLatchLock", key);
        RCountDownLatch door = redisson.getCountDownLatch(key);
        door.trySetCount(lockSum);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁上锁成功,当前计数 [{}]", key, lockSum);
        }
        door.await();
        //        door.await(10,TimeUnit.SECONDS);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁成功通行,当前计数", key);
        }
    }

    @Override
    public void decreaseCountDownLatch(String key) {
        key = getKey("downLatchLock", key);
        RCountDownLatch door = redisson.getCountDownLatch(key);
        door.countDown();
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁计数减少,当前计数 [{}]", key, door.getCount());
        }
    }

    @Override
    public void semaphoreLock(String key, Integer lockSum) {
        key = getKey("semaphoreLock", key);
        RSemaphore park = redisson.getSemaphore(key);
        park.trySetPermits(lockSum);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 信号量锁上锁成功,当前信号量 [{}]", key, lockSum);
        }
    }

    @Override
    public void increaseSemaphoreLock(String key, Integer lockNum) {
        key = getKey("semaphoreLock", key);
        RSemaphore park = redisson.getSemaphore(key);
        park.release(lockNum);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 信号量锁增加成功", key);
        }
    }

    @Override
    @SneakyThrows
    public void decreaseSemaphoreLock(String key, Integer lockNum) {
        key = getKey("semaphoreLock", key);
        RSemaphore park = redisson.getSemaphore(key);
        park.acquire(lockNum);

//        //最多等待秒数
//        park.tryAcquire(Duration.ofSeconds(5));
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 信号量锁减少成功", key);
        }
    }
}

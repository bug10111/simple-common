package com.simple.common.redis.service;

import com.simple.common.core.exception.DefaultException;
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

import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA
 * Description: redisson分布式锁的默认实现
 *
 * @author qty
 */
@Slf4j
@Component
@Primary
@ConditionalOnProperty(prefix = "redisson", name = "open", havingValue = "true", matchIfMissing = true)
public class DefaultRedissonLockService extends RedissonLockService {

    //信号量获取许可的最长等待秒数,超过该时长仍未获取到许可则抛出业务异常
    private static final long SEMAPHORE_ACQUIRE_WAIT_SECONDS = 5L;

    @Autowired
    private Redisson redisson;

    @Override
    @SneakyThrows
    public void lock(String key, DefaultFunction function) {

        key = getKey("lock", key);

        //获取锁
        RLock lock = redisson.getLock(key);

        //        //设置过期时间（时间后自动释放锁）
//                lock.lock(10, TimeUnit.SECONDS);

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

        //设置计数器初始值
        door.trySetCount(lockSum);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁上锁成功,当前计数 [{}]", key, lockSum);
        }

        //当前线程阻塞等待,直到计数被其他调用方减至0才继续执行
        door.await();
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁成功通行,当前计数 [{}]", key, door.getCount());
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
    @SneakyThrows
    public boolean tryAwait(String key, Long timeout, TimeUnit unit) {
        key = getKey("downLatchLock", key);
        RCountDownLatch door = redisson.getCountDownLatch(key);

        //在指定时长内等待计数归零,超时仍未归零返回false
        boolean passed = door.await(timeout, unit);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁等待结束,结果 [{}]", key, passed);
        }
        return passed;
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

        //在限定时长内获取许可,避免许可不足时线程无限悬挂
        boolean acquired = park.tryAcquire(lockNum, SEMAPHORE_ACQUIRE_WAIT_SECONDS, TimeUnit.SECONDS);

        //超时仍未获取到许可,抛出业务异常交由调用方处理
        if (!acquired) {
            throw new DefaultException("信号量获取超时, key: " + key);
        }
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 信号量锁减少成功", key);
        }
    }
}

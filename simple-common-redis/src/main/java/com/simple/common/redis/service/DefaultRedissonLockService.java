package com.simple.common.redis.service;

import com.simple.common.core.exception.DefaultException;
import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.redis.common.service.RedissonLockService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

    /**
     * 闭锁等待的兜底超时秒数：参与方异常退出未 countDown 时，等待线程最多悬挂该时长后快速失败，
     * 避免无限阻塞拖垮请求线程池
     */
    private static final long COUNT_DOWN_LATCH_WAIT_SECONDS = 300L;

    /**
     * 信号量获取许可的最长等待秒数,超过该时长仍未获取到许可则抛出业务异常
     */
    private static final long SEMAPHORE_ACQUIRE_WAIT_SECONDS = 5L;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    @SneakyThrows
    public void lock(String key, DefaultFunction function) {

        key = getKey("lock", key);

        //获取锁：lock() 不响应中断且由 Redisson 看门狗自动续期（等待为无限期，既有契约）；
        //需要可中断获取时业务方可改用 tryAwait/lockInterruptibly 形态
        RLock lock = redissonClient.getLock(key);

        lock.lock();

        //只有抢到锁的线程执行，其他的自旋等待
        try {
            if (log.isDebugEnabled()) {
                log.debug("key [{}] 获取到锁", key);
            }
            function.handler();
        } finally {
            //单次持有判定+受保护释放：释放失败只告警不抛出，避免 finally 异常
            //覆盖 function.handler() 的业务异常、或中断正常释放路径导致锁滞留
            releaseQuietly(key, lock, "锁");
        }
    }

    @Override
    @SneakyThrows
    public Object lockHaveValue(String key, ReturnValueFunction function) {

        key = getKey("lockHaveValue", key);

        //获取锁
        RLock lock = redissonClient.getLock(key);

        //未设 leaseTime，由 Redisson 看门狗自动续期
        lock.lock();

        //只有抢到锁的线程执行，其他的自旋等待
        try {
            if (log.isDebugEnabled()) {
                log.debug("key [{}] 获取到锁", key);
            }
            return function.handler();
        } finally {
            releaseQuietly(key, lock, "锁");
        }
    }

    @Override
    @SneakyThrows
    public void fairLock(String key, DefaultFunction function) {

        key = getKey("fairLock", key);

        //获取锁
        RLock lock = redissonClient.getFairLock(key);

        //未设 leaseTime，由 Redisson 看门狗自动续期
        lock.lock();

        //只有抢到锁的线程执行，其他的自旋等待
        try {
            if (log.isDebugEnabled()) {
                log.debug("key [{}] 获取到公平锁", key);
            }
            function.handler();
        } finally {
            releaseQuietly(key, lock, "公平锁");
        }
    }

    /**
     * 受保护的锁释放：仅当前持有线程释放，一次持有判定，释放异常降级为告警。
     * <p>
     * 原实现先 isLocked() 再 isHeldByCurrentThread() 再 unlock()——三次 Redis 往返，
     * 且 finally 中任何一步抛出（如锁过期后的 IllegalMonitorStateException、连接抖动）都会
     * 替换掉业务异常、甚至中断释放路径；此处收敛为单次持有判定 + 异常兜底。
     */
    private void releaseQuietly(String key, RLock lock, String lockType) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                if (log.isDebugEnabled()) {
                    log.debug("key [{}] 释放{}", key, lockType);
                }
            }
        } catch (Exception e) {
            log.warn("key [{}] 释放{}失败", key, lockType, e);
        }
    }

    @Override
    @SneakyThrows
    public void countDownLatch(String key, Integer lockSum) {
        //计数为 null/非正数直接快速失败，避免把无意义计数透传到 Redisson 深处才报错
        if (lockSum == null || lockSum <= 0) {
            throw new DefaultException("闭锁计数必须为正整数, key: " + key);
        }
        key = getKey("downLatchLock", key);
        RCountDownLatch door = redissonClient.getCountDownLatch(key);

        //trySetCount 只在 Redis 键首次创建时生效（create-only 语义）：键残留（如上次等待
        //超时/参与方崩溃遗留）时沿用旧计数，本次设置不生效——失败时显式告警，
        //避免日志宣称成功而等待方实际阻塞在陈旧计数上
        boolean initialized = door.trySetCount(lockSum);
        if (!initialized) {
            log.warn("key [{}] 闭锁已存在，沿用既有计数 [{}]，本次设置值 [{}] 未生效",
                    key, door.getCount(), lockSum);
        }

        //当前线程阻塞等待,直到计数被其他调用方减至0才继续执行
        if (!door.await(COUNT_DOWN_LATCH_WAIT_SECONDS, TimeUnit.SECONDS)) {
            //兜底超时快速失败：参与方异常退出未减计数时，避免等待线程永久悬挂拖垮线程池
            throw new DefaultException("闭锁等待超时, 当前计数: " + door.getCount() + ", key: " + key);
        }
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁成功通行,当前计数 [{}]", key, door.getCount());
        }
    }

    @Override
    public void decreaseCountDownLatch(String key) {
        key = getKey("downLatchLock", key);
        RCountDownLatch door = redissonClient.getCountDownLatch(key);
        door.countDown();
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁计数减少,当前计数 [{}]", key, door.getCount());
        }
    }

    @Override
    @SneakyThrows
    public boolean tryAwait(String key, Long timeout, TimeUnit unit) {
        //timeout 为 null/非正数时快速失败：null 会在 Redisson await(long, TimeUnit) 拆箱处
        //抛出难定位的 NPE，非正数语义未定义
        if (timeout == null || timeout <= 0) {
            throw new DefaultException("闭锁等待时长必须为正数, key: " + key);
        }
        key = getKey("downLatchLock", key);
        RCountDownLatch door = redissonClient.getCountDownLatch(key);

        //在指定时长内等待计数归零,超时仍未归零返回false
        boolean passed = door.await(timeout, unit);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 闭锁等待结束,结果 [{}]", key, passed);
        }
        return passed;
    }

    @Override
    public void semaphoreLock(String key, Integer lockSum) {
        //许可数必须为正整数，null/非正数快速失败，避免无效信号量被静默初始化
        if (lockSum == null || lockSum <= 0) {
            throw new DefaultException("信号量许可数量必须为正整数, key: " + key);
        }
        key = getKey("semaphoreLock", key);
        RSemaphore park = redissonClient.getSemaphore(key);
        //trySetPermits 只在 Redis 键首次创建时生效（create-only 语义）：复用/残留 key 时
        //沿用既有许可并告警，避免日志宣称的限额与实际限额不一致
        boolean initialized = park.trySetPermits(lockSum);
        if (!initialized) {
            log.warn("key [{}] 信号量已存在，沿用既有许可 [{}]，本次设置数量 [{}] 未生效",
                    key, park.availablePermits(), lockSum);
        }
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 信号量锁上锁成功,当前信号量 [{}]", key, park.availablePermits());
        }
    }

    @Override
    public void increaseSemaphoreLock(String key, Integer lockNum) {
        if (lockNum == null || lockNum <= 0) {
            throw new DefaultException("信号量释放数量必须为正整数, key: " + key);
        }
        key = getKey("semaphoreLock", key);
        RSemaphore park = redissonClient.getSemaphore(key);
        park.release(lockNum);
        if (log.isDebugEnabled()) {
            log.debug("key [{}] 信号量锁释放成功", key);
        }
    }

    @Override
    @SneakyThrows
    public void decreaseSemaphoreLock(String key, Integer lockNum) {
        if (lockNum == null || lockNum <= 0) {
            throw new DefaultException("信号量获取数量必须为正整数, key: " + key);
        }
        key = getKey("semaphoreLock", key);
        RSemaphore park = redissonClient.getSemaphore(key);

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

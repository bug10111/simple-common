package com.simple.common.redis.common.service;

import com.simple.common.core.common.service.lock.AbsLockService;

import java.util.concurrent.TimeUnit;

/**
 * Redisson分布式锁服务抽象类。
 * <p>
 * 基于Redisson实现的高级分布式锁功能,支持闭锁(CountDownLatch)和信号量(Semaphore)两种并发控制模式。
 * 继承自 {@link AbsLockService},提供了更细粒度的并发控制能力。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>闭锁：等待多个任务完成后才能继续执行,如批量数据处理、多步骤流程同步</li>
 *   <li>信号量：控制同时访问资源的线程数量,如连接池、限流、资源配额管理</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Service
 * public class MyRedissonLockService extends RedissonLockService {
 *     @Autowired
 *     private RedissonClient redissonClient;
 *
 *     @Override
 *     public void countDownLatch(String key, Integer lockSum) {
 *         RCountDownLatch latch = redissonClient.getCountDownLatch(key);
 *         latch.trySetCount(lockSum);
 *         //阻塞当前线程直到计数归零
 *         latch.await();
 *     }
 *
 *     @Override
 *     public void decreaseCountDownLatch(String key) {
 *         RCountDownLatch latch = redissonClient.getCountDownLatch(key);
 *         latch.countDown();
 *     }
 *
 *     // 其他方法实现...
 * }
 * }</pre>
 *
 * @author qty
 */
public abstract class RedissonLockService extends AbsLockService {

    /**
     * 闭锁-上锁
     * <p>
     * 初始化一个计数器并设置初始值,设置完成后<b>当前线程阻塞等待</b>,直到其他调用方通过
     * {@link #decreaseCountDownLatch(String)} 将计数减至0才继续执行,计数未归零前一直阻塞。
     * 适用于需要等待多个并行任务全部完成的场景;不希望无限阻塞时,改用 {@link #tryAwait(String, Long, TimeUnit)}。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 场景：等待20个学生全部离开教室后才能关门
     * String lockKey = "classroom:lock:" + classroomId;
     * redissonLockService.countDownLatch(lockKey, 20);  // 设置计数器为20,当前线程阻塞,直到计数归零
     *
     * // 在其他地方（如各任务线程内）,每个学生离开时调用
     * redissonLockService.decreaseCountDownLatch(lockKey);  // 计数器减1
     *
     * // 计数器减至0后,上面阻塞在countDownLatch的线程继续执行
     * }</pre>
     *
     * @param key     加锁的key,建议使用业务前缀避免冲突
     * @param lockSum 计数器初始值,表示需要等待完成的任务数量
     */
    public abstract void countDownLatch(String key, Integer lockSum);

    /**
     * 闭锁-条件触发
     * <p>
     * 将闭锁计数器减1。当计数器减至0时,所有阻塞在 {@link #countDownLatch(String, Integer)} 上的线程将被唤醒。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 学生离开教室时调用
     * public void studentLeave(String classroomId) {
     *     String lockKey = "classroom:lock:" + classroomId;
     *     redissonLockService.decreaseCountDownLatch(lockKey);
     * }
     * }</pre>
     *
     * @param key 加锁的key,必须与countDownLatch中使用的key一致
     */
    public abstract void decreaseCountDownLatch(String key);

    /**
     * 闭锁-条件等待（带超时）
     * <p>
     * 在指定时长内等待闭锁计数器归零:计数器归零立即返回true,超时仍未归零返回false,不会无限阻塞。
     * 闭锁计数器由 {@link #countDownLatch(String, Integer)} 或其他方式初始化。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 场景：最多等待20个学生离开30秒,超时不再等待
     * String lockKey = "classroom:lock:" + classroomId;
     * boolean passed = redissonLockService.tryAwait(lockKey, 30L, TimeUnit.SECONDS);
     * if (passed) {
     *     // 计数器已归零,执行关门等后续逻辑
     * } else {
     *     // 超时仍未归零,按超时策略处理
     * }
     * }</pre>
     *
     * @param key     加锁的key,必须与countDownLatch中使用的key一致
     * @param timeout 最长等待时长
     * @param unit    等待时长的单位
     * @return true表示计数器已归零;false表示超时仍未归零
     */
    public abstract boolean tryAwait(String key, Long timeout, TimeUnit unit);

    /**
     * 闭锁-条件等待（带超时,默认秒单位）
     * <p>
     * {@link #tryAwait(String, Long, TimeUnit)} 的便捷重载,等待时长单位默认为秒。
     * </p>
     *
     * @param key     加锁的key,必须与countDownLatch中使用的key一致
     * @param timeout 最长等待秒数
     * @return true表示计数器已归零;false表示超时仍未归零
     */
    public boolean tryAwait(String key, Long timeout) {
        return tryAwait(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 信号量-上锁
     * <p>
     * 初始化一个信号量,设置最大许可数量。只有当信号量值大于0时,才能获取许可继续执行。
     * 适用于控制同时访问共享资源的线程数量,实现限流和资源配额管理。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 场景：停车场有3个车位
     * String semaphoreKey = "parking:semaphore:" + parkingLotId;
     * redissonLockService.semaphoreLock(semaphoreKey, 3);  // 设置3个许可
     *
     * // 车辆进入停车场时获取许可（许可不足阻塞等待,超时抛业务异常）
     * redissonLockService.decreaseSemaphoreLock(semaphoreKey, 1);
     * parkVehicle(vehicle);
     * }</pre>
     *
     * @param key     信号量的key,建议使用业务前缀避免冲突
     * @param lockSum 最大许可数量,表示允许同时访问的资源数量
     */
    public abstract void semaphoreLock(String key, Integer lockSum);

    /**
     * 信号量-获取许可（值减少）
     * <p>
     * 获取指定数量的许可,信号量值相应减少。当前可用许可不足时阻塞等待,超过等待时长仍未获取到
     * 许可则抛出业务异常,不会无限阻塞。通常在进入受控资源前调用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 车辆进入停车场时获取许可
     * public void vehicleEnter(String parkingLotId) {
     *     String semaphoreKey = "parking:semaphore:" + parkingLotId;
     *     redissonLockService.decreaseSemaphoreLock(semaphoreKey, 1);  // 获取1个许可,空闲车位-1
     * }
     * }</pre>
     *
     * @param key     信号量的key
     * @param lockNum 需要获取的许可数量,通常为1
     */
    public abstract void decreaseSemaphoreLock(String key, Integer lockNum);

    /**
     * 信号量-释放许可（值增加）
     * <p>
     * 释放指定数量的许可,信号量值相应增加。通常在资源使用完毕后调用,与
     * {@link #decreaseSemaphoreLock(String, Integer)} 配对使用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 车辆离开停车场时释放许可
     * public void vehicleLeave(String parkingLotId) {
     *     String semaphoreKey = "parking:semaphore:" + parkingLotId;
     *     redissonLockService.increaseSemaphoreLock(semaphoreKey, 1);  // 释放1个许可,空闲车位+1
     * }
     * }</pre>
     *
     * @param key     信号量的key
     * @param lockNum 释放的许可数量,通常为1
     */
    public abstract void increaseSemaphoreLock(String key, Integer lockNum);
}

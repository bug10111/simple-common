package com.simple.common.redis.common.service;

import com.simple.common.core.common.service.lock.AbsLockService;

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
     * 初始化一个计数器并设置初始值,需要在其他地方将计数减至0,才能解除锁继续执行。
     * 适用于需要等待多个并行任务全部完成的场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 场景：等待20个学生全部离开教室后才能关门
     * String lockKey = "classroom:lock:" + classroomId;
     * redissonLockService.countDownLatch(lockKey, 20);  // 设置计数器为20
     * 
     * // 在其他地方，每个学生离开时调用
     * redissonLockService.decreaseCountDownLatch(lockKey);  // 计数器减1
     * 
     * // 当计数器减至0时，等待的线程会继续执行
     * redissonLockService.await(lockKey);  // 等待计数器归零
     * }</pre>
     *
     * @param key     加锁的key,建议使用业务前缀避免冲突
     * @param lockSum 计数器初始值,表示需要等待完成的任务数量
     */
    public abstract void countDownLatch(String key, Integer lockSum);

    /**
     * 闭锁-条件触发
     * <p>
     * 将闭锁计数器减1。当计数器减至0时,所有在await()上等待的线程将被唤醒。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 学生离开教室时调用
     * public void studentLeave(String classroomId) {
     *     String lockKey = "classroom:lock:" + classroomId;
     *     redissonLockService.decreaseCountDownLatch(lockKey);
     *     log.info("学生离开，当前剩余人数: {}", getRemainingCount(lockKey));
     * }
     * }</pre>
     *
     * @param key 加锁的key,必须与countDownLatch中使用的key一致
     */
    public abstract void decreaseCountDownLatch(String key);

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
     * // 车辆进入停车场时获取许可
     * boolean acquired = redissonLockService.tryAcquireSemaphore(semaphoreKey);
     * if (acquired) {
     *     // 成功进入，信号量值减1
     *     parkVehicle(vehicle);
     * } else {
     *     // 车位已满，等待或拒绝
     *     log.warn("车位已满，请稍后再试");
     * }
     * }</pre>
     *
     * @param key     信号量的key,建议使用业务前缀避免冲突
     * @param lockSum 最大许可数量,表示允许同时访问的资源数量
     */
    public abstract void semaphoreLock(String key, Integer lockSum);

    /**
     * 信号量-减少（释放许可）
     * <p>
     * 释放指定数量的许可,使信号量值增加。通常在资源使用完毕后调用。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 车辆离开停车场时释放许可
     * public void vehicleLeave(String parkingLotId) {
     *     String semaphoreKey = "parking:semaphore:" + parkingLotId;
     *     redissonLockService.decreaseSemaphoreLock(semaphoreKey, 1);  // 释放1个许可
     *     log.info("车辆离开，空闲车位+1");
     * }
     * }</pre>
     *
     * @param key     信号量的key
     * @param lockNum 释放的许可数量,通常为1
     */
    public abstract void decreaseSemaphoreLock(String key, Integer lockNum);

    /**
     * 信号量-增加（获取许可）
     * <p>
     * 获取指定数量的许可,使信号量值减少。如果当前可用许可不足,则阻塞等待或返回失败。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 车辆进入停车场时获取许可
     * public boolean vehicleEnter(String parkingLotId) {
     *     String semaphoreKey = "parking:semaphore:" + parkingLotId;
     *     return redissonLockService.increaseSemaphoreLock(semaphoreKey, 1);  // 获取1个许可
     * }
     * }</pre>
     *
     * @param key     信号量的key
     * @param lockNum 需要获取的许可数量,通常为1
     * @return true表示成功获取许可,false表示许可不足
     */
    public abstract void increaseSemaphoreLock(String key, Integer lockNum);
}

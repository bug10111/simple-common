package com.simple.common.core.common.service.lock;

import com.simple.common.core.function.DefaultFunction;
import com.simple.common.core.function.ReturnValueFunction;

/**
 * 分布式锁服务接口。
 * <p>
 * 提供统一的锁操作接口,支持可重入锁、公平锁等多种锁类型。
 * 默认实现 {@link com.simple.common.core.service.lock.RedissonLockService} 基于 Redisson 实现分布式锁。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>可重入锁：库存扣减、订单创建等需要保证原子性的场景</li>
 *   <li>公平锁：抢票、秒杀等需要保证顺序的场景</li>
 * </ul>
 *
 * @author qty
 */
public interface LockService {

    /**
     * 可重入锁(无返回值)
     * <p>
     * 适用于不需要返回值的业务场景,如库存扣减、状态更新等。
     * 同一线程可以多次获取同一把锁,每次获取后需要对应释放。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * lockService.lock("stock:" + productId, () -> {
     *     // 扣减库存逻辑
     *     stockService.decrease(productId, quantity);
     * });
     * }</pre>
     *
     * @param key      锁的标识Key,相同Key的请求会排队执行
     * @param function 要执行的业务函数(无返回值)
     * @throws RuntimeException 当获取锁失败或业务执行异常时抛出异常
     */
    void lock(String key, DefaultFunction function);

    /**
     * 可重入锁(有返回值)
     * <p>
     * 适用于需要返回值的业务场景,如查询并更新、计算并保存等。
     * 同一线程可以多次获取同一把锁,每次获取后需要对应释放。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * Integer newStock = (Integer) lockService.lockHaveValue("stock:" + productId, () -> {
     *     // 查询当前库存
     *     Integer stock = stockService.getStock(productId);
     *     // 扣减库存
     *     stockService.decrease(productId, quantity);
     *     return stock - quantity;
     * });
     * }</pre>
     *
     * @param key      锁的标识Key,相同Key的请求会排队执行
     * @param function 要执行的业务函数(有返回值)
     * @return 业务函数的返回值
     * @throws RuntimeException 当获取锁失败或业务执行异常时抛出异常
     */
    Object lockHaveValue(String key, ReturnValueFunction function);

    /**
     * 公平锁(无返回值)
     * <p>
     * 公平锁遵循先进先出(FIFO)原则,按照请求到达的顺序依次执行。
     * 适用于需要保证顺序的场景,如抢票、排队处理等。
     * 注意：公平锁性能略低于非公平锁,因为需要维护等待队列。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 春节抢票场景
     * lockService.fairLock("ticket:" + ticketId, () -> {
     *     // 检查余票
     *     if (ticketService.hasRemaining(ticketId)) {
     *         // 出票
     *         ticketService.issue(userId, ticketId);
     *     }
     * });
     * }</pre>
     *
     * @param key      锁的标识Key,相同Key的请求按到达顺序排队执行
     * @param function 要执行的业务函数(无返回值)
     * @throws RuntimeException 当获取锁失败或业务执行异常时抛出异常
     */
    void fairLock(String key, DefaultFunction function);

}

package com.simple.common.core.common.service.cycle;

import java.util.HashMap;
import java.util.Map;

/**
 * 循环任务服务接口。
 * <p>
 * 用于定义循环执行的任务逻辑,支持自动重试、延迟执行、定时检查等场景。
 * 默认抽象实现 {@link AbsCycleService} 提供了任务执行、失败重试、完成回调的完整流程框架。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>订单状态轮询：定期检查订单支付状态</li>
 *   <li>异步任务重试：失败后自动重试,最多重试N次</li>
 *   <li>数据同步检查：定期检柨数据同步状态</li>
 * </ul>
 *
 * <h3>扩展示例：</h3>
 * <pre>{@code
 * @Service
 * public class OrderStatusCheckService extends AbsCycleService<OrderQuery> {
 *     @Override
 *     public void execute(OrderQuery query) {
 *         // 查询订单状态
 *         Order order = orderService.findById(query.getOrderId());
 *         if (order.isPaid()) {
 *             // 已支付,完成任务
 *             complete(query);
 *         } else {
 *             // 未支付,继续等待下次检查
 *             throw new RuntimeException("订单未支付");
 *         }
 *     }
 *     
 *     @Override
 *     public void complete(OrderQuery query) {
 *         // 任务完成后的处理
 *         log.info("订单 {} 已支付", query.getOrderId());
 *     }
 * }
 * }</pre>
 *
 * @param <T> 任务数据类型
 * @author qty
 */
public interface CycleService<T> {

    /**
     * 执行循环任务
     * <p>
     * 根据配置的执行次数、时间间隔等参数,循环执行任务。
     * 支持均匀延迟和累加延迟两种模式。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 最多执行5次,每次间隔10秒,累加延迟
     * cycleService.run(orderQuery, 5, 10, true, params);
     * // 第1次: 立即执行
     * // 第2次: 10秒后执行
     * // 第3次: 20秒后执行
     * // 第4次: 30秒后执行
     * // 第5次: 40秒后执行
     * }</pre>
     *
     * @param runBody      任务参数对象
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔(单位:秒)
     * @param isAccumulate 是否累加延迟:true 表示延迟时间 = timeInterval × 当前次数,false 表示每次延迟固定为 timeInterval
     * @param parameters   扩展参数Map,可传递额外业务数据
     * @throws RuntimeException 当任务执行失败且未达到最大重试次数时抛出,触发下次重试
     */
    void run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters);

    /**
     * 均匀延迟时间执行(每次间隔固定)
     * <p>
     * 便捷方法,每次执行间隔固定为 timeInterval 秒。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 最多执行5次,每次间隔10秒
     * cycleService.runUniform(orderQuery, 5, 10);
     * // 第1次: 立即执行
     * // 第2次: 10秒后执行
     * // 第3次: 20秒后执行
     * // ...
     * }</pre>
     *
     * @param runBody      任务参数对象
     * @param sum          最大执行次数
     * @param timeInterval 每次执行的时间间隔(单位:秒)
     * @see #run(Object, Integer, Integer, Boolean, Map)
     */
    default void runUniform(T runBody, Integer sum, Integer timeInterval) {
        run(runBody, sum, timeInterval, false, new HashMap<>());
    }

    /**
     * 累加延迟时间执行
     * <p>
     * 便捷方法,延迟时间 = timeInterval × 当前次数。
     * 适用于需要逐渐增加等待时间的场景,如指数退避重试。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 最多执行5次,基础间隔10秒,累加延迟
     * cycleService.runAccumulate(orderQuery, 5, 10);
     * // 第1次: 立即执行
     * // 第2次: 10秒后执行 (10 × 1)
     * // 第3次: 20秒后执行 (10 × 2)
     * // 第4次: 30秒后执行 (10 × 3)
     * // 第5次: 40秒后执行 (10 × 4)
     * }</pre>
     *
     * @param runBody      任务参数对象
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔(单位:秒)
     * @see #run(Object, Integer, Integer, Boolean, Map)
     */
    default void runAccumulate(T runBody, Integer sum, Integer timeInterval) {
        run(runBody, sum, timeInterval, true, new HashMap<>());
    }

    /**
     * 累加延迟时间执行(带扩展参数)
     * <p>
     * 与 {@link #runAccumulate(Object, Integer, Integer)} 相同,但支持传递扩展参数。
     * </p>
     *
     * @param runBody      任务参数对象
     * @param sum          最大执行次数
     * @param timeInterval 基础时间间隔(单位:秒)
     * @param parameters   扩展参数Map
     * @see #runAccumulate(Object, Integer, Integer)
     */
    default void runAccumulate(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters) {
        run(runBody, sum, timeInterval, true, parameters);
    }

    /**
     * 均匀延迟时间执行(带扩展参数)
     * <p>
     * 与 {@link #runUniform(Object, Integer, Integer)} 相同,但支持传递扩展参数。
     * </p>
     *
     * @param runBody      任务参数对象
     * @param sum          最大执行次数
     * @param timeInterval 每次执行的时间间隔(单位:秒)
     * @param parameters   扩展参数Map
     * @see #runUniform(Object, Integer, Integer)
     */
    default void runUniform(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters) {
        run(runBody, sum, timeInterval, false, parameters);
    }
}
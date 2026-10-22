package com.simple.common.eventbus.common.service;

import java.util.concurrent.TimeUnit;

/**
 * 事件总线服务接口。
 * <p>
 * 提供事件发布功能,支持同步和异步两种事件处理方式。
 * 默认实现包括 {@link com.simple.common.eventbus.service.SyncEventBusService}(同步) 
 * 和 {@link com.simple.common.eventbus.service.MqEventBusService}(异步,基于RabbitMQ)。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>模块间解耦：用户注册后发送欢迎邮件、初始化数据等</li>
 *   <li>异步处理：订单创建后发送通知、更新统计等</li>
 *   <li>延迟任务：订单超时取消、定时提醒等</li>
 * </ul>
 *
 * @author qty
 */
public interface EventBusService {

    /**
     * 发布事件(自动识别事件类型)
     * <p>
     * 事件类型取 @Event 注解的 value()，未指定时取事件类的短类名。
     * 事件对象必须添加 @Event 注解。
     * </p>
     * <p>失败语义按实现区分：
     * 同步实现（SyncEventBusService）在发布方线程执行处理器，处理器异常向发布方抛出；
     * MQ 实现（MqEventBusService）为尽力而为投递，单个目标发送失败仅记录日志并继续其余目标，
     * 不向上抛出，发布方不能依赖本方法感知 MQ 侧的投递失败（无投递保证）。</p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * eventBusService.push(new UserCreatedEvent()
     *     .setUserId("123")
     *     .setUsername("张三"));
     * }</pre>
     *
     * @param event 事件对象,必须带有@Event注解
     * @throws IllegalArgumentException 事件对象为空或未标注@Event时抛出（两个实现行为一致）
     * @throws RuntimeException         同步模式下处理器执行失败时抛出；MQ 模式下不会因投递失败抛出
     */
    void push(Object event);

    /**
     * 发布事件(指定事件类型)
     * <p>
     * 显式指定事件类型,适用于需要将对象转换为特定事件类型的场景。
     * </p>
     * <p>前置条件与失败语义：
     * eventClass 本身必须带有 @Event 注解（转换后走 {@link #push(Object)}，缺注解会抛出 IllegalArgumentException）；
     * obj/eventClass 为空时抛出 IllegalArgumentException；
     * 字段名与类型不匹配的属性不会拷贝（保持目标类型默认值），不做额外校验。</p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 将 Map 转换为 UserCreatedEvent 事件
     * Map<String, Object> data = new HashMap<>();
     * data.put("userId", "123");
     * data.put("username", "张三");
     * eventBusService.push(data, UserCreatedEvent.class);
     * }</pre>
     *
     * @param obj        要发送的数据对象,不能为空
     * @param eventClass 事件类型Class,必须带有@Event注解,不能为空
     * @throws IllegalArgumentException obj/eventClass 为空，或 eventClass 未标注 @Event 时抛出
     */
    void push(Object obj, Class<?> eventClass);

    /**
     * 发布延迟事件(自动识别事件类型)
     * <p>
     * 延迟指定时间后发布事件,适用于定时任务、超时处理等场景。
     * 底层通过 RabbitMQ 延迟队列或定时调度实现。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 30分钟后取消未支付订单
     * eventBusService.push(new OrderTimeoutEvent()
     *     .setOrderId("ORDER123"), 30, TimeUnit.MINUTES);
     * }</pre>
     *
     * @param event    事件对象,必须带有@Event注解
     * @param time     延迟时间,必须大于 0；<= 0 时按实现立即执行/立即发送（不会丢弃事件）
     * @param timeUnit 时间单位,不能为空（同步模式延迟分支与 MQ 延迟交换机都依赖它）
     * @throws IllegalArgumentException 延迟分支下 timeUnit 为空时抛出
     * @throws RuntimeException         同步模式下延迟执行失败仅记录调度日志；MQ 模式下不会因投递失败抛出
     */
    void push(Object event, int time, TimeUnit timeUnit);

    /**
     * 发布延迟事件(指定事件类型)
     * <p>
     * 延迟指定时间后发布事件,并显式指定事件类型。
     * 适用于需要将对象转换为特定事件类型的延迟场景。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 24小时后发送用户活跃度提醒
     * Map<String, Object> data = new HashMap<>();
     * data.put("userId", "123");
     * eventBusService.push(data, UserActiveReminderEvent.class, 24, TimeUnit.HOURS);
     * }</pre>
     *
     * @param obj        要发送的数据对象
     * @param eventClass 事件类型Class
     * @param time       延迟时间
     * @param timeUnit   时间单位
     * @throws RuntimeException 当延迟事件发布失败时抛出异常
     */
    void push(Object obj, Class<?> eventClass, int time, TimeUnit timeUnit);

}

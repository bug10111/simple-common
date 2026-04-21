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
     * 根据事件对象的类名自动识别事件类型,并分发给对应的处理器。
     * 事件对象必须添加 @Event 注解。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * eventBusService.push(new UserCreatedEvent()
     *     .setUserId("123")
     *     .setUsername("张三"));
     * }</pre>
     *
     * @param event 事件对象,必须带有@Event注解
     * @throws RuntimeException 当事件处理失败时抛出异常
     */
    void push(Object event);

    /**
     * 发布事件(指定事件类型)
     * <p>
     * 显式指定事件类型,适用于需要将对象转换为特定事件类型的场景。
     * </p>
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
     * @param obj        要发送的数据对象
     * @param eventClass 事件类型Class,用于确定目标处理器
     * @throws RuntimeException 当事件处理失败时抛出异常
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
     * @param time     延迟时间
     * @param timeUnit 时间单位
     * @throws RuntimeException 当延迟事件发布失败时抛出异常
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

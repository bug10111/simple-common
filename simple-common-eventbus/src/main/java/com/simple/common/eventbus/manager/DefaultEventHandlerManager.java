package com.simple.common.eventbus.manager;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectReader;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.SerializeUtils;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.entity.EventHandlerKey;
import com.simple.common.eventbus.common.entity.EventHandlerValue;
import com.simple.common.eventbus.common.function.EventHandlerFunction;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import com.simple.common.eventbus.common.properties.EventProperties;
import com.simple.common.eventbus.util.EventThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件管理器默认实现（使用 MethodHandle 优化反射调用，适用于 Java 17）
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultEventHandlerManager implements EventHandlerManager {

    private final ConcurrentHashMap<EventHandlerKey, List<EventHandlerValue>> listenersMap = new ConcurrentHashMap<>();

    /**
     * Jackson ObjectReader 缓存，按目标类型复用，提升反序列化性能
     */
    private final Map<Class<?>, ObjectReader> readerCache = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private EventProperties eventProperties;

    /**
     * 判断当前事件模式是否为同步模式
     * <p>模式口径与 SyncEventBusService/MqEventBusService 的装配条件一致，均以 simple.event.type 为准</p>
     *
     * @return true 表示同步模式（simple.event.type=sync）
     */
    private boolean isSyncMode() {
        return EventConstant.EVENT_TYPE_SYNC.equals(eventProperties.getType());
    }

    /**
     * 将同步模式下的事件处理器异常向上传播给发布方
     * <p>保持原始异常类型与栈信息，无法原样抛出时以受检异常包装并保留原因链</p>
     *
     * @param e           处理器执行异常
     * @param eventData   事件数据
     * @param evenHandler 失败的处理器
     */
    private void rethrowSyncFailure(Throwable e, EventData eventData, EventHandlerValue evenHandler) {
        // 运行时异常与错误原样抛出，保留原始类型与栈信息
        if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (e instanceof Error error) {
            throw error;
        }
        // 受检异常无法原样抛出，包装为运行时异常并保留原因链
        throw new IllegalStateException("同步事件处理器执行失败, 事件名称: " + eventData.getEventName()
                + ", 处理器: " + handlerName(evenHandler), e);
    }

    /**
     * 获取事件处理器的展示名称，用于异常信息与日志定位
     *
     * @param evenHandler 事件处理器
     * @return 处理器方法名，无法获取时返回 unknown
     */
    private String handlerName(EventHandlerValue evenHandler) {
        Method method = evenHandler.getMethodForDebug();
        return method != null ? method.getName() : "unknown";
    }

    /**
     * 注册事件监听器（精确指定 beanName）
     *
     * @param beanName Spring 容器中的 bean 名称
     * @param method   带有 @EventHandler 注解的方法
     */
    @Override
    public void register(String beanName, Method method) {
        // getBean 在 bean 不存在时直接抛出 NoSuchBeanDefinitionException（信息比手工判空更明确），无需再判 null
        Object bean = applicationContext.getBean(beanName);
        doRegister(beanName, method, bean);
    }

    /**
     * 取消注册事件监听器
     *
     * @param method 带有 @EventHandler 注解的方法
     */
    @Override
    public void unregister(Method method) {
        EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer = (eventHandlerKey, eventHandlerValue) ->
                // computeIfPresent 在同一 key 的桶锁内原子完成“移除匹配项 + 空表清理”，
                // 与 doRegister 的 computeIfAbsent 串行化，避免并发注册时整表误删
                listenersMap.computeIfPresent(eventHandlerKey, (key, list) -> {
                    list.removeIf(v -> method.equals(v.getMethodForDebug()));
                    return list.isEmpty() ? null : list;
                });
        mapOperation(method, consumer, null, null);
    }

    /**
     * 取消注册指定 bean 的事件监听器（仅移除该 bean 的处理器，不影响其它 bean 的同签名方法）
     *
     * @param beanName 注册时使用的 bean 名称
     * @param method   带有 @EventHandler 注解的方法
     */
    @Override
    public void unregister(String beanName, Method method) {
        EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer = (eventHandlerKey, eventHandlerValue) ->
                listenersMap.computeIfPresent(eventHandlerKey, (key, list) -> {
                    list.removeIf(v -> method.equals(v.getMethodForDebug()) && StrUtil.equals(beanName, v.getBeanName()));
                    return list.isEmpty() ? null : list;
                });
        mapOperation(method, consumer, null, beanName);
    }

    /**
     * 处理事件
     * <p>统一在此处管理 ThreadLocal 生命周期，避免同步调用时未清理导致内存泄漏</p>
     *
     * @param event 事件对象（必须为EventData类型）
     */
    @Override
    public void handler(Object event) {
        handlerWithResult(event);
    }

    /**
     * 处理事件并上报分发结果
     * <p>
     * 失败语义按事件模式区分：
     * 同步模式（simple.event.type=sync）下处理器异常向上抛出，发布方直接感知失败，本方法不会返回 false；
     * MQ 异步模式（simple.event.type=mq）下单个处理器失败仅记录日志并继续执行其余处理器（失败隔离），
     * 存在任一失败时返回 false，供 MQ 消费链路感知失败以触发消息级重试/死信。
     * </p>
     *
     * @param event 事件对象（必须为EventData类型）
     * @return true 表示全部命中处理器执行成功；false 表示 MQ 异步模式下存在处理器执行失败
     */
    @Override
    public boolean handlerWithResult(Object event) {
        if (!(event instanceof EventData eventData)) {
            String eventTypeName = event == null ? "null" : event.getClass().getName();
            log.error("事件处理器只接受EventData类型，当前类型: {}", eventTypeName);
            return true;
        }

        // 将事件数据放入 ThreadLocal，便于处理器内部获取上下文；
        // 先保存外层上下文（支持监听器内嵌套发布/同步执行事件），结束后还原，避免清空嵌套外层的上下文
        String contextKey = eventData.getClass().getName();
        Object previousContext = EventThreadLocalUtils.get(contextKey);
        EventThreadLocalUtils.put(contextKey, eventData);

        try {
            // 获取原始目标系统列表，不要覆盖原始字段
            List<String> targetApplications = eventData.getObjectivesApplication();
            if (ObjUtil.isEmpty(targetApplications)) {
                // 若未指定目标，默认只发送到本机
                targetApplications = Collections.singletonList(applicationProperties.getName());
            }

            String eventName = eventData.getEventName();
            boolean allSuccess = true;

            // 遍历每个目标系统进行匹配
            for (String targetApp : targetApplications) {
                if (EventConstant.TARGET_ALL.equals(targetApp)) {
                    // 全局广播：查找 applicationName=TARGET_ALL 的监听器
                    EventHandlerKey allKey = new EventHandlerKey(EventConstant.TARGET_ALL, eventName);
                    allSuccess &= handlerForTarget(eventData, listenersMap.get(allKey));
                } else {
                    // 精确匹配：查找 applicationName=targetApp 的监听器
                    EventHandlerKey exactKey = new EventHandlerKey(targetApp, eventName);
                    allSuccess &= handlerForTarget(eventData, listenersMap.get(exactKey));
                }
            }
            return allSuccess;
        } finally {
            // 仅清理本次调用写入的 key：外层存在上下文时还原，否则移除自身
            if (previousContext != null) {
                EventThreadLocalUtils.put(contextKey, previousContext);
            } else {
                EventThreadLocalUtils.remove(contextKey);
            }
        }
    }

    /**
     * 针对特定目标执行监听器列表
     * <p>使用 ObjectReader 缓存提升反序列化性能，避免每次创建 Reader</p>
     * <p>异常语义按事件模式区分：
     * 同步模式（simple.event.type=sync）下发布方与处理器同线程执行，任一处理器失败立即向发布方抛出，保证发布方可感知业务失败；
     * MQ 异步模式（simple.event.type=mq）下处理器在消费线程执行，单个处理器失败仅记录日志并继续执行其余处理器（失败隔离），
     * 聚合结果经 {@link #handlerWithResult(Object)} 返回，由 MQ 消费链路负责消息级重试</p>
     *
     * @param eventData    事件数据
     * @param listenerList 监听器列表
     * @return true 表示全部监听器执行成功
     */
    private boolean handlerForTarget(EventData eventData, List<EventHandlerValue> listenerList) {
        if (ObjUtil.isEmpty(listenerList)) {
            return true;
        }

        boolean allSuccess = true;
        for (EventHandlerValue evenHandler : listenerList) {
            try {
                Class<?> paramType = evenHandler.getAClass();
                // 从缓存获取或创建 ObjectReader
                ObjectReader reader = readerCache.computeIfAbsent(paramType, SerializeUtils::readerFor);
                Object paramObj = reader.readValue(eventData.getMsgData());

                // 使用 MethodHandle 高效调用，性能接近原生方法调用
                evenHandler.getMethodHandle().invoke(paramObj);
            } catch (Throwable e) {
                // 同步模式：异常向上传播，发布方必须感知处理器失败
                if (isSyncMode()) {
                    rethrowSyncFailure(e, eventData, evenHandler);
                }
                // MQ 异步模式：失败隔离，但 JVM 级 Error（OOM/StackOverflow 等）不属于业务失败，直接上抛终止消费
                if (e instanceof Error error) {
                    throw error;
                }
                allSuccess = false;
                log.error("执行事件处理器失败, 事件名称: {}, 处理器: {}",
                          eventData.getEventName(), handlerName(evenHandler), e);
            }
        }
        return allSuccess;
    }

    /**
     * 对方法进行通用操作（注册或取消注册）
     *
     * @param method   目标方法
     * @param consumer 具体操作函数
     * @param bean     指定的 Bean 实例（注册时提供，注销时为 null，此时跳过调用句柄创建）
     * @param beanName 注册/注销时的 bean 名称（注销时可为 null）
     */
    protected void mapOperation(Method method, EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer, Object bean, String beanName) {
        EventHandler annotation = method.getAnnotation(EventHandler.class);
        if (annotation == null) {
            return;
        }

        String[] applicationName = annotation.applicationName();
        String eventName = annotation.value();
        Parameter[] parameters = method.getParameters();
        AssertUtils.isTrue(ObjUtil.isNotEmpty(parameters) && parameters.length == 1, "EventHandler标记的方法必须且只能有一个参数对象");
        // 空数组若放行，注册循环不会执行，处理器会被静默忽略，这里快速失败暴露配置错误
        AssertUtils.isTrue(applicationName.length > 0, "EventHandler的applicationName必须至少声明一个目标系统");
        Class<?> type = parameters[0].getType();
        if (StrUtil.isBlank(eventName)) {
            eventName = type.getSimpleName();
        }

        EventHandlerValue value = new EventHandlerValue()
                .setAClass(type)
                .setMethodForDebug(method) // 保留原始 Method 对象，用于注销和日志
                .setBeanName(beanName);

        // 注销场景（bean 为 null）不创建调用句柄：移除操作只做方法匹配，
        // 若对无实例方法强行构造句柄会因 bindTo(null)/访问受限抛异常中断注销
        if (bean != null) {
            // 创建高效的 MethodHandle 调用句柄（内部保证非 null：彻底失败时抛出 IllegalStateException）
            value.setMethodHandle(createMethodHandle(method, bean));
        }

        for (String appName : applicationName) {
            String resolvedAppName = EventConstant.THIS_MACHINE.equals(appName) ? applicationProperties.getName() : appName;

            EventHandlerKey key = new EventHandlerKey(resolvedAppName, eventName);

            consumer.handler(key, value);
        }
    }

    /**
     * 执行注册的核心逻辑
     */
    private void doRegister(String beanName, Method method, Object bean) {
        EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer = (eventHandlerKey, eventHandlerValue) ->
                listenersMap.computeIfAbsent(eventHandlerKey, k -> new CopyOnWriteArrayList<>()).add(eventHandlerValue);
        mapOperation(method, consumer, bean, beanName);
    }

    /**
     * 使用 MethodHandle 创建高效的方法调用句柄（Java 17 优化）
     * <p>创建彻底失败时降级为基于 {@code Method.invoke} 的反射句柄（对私有方法、模块封闭、静态方法均可用）</p>
     *
     * @param method 目标方法
     * @param bean   目标 Bean 实例
     * @return MethodHandle 调用句柄，签名适配为 (Object) -> void
     */
    private MethodHandle createMethodHandle(Method method, Object bean) {
        try {
            // 确保方法可访问（绕过 Java 访问控制检查）
            method.setAccessible(true);

            MethodHandles.Lookup lookup;
            try {
                // 尝试获取私有访问权限（适用于模块化环境）
                lookup = MethodHandles.privateLookupIn(method.getDeclaringClass(), MethodHandles.lookup());
            } catch (IllegalAccessException e) {
                // 降级为默认 lookup（适用于非模块化环境）
                lookup = MethodHandles.lookup();
            }

            // 将反射 Method 转换为 MethodHandle
            MethodHandle handle = lookup.unreflect(method);
            // 绑定 bean 实例（相当于 this 对象）；静态方法没有接收者，bindTo 会把事件参数错误地绑定到接收者位
            MethodHandle boundHandle = Modifier.isStatic(method.getModifiers()) ? handle : handle.bindTo(bean);
            // 调整方法类型为 (Object) -> void，方便统一调用
            return boundHandle.asType(MethodType.methodType(void.class, Object.class));
        } catch (Throwable e) {
            // 统一降级为基于 Method.invoke 的句柄（再次 unreflect+bindTo 会以相同原因失败，不是有效降级）
            log.warn("MethodHandle 创建失败，降级为反射调用句柄: {}", method, e);
            return createReflectiveMethodHandle(method, bean);
        }
    }

    /**
     * 反射降级句柄：将 {@code Method.invoke} 包装为 (Object) -> void 句柄
     * <p>通过 {@code findVirtual(Method.class, "invoke", ...)} 构造调用点，
     * 对 private 方法（JPMS 未开放包）、静态方法、任意参数类型均可用，是真正的兜底路径；
     * 若构造仍失败说明处理器方法完全不可调用，抛出异常阻止注册半可用的处理器</p>
     *
     * @param method 目标方法
     * @param bean   目标 Bean 实例（静态方法忽略）
     * @return MethodHandle 调用句柄，签名适配为 (Object) -> void
     */
    private MethodHandle createReflectiveMethodHandle(Method method, Object bean) {
        try {
            method.setAccessible(true);
            boolean staticMethod = Modifier.isStatic(method.getModifiers());
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // (Object receiver, Object[] args) -> Object
            MethodHandle invokeHandle = lookup.findVirtual(Method.class, "invoke",
                    MethodType.methodType(Object.class, Object.class, Object[].class));
            // 绑定 method 常量 -> (Object receiver, Object[] args) -> Object
            invokeHandle = MethodHandles.insertArguments(invokeHandle, 0, method);
            // 绑定接收者（静态方法接收者传 null，Method.invoke 对静态方法忽略接收者） -> (Object[] args) -> Object
            invokeHandle = MethodHandles.insertArguments(invokeHandle, 0, staticMethod ? null : bean);
            // 展开单事件参数 -> (Object event) -> Object
            invokeHandle = invokeHandle.asSpreader(Object[].class, 1);
            return invokeHandle.asType(MethodType.methodType(void.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("创建事件处理器调用句柄失败: " + method, e);
        }
    }
}

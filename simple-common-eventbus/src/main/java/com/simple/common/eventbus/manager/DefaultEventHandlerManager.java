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
import com.simple.common.eventbus.util.EventThreadLocalUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
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

    /**
     * 注册事件监听器（通过方法对象，已废弃）
     *
     * @param method 带有 @EventHandler 注解的方法
     * @deprecated 该方法通过 Class 查找 Bean 可能不准确，建议使用 {@link #register(String, Method)}
     */
    @Override
    @Deprecated
    public void register(Method method) {
        Class<?> declaringClass = method.getDeclaringClass();
        Object bean;
        try {
            bean = applicationContext.getBean(declaringClass);
        } catch (Exception e) {
            log.error("通过 Class 获取事件处理器 Bean 失败: {}，该方法将被忽略", declaringClass.getName(), e);
            throw new IllegalStateException("无法获取事件处理器Bean: " + declaringClass.getName(), e);
        }
        // 获取 Bean 名称（降级处理）
        String[] beanNames = applicationContext.getBeanNamesForType(declaringClass);
        String beanName = beanNames.length > 0 ? beanNames[0] : declaringClass.getSimpleName();
        doRegister(beanName, method, bean);
    }

    /**
     * 注册事件监听器（推荐使用，精确指定 beanName）
     *
     * @param beanName Spring 容器中的 bean 名称
     * @param method   带有 @EventHandler 注解的方法
     */
    @Override
    public void register(String beanName, Method method) {
        Object bean = applicationContext.getBean(beanName);
        if (bean == null) {
            log.error("无法根据 beanName 获取 Bean 实例: {}", beanName);
            throw new IllegalStateException("无法获取事件处理器Bean: " + beanName);
        }
        doRegister(beanName, method, bean);
    }

    /**
     * 取消注册事件监听器
     *
     * @param method 带有 @EventHandler 注解的方法
     */
    @Override
    public void unregister(Method method) {
        EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer = (eventHandlerKey, eventHandlerValue) -> {
            List<EventHandlerValue> list = listenersMap.get(eventHandlerKey);
            if (list != null) {
                // 使用保存的原始 Method 进行匹配，确保准确移除
                list.removeIf(v -> method.equals(v.getMethodForDebug()));
                if (list.isEmpty()) {
                    listenersMap.remove(eventHandlerKey);
                }
            }
        };
        mapOperation(method, consumer, null);
    }

    /**
     * 处理事件
     * <p>修复：统一在此处管理 ThreadLocal 生命周期，避免同步调用时未清理导致内存泄漏</p>
     *
     * @param event 事件对象（必须为EventData类型）
     */
    @Override
    public void handler(Object event) {
        if (!(event instanceof EventData eventData)) {
            log.error("事件处理器只接受EventData类型，当前类型: {}", event.getClass().getName());
            return;
        }

        try {
            // 将事件数据放入 ThreadLocal，便于处理器内部获取上下文
            EventThreadLocalUtils.put(eventData.getClass().getName(), eventData);

            // 获取原始目标系统列表，不要覆盖原始字段
            List<String> targetApplications = eventData.getObjectivesApplication();
            if (ObjUtil.isEmpty(targetApplications)) {
                // 若未指定目标，默认只发送到本机
                targetApplications = Collections.singletonList(applicationProperties.getName());
            }

            String eventName = eventData.getEventName();

            // 遍历每个目标系统进行匹配
            for (String targetApp : targetApplications) {
                // 精确匹配目标系统
                EventHandlerKey exactKey = new EventHandlerKey().setEventName(eventName).setApplicationName(targetApp);
                List<EventHandlerValue> exactListeners = listenersMap.get(exactKey);
                if (ObjUtil.isNotEmpty(exactListeners)) {
                    handlerForTarget(eventData, exactListeners);
                }

                // 修复：只有当目标是全局广播时，才触发全局监听器，避免同一事件在单服务内重复执行
                if (EventConstant.TARGET_ALL_X.equals(targetApp)) {
                    EventHandlerKey allKey = new EventHandlerKey().setEventName(eventName).setApplicationName(EventConstant.TARGET_ALL_X);
                    List<EventHandlerValue> allListeners = listenersMap.get(allKey);
                    if (ObjUtil.isNotEmpty(allListeners)) {
                        handlerForTarget(eventData, allListeners);
                    }
                }
            }
        } finally {
            // 确保 ThreadLocal 被清理
            EventThreadLocalUtils.clear();
        }
    }

    /**
     * 针对特定目标执行监听器列表
     * <p>优化：使用 ObjectReader 缓存提升反序列化性能，避免每次创建 Reader</p>
     *
     * @param eventData    事件数据
     * @param listenerList 监听器列表
     */
    private void handlerForTarget(EventData eventData, List<EventHandlerValue> listenerList) {
        if (ObjUtil.isEmpty(listenerList)) {
            return;
        }

        for (EventHandlerValue evenHandler : listenerList) {
            try {
                Class<?> paramType = evenHandler.getAClass();
                // 从缓存获取或创建 ObjectReader
                ObjectReader reader = readerCache.computeIfAbsent(paramType, SerializeUtils::readerFor);
                Object paramObj = reader.readValue(eventData.getMsgData());

                // 使用 MethodHandle 高效调用，性能接近原生方法调用
                evenHandler.getMethodHandle().invoke(paramObj);
            } catch (Throwable e) {
                log.error("执行事件处理器失败, 事件名称: {}, 处理器: {}",
                          eventData.getEventName(),
                          evenHandler.getMethodForDebug() != null ? evenHandler.getMethodForDebug().getName() : "unknown", e);
                // 继续执行其他监听器，不中断
            }
        }
    }

    /**
     * 对方法进行通用操作（注册或取消注册）
     *
     * @param method   目标方法
     * @param consumer 具体操作函数
     * @param bean     指定的 Bean 实例（注册时提供，注销时可为 null）
     */
    protected void mapOperation(Method method, EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer, Object bean) {
        EventHandler annotation = method.getAnnotation(EventHandler.class);
        if (annotation == null) {
            return;
        }

        String[] applicationName = annotation.applicationName();
        String eventName = annotation.value();
        Parameter[] parameters = method.getParameters();
        AssertUtils.isTrue(ObjUtil.isNotEmpty(parameters) && parameters.length == 1, "EventHandler标记的方法必须且只能有一个参数对象");
        Class<?> type = parameters[0].getType();
        if (StrUtil.isBlank(eventName)) {
            eventName = type.getSimpleName();
        }

        // 创建高效的 MethodHandle 调用句柄
        MethodHandle methodHandle = createMethodHandle(method, bean);
        if (methodHandle == null) {
            log.error("创建 MethodHandle 失败，事件处理器注册失败: {}", method);
            throw new IllegalStateException("创建 MethodHandle 失败: " + method);
        }

        EventHandlerValue value = new EventHandlerValue()
                .setAClass(type)
                .setMethodHandle(methodHandle)
                .setBean(bean)
                .setMethodForDebug(method); // 保留原始 Method 对象，用于注销和日志

        for (String appName : applicationName) {
            String resolvedAppName = EventConstant.THIS_MACHINE.equals(appName) ? applicationProperties.getName() : appName;

            EventHandlerKey key = new EventHandlerKey().setEventName(eventName).setApplicationName(resolvedAppName);

            consumer.handler(key, value);
        }
    }

    /**
     * 执行注册的核心逻辑
     */
    private void doRegister(String beanName, Method method, Object bean) {
        EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer = (eventHandlerKey, eventHandlerValue) ->
                listenersMap.computeIfAbsent(eventHandlerKey, k -> new CopyOnWriteArrayList<>()).add(eventHandlerValue);
        mapOperation(method, consumer, bean);
    }

    /**
     * 使用 MethodHandle 创建高效的方法调用句柄（Java 17 优化）
     * <p>修复：简化降级逻辑，确保私有方法可访问，并保留终极反射降级方案</p>
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
            // 绑定 bean 实例（相当于 this 对象）
            MethodHandle boundHandle = handle.bindTo(bean);
            // 调整方法类型为 (Object) -> void，方便统一调用
            return boundHandle.asType(MethodType.methodType(void.class, Object.class));
        } catch (IllegalAccessException e) {
            // 降级方案：使用纯反射包装 MethodHandle（兼容性最强）
            log.warn("MethodHandle 创建失败，降级为反射调用: {}", method, e);
            return createReflectionOnlyMethodHandle(method, bean);
        } catch (Throwable e) {
            log.error("MethodHandle 创建异常，使用纯反射降级方案: {}", method, e);
            return createReflectionOnlyMethodHandle(method, bean);
        }
    }

    /**
     * 终极降级方案：纯反射调用包装为 MethodHandle
     * <p>当所有 MethodHandle 创建尝试均失败时使用，确保事件处理器在极端环境下仍可工作</p>
     *
     * @param method 目标方法
     * @param bean   目标 Bean 实例
     * @return MethodHandle 调用句柄
     */
    @SneakyThrows
    private MethodHandle createReflectionOnlyMethodHandle(Method method, Object bean) {
        method.setAccessible(true);
        // 使用反射 MethodHandle 构造一个调用点
        MethodHandle target = MethodHandles.lookup().unreflect(method).bindTo(bean);
        return target.asType(MethodType.methodType(void.class, Object.class));
    }

    /**
     * 执行具体的事件监听方法（保留原有方法签名，用于兼容）
     *
     * @param eventData            事件数据
     * @param evenHandlerAllValues 监听器列表
     * @deprecated 建议使用 {@link #handlerForTarget(EventData, List)} 以获得更好的缓存性能
     */
    @Deprecated
    protected void handler(EventData eventData, List<EventHandlerValue> evenHandlerAllValues) {
        handlerForTarget(eventData, evenHandlerAllValues);
    }
}
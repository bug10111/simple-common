package com.simple.common.eventbus.manager;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.entity.EventHandlerKey;
import com.simple.common.eventbus.common.entity.EventHandlerValue;
import com.simple.common.eventbus.common.function.EventHandlerFunction;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
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
import java.util.HashMap;
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

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * 注册事件监听器
     *
     * @param method 带有 @EventHandler 注解的方法
     */
    @Override
    public void register(Method method) {
        EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer = (eventHandlerKey, eventHandlerValue) -> listenersMap.computeIfAbsent(eventHandlerKey,
                                                                                                                                                 k -> new CopyOnWriteArrayList<>())
                                                                                                                                .add(eventHandlerValue);
        mapOperation(method, consumer);
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
        mapOperation(method, consumer);
    }

    /**
     * 处理事件
     *
     * @param event 事件对象（必须为EventData类型）
     */
    @Override
    public void handler(Object event) {
        if (!(event instanceof EventData eventData)) {
            log.error("事件处理器只接受EventData类型，当前类型: {}", event.getClass().getName());
            return;
        }

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

            // 同时匹配监听所有系统的监听器（TARGET_ALL_X），但避免重复处理当targetApp就是TARGET_ALL_X时
            if (!EventConstant.TARGET_ALL_X.equals(targetApp)) {
                EventHandlerKey allKey = new EventHandlerKey().setEventName(eventName).setApplicationName(EventConstant.TARGET_ALL_X);
                List<EventHandlerValue> allListeners = listenersMap.get(allKey);
                if (ObjUtil.isNotEmpty(allListeners)) {
                    handlerForTarget(eventData, allListeners);
                }
            }
        }
    }

    /**
     * 针对特定目标执行监听器列表
     *
     * @param eventData    事件数据
     * @param listenerList 监听器列表
     */
    private void handlerForTarget(EventData eventData, List<EventHandlerValue> listenerList) {
        if (ObjUtil.isEmpty(listenerList)) {
            return;
        }

        // 优化：只反序列化一次并缓存结果（按类型）
        Map<Class<?>, Object> deserializedCache = new HashMap<>();

        for (EventHandlerValue evenHandler : listenerList) {
            try {
                Class<?> paramType = evenHandler.getAClass();

                // 从缓存获取或反序列化
                Object paramObj = deserializedCache.computeIfAbsent(paramType, clazz -> JsonUtils.toJsonObj(eventData.getMsgData(), clazz));

                // 使用 MethodHandle 高效调用，性能接近原生方法调用
                evenHandler.getMethodHandle().invoke(paramObj);
            } catch (Throwable e) {
                log.error("执行事件处理器失败, 事件名称: {}, 处理器: {}", eventData.getEventName(),
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
     */
    protected void mapOperation(Method method, EventHandlerFunction<EventHandlerKey, EventHandlerValue> consumer) {
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

        Class<?> declaringClass = method.getDeclaringClass();
        Object bean;
        try {
            bean = applicationContext.getBean(declaringClass);
        } catch (Exception e) {
            log.error("获取事件处理器Bean失败: {}，该方法将被忽略", declaringClass.getName(), e);
            throw new IllegalStateException("无法获取事件处理器Bean: " + declaringClass.getName(), e);
        }

        // 创建高效的 MethodHandle 调用句柄
        MethodHandle methodHandle = createMethodHandle(method, bean);

        EventHandlerValue value = new EventHandlerValue().setAClass(type).setMethodHandle(methodHandle).setBean(bean).setMethodForDebug(method); // 保留原始 Method 对象，用于注销和日志

        for (String appName : applicationName) {
            String resolvedAppName = EventConstant.THIS_MACHINE.equals(appName) ? applicationProperties.getName() : appName;

            EventHandlerKey key = new EventHandlerKey().setEventName(eventName).setApplicationName(resolvedAppName);

            consumer.handler(key, value);
        }
    }

    /**
     * 使用 MethodHandle 创建高效的方法调用句柄（Java 17 优化）
     *
     * @param method 目标方法
     * @param bean   目标 Bean 实例
     * @return MethodHandle 调用句柄，签名适配为 (Object) -> void
     */
    private MethodHandle createMethodHandle(Method method, Object bean) {
        try {
            // 确保方法可访问（绕过 Java 访问控制检查）
            method.setAccessible(true);

            MethodHandles.Lookup lookup = MethodHandles.lookup();
            // 将反射 Method 转换为 MethodHandle
            MethodHandle handle = lookup.unreflect(method);
            // 绑定 bean 实例（相当于 this 对象）
            MethodHandle boundHandle = handle.bindTo(bean);
            // 调整方法类型为 (Object) -> void，方便统一调用
            return boundHandle.asType(MethodType.methodType(void.class, Object.class));
        } catch (IllegalAccessException e) {
            // 降级方案：使用反射调用（理论上不会发生，但保留作为兜底）
            log.warn("MethodHandle 创建失败，降级为反射调用: {}", method, e);
            return createFallbackMethodHandle(method, bean);
        }
    }

    /**
     * 降级方案：基于反射的 MethodHandle 包装器
     *
     * @param method 目标方法
     * @param bean   目标 Bean 实例
     * @return MethodHandle 调用句柄
     */
    @SneakyThrows
    private MethodHandle createFallbackMethodHandle(Method method, Object bean) {
        return MethodHandles.lookup()
                            .findVirtual(method.getDeclaringClass(), method.getName(), MethodType.methodType(method.getReturnType(), method.getParameterTypes()))
                            .bindTo(bean)
                            .asType(MethodType.methodType(void.class, Object.class));
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
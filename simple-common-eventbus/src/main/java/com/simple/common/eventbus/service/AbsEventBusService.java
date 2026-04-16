package com.simple.common.eventbus.service;

import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 发送事件的抽象方法
 *
 * @author qty
 */
@Slf4j
public abstract class AbsEventBusService implements EventBusService {

    @Autowired
    private ApplicationProperties applicationProperties;

    /**
     * 注解缓存，避免重复反射
     */
    private final Map<Class<?>, Event> eventAnnotationCache = new ConcurrentHashMap<>();

    /**
     * 应用名称缓存，避免重复获取
     */
    private volatile String cachedApplicationName;

    /**
     * 处理普通事件发送
     *
     * @param eventData 事件数据
     */
    protected abstract void handler(EventData eventData);

    /**
     * 处理延迟事件发送
     *
     * @param eventData 事件数据
     * @param time      延迟时间
     * @param timeUnit  时间单位
     */
    protected abstract void handler(EventData eventData, int time, TimeUnit timeUnit);

    /**
     * 发布普通事件
     *
     * @param event 事件对象（必须带有@Event注解）
     */
    @Override
    public void push(Object event) {
        EventData eventData = getEvenData(event);
        handler(eventData);
    }

    /**
     * 发布延迟事件
     *
     * @param event    事件对象
     * @param time     延迟时间
     * @param timeUnit 时间单位
     */
    @Override
    public void push(Object event, int time, TimeUnit timeUnit) {
        EventData eventData = getEvenData(event);
        handler(eventData, time, timeUnit);
    }

    /**
     * 将对象转换为指定事件类型后发布
     *
     * @param obj        源对象
     * @param eventClass 事件类型
     */
    @Override
    public void push(Object obj, Class<?> eventClass) {
        Object event = BeanUtils.copyProperties(obj, eventClass);
        push(event);
    }

    /**
     * 将对象转换为指定事件类型后延迟发布
     *
     * @param obj        源对象
     * @param eventClass 事件类型
     * @param time       延迟时间
     * @param timeUnit   时间单位
     */
    @Override
    public void push(Object obj, Class<?> eventClass, int time, TimeUnit timeUnit) {
        Object event = BeanUtils.copyProperties(obj, eventClass);
        push(event, time, timeUnit);
    }

    /**
     * 根据事件对象构建EventData
     * <p>优化：若 targets 中包含广播目标(TARGET_ALL_X)，则忽略其他单播目标，避免本机重复消费</p>
     *
     * @param event 事件对象
     * @return EventData
     */
    protected EventData getEvenData(Object event) {
        AssertUtils.notNull(event, "事件对象不能为空");

        Class<?> aClass = event.getClass();
        Event annotation = eventAnnotationCache.computeIfAbsent(aClass, clazz -> clazz.getAnnotation(Event.class));
        AssertUtils.notEmpty(annotation, "发布的事件必须带注解@Event");

        String eventName = annotation.value();
        if (!StringUtils.hasLength(eventName)) {
            eventName = aClass.getSimpleName();
        }

        // 双重检查锁正确写法
        if (cachedApplicationName == null) {
            synchronized (this) {
                if (cachedApplicationName == null) {
                    cachedApplicationName = applicationProperties.getName();
                }
            }
        }

        // 解析并优化目标列表
        List<String> rawTargets = Arrays.asList(annotation.targets());
        List<String> optimizedTargets = optimizeTargets(rawTargets, cachedApplicationName);

        String jsonData = JsonUtils.toJsonStr(event);
        EventData eventData = new EventData();
        eventData.setEventName(eventName);
        eventData.setApplicationName(cachedApplicationName);
        eventData.setObjectivesApplication(optimizedTargets);
        eventData.setMsgData(jsonData);

        log.trace("发送事件消息：[{}]", JsonUtils.toJsonStr(eventData));
        return eventData;
    }

    /**
     * 优化事件目标列表：若包含广播目标，则仅保留广播目标（因为广播已覆盖所有服务）
     * 同时将 THIS_MACHINE 占位符替换为实际应用名
     *
     * @param rawTargets     原始目标数组
     * @param applicationName 当前应用名称
     * @return 优化后的目标列表（不可变）
     */
    private List<String> optimizeTargets(List<String> rawTargets, String applicationName) {
        Set<String> targetSet = new LinkedHashSet<>(); // 保持顺序，自动去重

        boolean hasAll = false;
        for (String target : rawTargets) {
            String resolved = target;
            if (EventConstant.THIS_MACHINE.equals(target)) {
                resolved = applicationName;
            }
            if (EventConstant.TARGET_ALL_X.equals(resolved)) {
                hasAll = true;
            }
            targetSet.add(resolved);
        }

        // 若包含广播目标，则仅保留广播目标（因为广播消息会发送到所有绑定队列，包括本机）
        if (hasAll) {
            return Collections.singletonList(EventConstant.TARGET_ALL_X);
        }

        return new ArrayList<>(targetSet);
    }
}
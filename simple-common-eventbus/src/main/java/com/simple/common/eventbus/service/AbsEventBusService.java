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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        List<String> targets = new ArrayList<>(annotation.targets().length);
        for (String string : annotation.targets()) {
            if (EventConstant.THIS_MACHINE.equals(string)) {
                string = cachedApplicationName;
            }
            targets.add(string);
        }

        String jsonData = JsonUtils.toJsonStr(event);
        EventData eventData = new EventData();
        eventData.setEventName(eventName);
        eventData.setApplicationName(cachedApplicationName);
        eventData.setObjectivesApplication(targets);
        eventData.setMsgData(jsonData);

        log.trace("发送事件消息：[{}]", JsonUtils.toJsonStr(eventData));
        return eventData;
    }
}
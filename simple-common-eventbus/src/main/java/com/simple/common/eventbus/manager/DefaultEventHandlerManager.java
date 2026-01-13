package com.simple.common.eventbus.manager;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.constants.EventConstant;
import com.simple.common.eventbus.common.entity.EvenHandlerValue;
import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.entity.EventHandlerKey;
import com.simple.common.eventbus.common.function.EventHandlerFunction;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultEventHandlerManager implements EventHandlerManager {

    private final ConcurrentHashMap<EventHandlerKey, List<EvenHandlerValue>> listenersMap = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public void register(Method method) {
        EventHandlerFunction<EventHandlerKey, EvenHandlerValue> consumer = (eventHandlerKey, evenHandlerValue) -> listenersMap.computeIfAbsent(eventHandlerKey,
                                                                                                                                               k -> new CopyOnWriteArrayList<>())
                                                                                                                              .add(evenHandlerValue);
        mapOperation(method, consumer);
    }

    @Override
    public void unregister(Method method) {
        EventHandlerFunction<EventHandlerKey, EvenHandlerValue> consumer = (eventHandlerKey, evenHandlerValue) -> listenersMap.remove(eventHandlerKey);
        mapOperation(method, consumer);
    }

    @Override
    @SneakyThrows
    public void handler(Object event) {
        EventData eventData = (EventData) event;
        EventHandlerKey key = new EventHandlerKey().setEventName(eventData.getEventName()).setApplicationName(eventData.getObjectives());

        //获取事件执行方法
        List<EvenHandlerValue> evenHandlerValues = listenersMap.get(key);
        if (ObjUtil.isNotEmpty(evenHandlerValues)) handler(eventData, evenHandlerValues);

        //追加存在监听所有系统的实现
        if (!EventConstant.TARGET_ALL_X.equals(eventData.getApplicationName())) {
            List<EvenHandlerValue> evenHandlerAllValues = listenersMap.get(key.setApplicationName(EventConstant.TARGET_ALL_X));
            if (ObjUtil.isNotEmpty(evenHandlerAllValues)) handler(eventData, evenHandlerAllValues);
        }

    }

    protected void handler(EventData eventData, List<EvenHandlerValue> evenHandlerAllValues) throws IllegalAccessException, InvocationTargetException {
        for (EvenHandlerValue evenHandler : evenHandlerAllValues) {

            //获取执行方法相关
            Method method = evenHandler.getMethod();
            var bean = evenHandler.getBean();

            //获取请求参数
            var obj = JsonUtils.toJsonObj(eventData.getMsgData(), evenHandler.getAClass());

            //执行请求
            method.invoke(bean, obj);
        }
    }

    protected void mapOperation(Method method, EventHandlerFunction<EventHandlerKey, EvenHandlerValue> consumer) {
        EventHandler annotation = method.getAnnotation(EventHandler.class);

        //获取服务名称
        String[] applicationName = annotation.applicationName();

        //获取事件
        String eventName = annotation.value();
        Parameter[] parameters = method.getParameters();
        AssertUtils.isTrue(ObjUtil.isNotEmpty(parameters) && parameters.length == 1, "EventHandler标记的方法必须且只能只有一个参数对象");
        Class<?> type = method.getParameters()[0].getType();
        if (StrUtil.isBlank(eventName)) {
            eventName = type.getSimpleName();
        }

        for (String appName : applicationName) {
            if (appName.equals(EventConstant.THIS_MACHINE)) {
                appName = applicationProperties.getName();
            }
            EventHandlerKey key = new EventHandlerKey().setEventName(eventName).setApplicationName(appName);

            //获取方法bean，多个实例的时候任意取一个执行
            var declaringClass = method.getDeclaringClass();
            var bean = applicationContext.getBean(declaringClass);
            EvenHandlerValue value = new EvenHandlerValue().setMethod(method).setBean(bean).setAClass(type);
            consumer.handler(key, value);
        }
    }
}

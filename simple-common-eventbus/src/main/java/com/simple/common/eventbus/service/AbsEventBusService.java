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
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 发送事件的抽象方法
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public abstract class AbsEventBusService implements EventBusService {

    @Autowired
    private ApplicationProperties applicationProperties;

    protected abstract void handler(EventData eventData);

    protected abstract void handler(EventData eventData, int time, TimeUnit timeUnit);

    @Override
    public void push(Object event) {
        EventData eventData = getEvenData(event);
        handler(eventData);
    }

    @Override
    public void push(Object event, int time, TimeUnit timeUnit) {
        EventData eventData = getEvenData(event);
        handler(eventData, time, timeUnit);
    }

    @Override
    public void push(Object obj, Class<?> eventClass) {
        Object event = BeanUtils.copyProperties(obj, eventClass);
        push(event);
    }

    @Override
    public void push(Object obj, Class<?> eventClass, int time, TimeUnit timeUnit) {
        Object event = BeanUtils.copyProperties(obj, eventClass);
        push(event, time, timeUnit);
    }

    protected EventData getEvenData(Object event) {
        Class<?> aClass = event.getClass();
        Event annotation = aClass.getAnnotation(Event.class);
        AssertUtils.notEmpty(annotation, "发布的事件必须带注解@Event");

        //获取事件名称
        String eventName = annotation.value();
        if (!StringUtils.hasLength(eventName)) {
            eventName = aClass.getSimpleName();
        }

        //获取发送目标服务
        List<String> targets = new ArrayList<>();
        for (String string : annotation.targets()) {

            //如果是本机，获取本机交换机
            if (string.equals(EventConstant.THIS_MACHINE)) {
                string = applicationProperties.getName();
            }

            targets.add(string);
        }

        //构建消息体
        EventData eventData = (EventData) new EventData().setEventName(eventName)
                                                         .setApplicationName(applicationProperties.getName())
                                                         .setObjectivesApplication(targets)
                                                         .setMsgData(JsonUtils.toJsonStr(event));
        log.trace("发送事件消息：[{}]", JsonUtils.toJsonStr(eventData));
        return eventData;
    }
}

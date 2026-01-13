package com.simple.common.eventbus.service;

import com.simple.common.eventbus.common.entity.EventData;
import com.simple.common.eventbus.common.manager.EventHandlerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 同步事件执行器
 *
 * @author qty
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "simple.event", name = "type", havingValue = "sync")
public class SyncEventBusService extends AbsEventBusService {

    @Autowired
    private EventHandlerManager eventHandlerManager;

    @Override
    protected void handler(EventData eventData) {
        eventHandlerManager.handler(eventData);
    }

    @Override
    protected void handler(EventData eventData, int time, TimeUnit timeUnit) {
        eventHandlerManager.handler(eventData);
        log.error("同步事件执行器不支持延迟执行，已同步执行！");
    }
}

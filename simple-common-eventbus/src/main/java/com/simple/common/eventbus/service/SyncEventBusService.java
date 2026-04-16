package com.simple.common.eventbus.service;

import com.simple.common.core.utils.ThreadUtils;
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
        if (eventData == null) {
            log.error("事件数据为空");
            return;
        }
        eventHandlerManager.handler(eventData);
    }

    /**
     * 处理延迟事件（同步模式下使用调度线程池）
     * <p>修复：当 time <= 0 时，视为立即执行，避免事件被丢弃</p>
     *
     * @param eventData 事件数据
     * @param time      延迟时间
     * @param timeUnit  时间单位
     */
    @Override
    protected void handler(EventData eventData, int time, TimeUnit timeUnit) {
        if (eventData == null) {
            log.error("事件数据为空");
            return;
        }

        if (time > 0) {
            ThreadUtils.schedule(() -> eventHandlerManager.handler(eventData), time, timeUnit);
            log.debug("同步事件执行器处理了延迟事件，延迟时间: {} {}", time, timeUnit);
        } else {
            // 修复：time <= 0 时立即执行，符合语义预期
            eventHandlerManager.handler(eventData);
            log.debug("同步事件执行器立即执行事件（延迟时间非正）");
        }
    }
}
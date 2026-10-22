package com.simple.common.eventbus.service;

import com.simple.common.core.utils.AssertUtils;
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
     * <p>延迟时间非正时视为立即执行，避免事件被丢弃</p>
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
            // 调度线程池对 null 时间单位直接抛 NPE 且异常挂在无人取回的 Future 上，此处先快速失败给出可读信息
            AssertUtils.notNull(timeUnit, "延迟事件的 timeUnit 不能为空");
            ThreadUtils.schedule(() -> {
                try {
                    eventHandlerManager.handler(eventData);
                } catch (Throwable e) {
                    // 延迟任务在调度线程执行，异常只会挂到未取回的 ScheduledFuture 上，必须就地记录，
                    // 否则延迟事件被静默丢弃，与立即执行路径的失败可见性不一致
                    log.error("同步事件执行器执行延迟事件失败, 事件名称: {}", eventData.getEventName(), e);
                }
            }, time, timeUnit);
            log.debug("同步事件执行器已调度延迟事件，延迟时间: {} {}", time, timeUnit);
        } else {
            // 延迟时间非正时立即执行，符合延迟调度语义预期
            eventHandlerManager.handler(eventData);
            log.debug("同步事件执行器立即执行事件（延迟时间非正）");
        }
    }
}
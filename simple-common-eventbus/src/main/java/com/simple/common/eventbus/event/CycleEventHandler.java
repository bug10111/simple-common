package com.simple.common.eventbus.event;

import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.eventbus.common.event.CycleEvent;
import com.simple.common.eventbus.common.service.AbsEventCycleService;
import com.simple.common.eventbus.util.CycleFactoryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 自动重试任务的事件实现
 *
 * @author qty
 */

@Slf4j
@Component
public class CycleEventHandler {

    @EventHandler
    public void on(CycleEvent event) {
        AbsEventCycleService<?> cycleService = CycleFactoryUtils.getCycleService(event.getBeanName());
        if (cycleService == null) {
            log.error("没有找到Cycle有效实现，beanName: {}", event.getBeanName());
        } else {
            cycleService.execution(event);
        }
    }

}
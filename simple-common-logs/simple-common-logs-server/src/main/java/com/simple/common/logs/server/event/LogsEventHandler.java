package com.simple.common.logs.server.event;

import com.simple.common.eventbus.common.annotation.EventHandler;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: 日志事件实现
 *
 * @author qty
 */
@Component
public class LogsEventHandler {

    @Autowired
    private LogsSaveManager logsSaveManager;

    @EventHandler
    public void create(LogDataEvent event) {
        logsSaveManager.save(event);
    }

}

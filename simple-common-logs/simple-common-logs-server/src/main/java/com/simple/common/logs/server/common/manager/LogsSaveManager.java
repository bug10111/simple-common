package com.simple.common.logs.server.common.manager;

import com.simple.common.logs.client.common.event.LogDataEvent;

/**
 * Created with IntelliJ IDEA
 * Description: 日志操作
 *
 * @author qty
 */
public interface LogsSaveManager {

    /**
     * 添加日志到缓存区
     */
    void save(LogDataEvent event);

    /**
     * 持久化
     */
    void processLogs();

}

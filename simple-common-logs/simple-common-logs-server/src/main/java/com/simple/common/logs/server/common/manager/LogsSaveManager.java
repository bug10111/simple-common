package com.simple.common.logs.server.common.manager;

import com.simple.common.logs.client.common.event.LogDataEvent;

/**
 * 日志保存管理器接口
 *
 * @author qty
 */
public interface LogsSaveManager {

    /**
     * 添加日志数据到队列
     */
    void addLogData(LogDataEvent logDataEvent);

    /**
     * 批量保存日志
     */
    void processLogs();

    /**
     * 获取队列大小
     */
    int getQueueSize();
}
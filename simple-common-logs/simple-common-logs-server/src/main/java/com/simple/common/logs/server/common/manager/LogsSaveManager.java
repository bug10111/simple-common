package com.simple.common.logs.server.common.manager;

import com.simple.common.logs.client.common.event.LogDataEvent;

/**
 * 日志保存管理器接口
 *
 * @author qty
 */
public interface LogsSaveManager {

    /**
     * 保存日志
     *
     * @param logDataEvent 日志数据事件
     */
    void saveLog(LogDataEvent logDataEvent);

    /**
     * 处理日志队列中的日志
     */
    void processLogs();

    /**
     * 获取队列大小
     *
     * @return 队列大小
     */
    int getQueueSize();
}
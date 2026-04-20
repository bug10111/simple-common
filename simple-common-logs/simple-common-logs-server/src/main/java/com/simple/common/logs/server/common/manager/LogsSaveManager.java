package com.simple.common.logs.server.common.manager;

import com.simple.common.logs.proto.common.event.LogDataEvent;

import java.util.List;

/**
 * 日志保存管理器接口。
 * <p>
 * 用于管理日志数据的接收、队列存储和持久化处理。
 * 默认抽象实现 {@link com.simple.common.logs.server.manager.AbsLogsSaveManager} 提供了
 * 日志队列管理和批量处理的框架，具体持久化逻辑需继承并重写 {@code saveLogsToStorage} 方法。
 * </p>
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
     * 批量保存日志
     * <p>
     * 服务端接收到批量日志后，应优先使用此方法进行批量处理，
     * 以减少入队次数和提升吞吐量。
     * </p>
     *
     * @param events 日志数据事件列表
     */
    void saveLogBatch(List<LogDataEvent> events);

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
package com.simple.common.logs.server.common.manager;

import com.simple.common.logs.proto.common.event.LogDataEvent;

import java.util.List;

/**
 * 日志保存管理器接口。
 * <p>
 * 用于管理日志数据的接收、队列存储和持久化处理。
 * 默认抽象实现 {@link com.simple.common.logs.server.manager.AbsLogsSaveManager} 提供了
 * 日志队列管理和批量处理的框架，具体持久化逻辑需继承并重写 {@code persistence(List<LogDataEvent>)} 方法
 * （WAL 重放可能多次调用持久化，实现必须保证幂等）。
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
     * @param events 日志数据事件列表；null 或空列表应被安全忽略，列表元素不允许为 null
     */
    void saveLogBatch(List<LogDataEvent> events);

    /**
     * 处理日志队列中的日志
     * <p>
     * 该方法为非阻塞的尽力而为式消费：单次调用不保证清空队列；
     * 可能被调度线程、停机流程等多线程并发调用，
     * 实现必须保证同一批次不会被重复持久化。
     * </p>
     */
    void processLogs();

    /**
     * 获取队列大小
     *
     * @return 队列大小
     */
    int getQueueSize();
}
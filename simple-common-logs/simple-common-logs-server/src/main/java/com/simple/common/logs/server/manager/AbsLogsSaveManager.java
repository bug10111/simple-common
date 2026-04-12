package com.simple.common.logs.server.manager;

import com.simple.common.core.common.service.thread.ThreadService;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * BlockingQueue常见用法
 * <p>
 * 插入元素：
 * add(E e)：添加元素，如果队列满，抛出异常。
 * offer(E e)：尝试插入元素，如果队列满，则返回 false。
 * put(E e)：插入元素，如果队列满，阻塞等待。
 * <p>
 * 移除元素：
 * remove()：删除并返回队列头元素，如果队列空则抛出异常。
 * poll()：尝试移除队列头元素，如果队列为空返回 null。
 * take()：删除并返回队列头元素，如果队列为空，阻塞等待。
 * <p>
 * 检查元素：
 * peek()：查看队列头元素但不移除，若队列空则返回 null。
 *
 * @author qty
 */
@Slf4j
public abstract class AbsLogsSaveManager implements LogsSaveManager, InitializingBean {

    protected abstract LogTcpServerProperties getProperties();

    protected abstract ThreadService getThreadService();

    /**
     * 日志数据队列
     */
    private final LinkedBlockingQueue<LogDataEvent> logQueue = new LinkedBlockingQueue<>(50000);

    @Override
    public void saveLog(LogDataEvent logDataEvent) {
        try {
            boolean success = logQueue.offer(logDataEvent);
            if (!success) {
                log.warn("日志队列已满，丢弃日志: {}", logDataEvent.getTraceId());
            } else {
                log.debug("添加日志到队列，当前队列大小: {}", logQueue.size());
            }
        } catch (Exception e) {
            log.error("添加日志到队列失败", e);
        }
    }

    @Override
    public void processLogs() {
        List<LogDataEvent> logsToSave = new ArrayList<>();
        logQueue.drainTo(logsToSave, getProperties().getBatchSize());
        if (!logsToSave.isEmpty()) {
            persistence(logsToSave);
        }
    }

    @Override
    public int getQueueSize() {
        return logQueue.size();
    }

    /**
     * 持久化，可根据需求选择持久化到哪里
     *
     * @param logsToSave 数据集合
     */
    protected abstract void persistence(List<LogDataEvent> logsToSave);

    @Override
    public void afterPropertiesSet() {
        getThreadService().scheduleWithFixedDelay(() -> {
            try {
                processLogs();
            } catch (Exception e) {

                // 处理异常，记录日志等
                log.error("任务执行失败: {}", e.getMessage());
            }

        }, getProperties().getBatchInterval(), TimeUnit.SECONDS);
    }
}

package com.simple.common.logs.server.manager;

import com.simple.common.core.common.service.thread.ThreadService;
import com.simple.common.core.utils.BeanUtils;
import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogsServerProperties;
import com.simple.common.logs.server.common.view.SysOperationLogsView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
@Component
public class DefaultLogsSaveManager implements LogsSaveManager, InitializingBean {

    private final LinkedBlockingQueue<SysOperationLogs> queue = new LinkedBlockingQueue<>();

    @Autowired
    private LogsServerProperties logsServerProperties;

    @Autowired
    private SysOperationLogsView sysOperationLogsView;

    @Autowired
    private ThreadService threadService;

    @Override
    public void save(LogDataEvent event) {
        queue.offer(BeanUtils.copyProperties(event, SysOperationLogs.class));
    }

    @Override
    public void processLogs() {
        List<SysOperationLogs> logsToSave = new ArrayList<>();
        queue.drainTo(logsToSave, logsServerProperties.getSaveSum());
        persistence(logsToSave);
    }

    /**
     * 持久化，可根据需求选择持久化到哪里
     *
     * @param logsToSave 数据集合
     */
    protected void persistence(List<SysOperationLogs> logsToSave) {
        if (!logsToSave.isEmpty()) {
            sysOperationLogsView.saves(logsToSave);
            if (log.isDebugEnabled()) {
                log.debug("批量保存日志到PG，成功[{}]条", logsToSave.size());
            }
        }
    }

    @Override
    public void afterPropertiesSet() {
        threadService.scheduleWithFixedDelay(() -> {
            try {
                processLogs();
            } catch (Exception e) {

                // 处理异常，记录日志等
                log.error("任务执行失败: {}", e.getMessage());
            }

        }, logsServerProperties.getTime(), TimeUnit.SECONDS);
    }
}

package com.simple.common.logs.server.manager;

import com.simple.common.logs.client.common.event.LogDataEvent;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import com.simple.common.logs.server.common.service.SysOperationLogsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 默认日志保存管理器
 * 使用内存队列批量保存日志
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultLogsSaveManager implements LogsSaveManager {

    @Autowired
    private SysOperationLogsService sysOperationLogsService;

    @Autowired
    private LogTcpServerProperties properties;

    /**
     * 日志数据队列
     */
    private final LinkedBlockingQueue<LogDataEvent> logQueue = new LinkedBlockingQueue<>(10000);

    @Override
    public void addLogData(LogDataEvent logDataEvent) {
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

    /**
     * 定时批量保存日志
     * 默认每5秒执行一次
     */
    @Scheduled(fixedRateString = "${simple.logs.tcp.server.batch-interval:5000}")
    public void scheduledProcessLogs() {
        processLogs();
    }

    @Override
    public void processLogs() {
        int batchSize = properties.getBatchSize();
        List<LogDataEvent> batchList = new ArrayList<>(batchSize);
        logQueue.drainTo(batchList, batchSize);

        if (batchList.isEmpty()) {
            return;
        }

        log.info("开始批量保存日志，数量: {}", batchList.size());

        try {
            // 转换为实体列表
            List<SysOperationLogs> logsList = new ArrayList<>();
            for (LogDataEvent event : batchList) {
                // 转换为实体对象
                SysOperationLogs logs = SysOperationLogs.fromLogDataEvent(event);
                
                log.debug("保存日志: id={}, operName={}, method={}, status={}", 
                        logs.getId(), logs.getOperName(), logs.getMethod(), logs.getStatus());
                logsList.add(logs);
            }

            // 批量保存
            sysOperationLogsService.batchSave(logsList);
            log.info("批量保存日志成功，数量: {}", logsList.size());
        } catch (Exception e) {
            log.error("批量保存日志失败", e);
        }
    }

    @Override
    public int getQueueSize() {
        return logQueue.size();
    }

    /**
     * 将LogDataEvent转换为SysOperationLogs实体
     */
    private SysOperationLogs convertToEntity(LogDataEvent event) {
        SysOperationLogs logs = new SysOperationLogs();
        if (event.getTraceId() != null) {
            logs.setTraceId(event.getTraceId());
        }
        logs.setOperIp(event.getOperIp());
        logs.setMethod(event.getMethod());
        logs.setOperUrl(event.getOperUrl());
        logs.setOperName(event.getOperName());
        logs.setOperParam(event.getOperParam());
        logs.setOperResult(event.getOperResult());
        logs.setErrorMessage(event.getErrorMessage());
        logs.setOperTime(event.getOperTime());
        logs.setUserId(event.getUserId());
        logs.setUserName(event.getUserName());
        logs.setDeptId(event.getDeptId());
        logs.setDeptName(event.getDeptName());
        logs.setRequestMethod(event.getRequestMethod());
        logs.setOperType(event.getOperType());
        return logs;
    }

}
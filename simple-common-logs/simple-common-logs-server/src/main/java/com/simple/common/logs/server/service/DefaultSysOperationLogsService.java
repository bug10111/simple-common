package com.simple.common.logs.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simple.common.logs.server.common.dto.CreateSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.SysOperationLogsInfoResponse;
import com.simple.common.logs.server.common.dto.SysOperationLogsPageResponse;
import com.simple.common.logs.server.common.dto.UpdateSysOperationLogsRequest;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.service.SysOperationLogsService;
import com.simple.common.logs.server.common.view.SysOperationLogsView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志(sys_operation_logs)默认接口实现
 *
 * @author qty
 */
@Slf4j
@Service
@Transactional
class DefaultSysOperationLogsService implements SysOperationLogsService {

    @Autowired
    private SysOperationLogsView sysOperationLogsView;

    @Override
    public void batchSave(List<SysOperationLogs> logsList) {
        if (logsList == null || logsList.isEmpty()) {
            return;
        }
        sysOperationLogsView.saves(logsList);
        log.info("批量保存日志成功，数量: {}", logsList.size());
    }

    @Override
    public IPage<SysOperationLogsPageResponse> findAll(FindAllSysOperationLogsRequest findAllRequest) {
        IPage<SysOperationLogs> page = sysOperationLogsView.findAll(findAllRequest);
        // 转换为响应对象
        IPage<SysOperationLogsPageResponse> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        responsePage.setRecords(page.getRecords().stream()
                .map(this::convertToPageResponse)
                .collect(Collectors.toList()));
        return responsePage;
    }

    @Override
    public SysOperationLogsInfoResponse findById(String id) {
        SysOperationLogs logs = sysOperationLogsView.findById(id);
        if (logs == null) {
            return null;
        }
        return convertToInfoResponse(logs);
    }

    @Override
    public String save(CreateSysOperationLogsRequest createRequest) {
        SysOperationLogs logs = convertFromCreateRequest(createRequest);
        sysOperationLogsView.save(logs);
        return logs.getId();
    }

    @Override
    public String updateById(UpdateSysOperationLogsRequest updateRequest) {
        SysOperationLogs logs = convertFromUpdateRequest(updateRequest);
        sysOperationLogsView.updateById(logs);
        return logs.getId();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysOperationLogsView.deleteByIds(ids);
    }

    private SysOperationLogsPageResponse convertToPageResponse(SysOperationLogs logs) {
        SysOperationLogsPageResponse response = new SysOperationLogsPageResponse();
        response.setId(logs.getId());
        response.setTitle(logs.getOperName());
        response.setMethod(logs.getMethod());
        response.setOperUrl(logs.getOperUrl());
        response.setOperIp(logs.getOperIp());
        response.setOperLocation(logs.getOperLocation());
        response.setUserId(String.valueOf(logs.getUserId()));
        response.setStatus(String.valueOf(logs.getStatus()));
        response.setCreateTime(logs.getCreateTime() != null ? Date.from(logs.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()) : null);
        return response;
    }

    private SysOperationLogsInfoResponse convertToInfoResponse(SysOperationLogs logs) {
        SysOperationLogsInfoResponse response = new SysOperationLogsInfoResponse();
        response.setId(logs.getId());
        response.setMethod(logs.getMethod());
        response.setOperUrl(logs.getOperUrl());
        response.setOperIp(logs.getOperIp());
        response.setOperLocation(logs.getOperLocation());
        response.setOperName(logs.getOperName());
        response.setOperParam(logs.getOperParam());
        response.setUserId(logs.getUserId());
        response.setCreateTime(logs.getCreateTime());
        return response;
    }

    private SysOperationLogs convertFromCreateRequest(CreateSysOperationLogsRequest request) {
        SysOperationLogs logs = new SysOperationLogs();
        logs.setOperName(request.getTitle());
        logs.setMethod(request.getMethod());
        logs.setOperUrl(request.getOperUrl());
        logs.setOperIp(request.getOperIp());
        logs.setOperLocation(request.getOperLocation());
        logs.setUserId(request.getUserId() != null ? request.getUserId().longValue() : null);
        logs.setOperParam(request.getOperParam());
        logs.setCreateTime(java.time.LocalDateTime.now());
        return logs;
    }

    private SysOperationLogs convertFromUpdateRequest(UpdateSysOperationLogsRequest request) {
        SysOperationLogs logs = new SysOperationLogs();
        logs.setId(request.getId());
        logs.setOperName(request.getTitle());
        logs.setMethod(request.getMethod());
        logs.setOperUrl(request.getOperUrl());
        logs.setOperIp(request.getOperIp());
        logs.setOperLocation(request.getOperLocation());
        logs.setUserId(request.getUserId() != null ? request.getUserId().longValue() : null);
        logs.setOperParam(request.getOperParam());
        return logs;
    }
}
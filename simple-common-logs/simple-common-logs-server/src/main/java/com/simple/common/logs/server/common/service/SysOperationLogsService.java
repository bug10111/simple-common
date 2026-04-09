package com.simple.common.logs.server.common.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.logs.server.common.dto.CreateSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.SysOperationLogsInfoResponse;
import com.simple.common.logs.server.common.dto.SysOperationLogsPageResponse;
import com.simple.common.logs.server.common.dto.UpdateSysOperationLogsRequest;
import com.simple.common.logs.server.common.entity.SysOperationLogs;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)接口
 *
 * @author qty
 */
public interface SysOperationLogsService {

    /**
     * 批量保存日志
     *
     * @param logsList 日志列表
     */
    void batchSave(List<SysOperationLogs> logsList);

    /**
     * 分页查询
     *
     * @param findAllRequest 查询参数
     * @return IPage<SysOperationLogsPageResponse> 分页数据
     */
    IPage<SysOperationLogsPageResponse> findAll(FindAllSysOperationLogsRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysOperationLogsInfoResponse  操作日志 详细数据
     */
    SysOperationLogsInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 操作日志 请求对象
     */
    String save(CreateSysOperationLogsRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param updateRequest 操作日志 请求对象
     */
    String updateById(UpdateSysOperationLogsRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);
}
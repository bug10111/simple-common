package com.simple.common.logs.server.common.service;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.logs.server.common.dto.SysOperationLogsPageResponse;
import com.simple.common.logs.server.common.dto.SysOperationLogsInfoResponse;
import com.simple.common.logs.server.common.dto.CreateSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.UpdateSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;

/**
 * 操作日志(sys_operation_logs)接口
 *
 * @author qty
 */
public interface SysOperationLogsService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysOperationLogsPageResponse> findAll(FindAllSysOperationLogsRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysOperationLogsFullInfoResponse  操作日志 详细数据
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


package com.simple.common.logs.server.common.view;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindSysOperationLogsRequest;
import com.simple.common.logs.server.common.entity.SysOperationLogs;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)视图接口
 *
 * @author qty
 */
public interface SysOperationLogsView {

    /**
     * 批量保存日志
     *
     * @param logsList 日志列表
     */
    void batchSave(List<SysOperationLogs> logsList);

    /**
     * 分页查询
     *
     * @param findAllRequest 查询条件
     * @return 分页结果
     */
    IPage<SysOperationLogs> findAll(FindAllSysOperationLogsRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysOperationLogs 原始表数据
     */
    SysOperationLogs findById(String id);

    /**
     * 根据条件查询单条数据
     *
     * @param findRequest 查询条件
     * @param neRequest 排除条件
     * @return SysOperationLogs 原始表数据
     */
    SysOperationLogs findOne(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 根据条件查询列表
     *
     * @param findRequest 查询条件
     * @param neRequest 排除条件
     * @return 列表数据
     */
    List<SysOperationLogs> findList(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 保存单条数据
     *
     * @param logs 日志数据
     */
    void save(SysOperationLogs logs);

    /**
     * 批量保存数据
     *
     * @param logsList 日志列表
     */
    void saves(List<SysOperationLogs> logsList);

    /**
     * 根据主键修改
     *
     * @param logs 日志数据
     */
    void updateById(SysOperationLogs logs);

    /**
     * 根据条件修改
     *
     * @param logs 日志数据
     * @param findRequest 查询条件
     * @param neRequest 排除条件
     */
    void update(SysOperationLogs logs, FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 根据条件删除
     *
     * @param findRequest 查询条件
     * @param neRequest 排除条件
     */
    void delete(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 根据主键批量删除
     *
     * @param ids 主键列表
     */
    void deleteByIds(List<String> ids);
}
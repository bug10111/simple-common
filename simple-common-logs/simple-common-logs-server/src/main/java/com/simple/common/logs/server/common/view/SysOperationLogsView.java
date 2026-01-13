package com.simple.common.logs.server.common.view;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import com.simple.common.logs.server.common.dto.FindAllSysOperationLogsRequest;
import com.simple.common.logs.server.common.dto.FindSysOperationLogsRequest;

/**
 * 操作日志(sys_operation_logs)数据库视图接口
 *
 * @author qty
 */
public interface SysOperationLogsView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
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
     * 获取单条数据
     *
     * @param findRequest 查询条件
     * @param neRequest   排除条件
     * @return SysOperationLogs 原始表数据
     */
    SysOperationLogs findOne(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 获取列表
     *
     * @param findRequest 查询条件
     * @param neRequest   排除条件
     * @return SysOperationLogs 原始表数据
     */
    List<SysOperationLogs> findList(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 新增
     *
     * @param sysOperationLogs 操作日志对象
     */
    void save(SysOperationLogs sysOperationLogs);

    /**
     * 根据id修改
     *
     * @param sysOperationLogs 操作日志对象
     */
    void updateById(SysOperationLogs sysOperationLogs);

    /**
     * 根据条件修改
     *
     * @param sysOperationLogs 操作日志对象
     * @param findRequest      查询条件
     * @param neRequest        排除条件
     */
    void update(SysOperationLogs sysOperationLogs, FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysOperationLogs> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 删除
     *
     * @param findRequest 查询条件
     * @param neRequest   排除条件
     */
    void delete(FindSysOperationLogsRequest findRequest, FindSysOperationLogsRequest neRequest);

    /**
     * 获取单条数据
     *
     * @return SysOperationLogs 原始表数据
     */
    default SysOperationLogs findOne(FindSysOperationLogsRequest findRequest) {
        return findOne(findRequest, null);
    }

    /**
     * 获取列表
     *
     * @param findRequest 查询条件
     * @return SysOperationLogs 原始表数据
     */
    default List<SysOperationLogs> findList(FindSysOperationLogsRequest findRequest) {
        return findList(findRequest, null);
    }

    /**
     * 根据条件修改
     *
     * @param findRequest 查询条件
     */
    default void update(SysOperationLogs sysOperationLogs, FindSysOperationLogsRequest findRequest) {
        update(sysOperationLogs, findRequest, null);
    }

    /**
     * 删除
     *
     * @param findRequest 查询条件
     */
    default void delete(FindSysOperationLogsRequest findRequest) {
        delete(findRequest, null);
    }
}


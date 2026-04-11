package com.simple.common.logs.server.view;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysOperationLogsRepository extends BaseMapper<SysOperationLogs> {


}


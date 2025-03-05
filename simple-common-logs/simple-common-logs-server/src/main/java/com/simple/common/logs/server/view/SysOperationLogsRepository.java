package com.simple.common.logs.server.view;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.common.logs.server.common.entity.SysOperationLogs;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 操作日志(sys_operation_logs)数据库访问层
 *
 * @author 兄台丶请冷静
 */
@Mapper
public interface SysOperationLogsRepository extends BaseMapper<SysOperationLogs> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysOperationLogs> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysOperationLogs> entities);

}


package com.simple.oauth.view.sysDictType;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysDictType.SysDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典类型(sys_dict_type)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysDictTypeRepository extends BaseMapper<SysDictType> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysDictType> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysDictType> entities);

}


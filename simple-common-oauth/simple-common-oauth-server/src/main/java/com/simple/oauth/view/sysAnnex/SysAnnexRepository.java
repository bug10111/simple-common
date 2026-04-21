package com.simple.oauth.view.sysAnnex;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysAnnex.SysAnnex;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 附件(sys_annex)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysAnnexRepository extends BaseMapper<SysAnnex> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysAnnex> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysAnnex> entities);

}


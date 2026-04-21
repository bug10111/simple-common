package com.simple.oauth.view.sysAdvertisement;

import java.util.Date;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysAdvertisement.SysAdvertisement;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 广告表(sys_advertisement)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysAdvertisementRepository extends BaseMapper<SysAdvertisement> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysAdvertisement> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysAdvertisement> entities);

}


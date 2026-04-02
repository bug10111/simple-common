package com.simple.oauth.view.sysAnnouncement;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysAnnouncement.SysAnnouncement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统公告(sys_announcement)数据库访问层
 *
 * @author 兄台丶请冷静
 */
@Mapper
public interface SysAnnouncementRepository extends BaseMapper<SysAnnouncement> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysAnnouncement> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysAnnouncement> entities);

}


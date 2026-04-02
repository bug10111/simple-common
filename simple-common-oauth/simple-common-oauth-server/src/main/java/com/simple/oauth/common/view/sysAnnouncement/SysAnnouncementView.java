package com.simple.oauth.common.view.sysAnnouncement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.dto.sysAnnouncement.FindAllSysAnnouncementRequest;
import com.simple.oauth.common.dto.sysAnnouncement.FindOneSysAnnouncementRequest;
import com.simple.oauth.common.entity.sysAnnouncement.SysAnnouncement;

import java.util.List;

/**
 * 系统公告(sys_announcement)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysAnnouncementView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysAnnouncement> findAll(FindAllSysAnnouncementRequest findAllRequest);

    /**
     * 分页列表
     *
     * @return 分页数据
     */
    IPage<SysAnnouncement> findAll(PageBase pageBase);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysAnnouncement 原始表数据
     */
    SysAnnouncement findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysAnnouncement 原始表数据
     */
    SysAnnouncement findOne(FindOneSysAnnouncementRequest findOneRequest, FindOneSysAnnouncementRequest neRequest);

    /**
     * 新增,或者根据id修改
     *
     * @param sysAnnouncement 系统公告对象
     */
    void saveOrUpdate(SysAnnouncement sysAnnouncement);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysAnnouncement> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @return SysAnnouncement 原始表数据
     */
    default SysAnnouncement findOne(FindOneSysAnnouncementRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysAnnouncementRequest());
    }

}


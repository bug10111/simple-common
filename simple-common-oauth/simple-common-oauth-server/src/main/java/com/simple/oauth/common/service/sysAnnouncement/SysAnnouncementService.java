package com.simple.oauth.common.service.sysAnnouncement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.dto.sysAnnouncement.*;

import java.util.List;

/**
 * 系统公告(sys_announcement)接口
 *
 * @author qty
 */
public interface SysAnnouncementService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysAnnouncementPageResponse> findAll(FindAllSysAnnouncementRequest findAllRequest);

    /**
     * 分页列表
     *
     * @return 分页数据
     */
    IPage<SysAnnouncementPageResponse> findAll(PageBase pageBase);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysAnnouncementFullInfoResponse  系统公告 详细数据
     */
    SysAnnouncementInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 系统公告 请求对象
     */
    String save(CreateSysAnnouncementRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param updateRequest 系统公告 请求对象
     */
    String updateById(UpdateSysAnnouncementRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);
}


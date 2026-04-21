package com.simple.oauth.service.sysAnnouncement;

import com.simple.common.core.utils.BeanUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.dto.sysAnnouncement.*;
import com.simple.oauth.common.entity.sysAnnouncement.SysAnnouncement;
import com.simple.oauth.common.service.sysAnnouncement.SysAnnouncementService;
import com.simple.oauth.common.view.sysAnnouncement.SysAnnouncementView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统公告(sys_announcement)默认接口实现
 *
 * @author qty
 */
@Service
@Transactional
class DefaultSysAnnouncementService implements SysAnnouncementService {

    @Autowired
    private SysAnnouncementView sysAnnouncementView;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public IPage<SysAnnouncementPageResponse> findAll(FindAllSysAnnouncementRequest findAllRequest) {
        var pageInfo = sysAnnouncementView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysAnnouncementPageResponse.class));
    }

    @Override
    public IPage<SysAnnouncementPageResponse> findAll(PageBase pageBase) {
        var pageInfo = sysAnnouncementView.findAll(pageBase);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysAnnouncementPageResponse.class));
    }

    @Override
    public SysAnnouncementInfoResponse findById(String id) {
        var sysAnnouncement = sysAnnouncementView.findById(id);
        AssertUtils.notEmptyParams(sysAnnouncement, "主键为[{}]的数据为空", id);
        return BeanUtils.copyProperties(sysAnnouncement, SysAnnouncementInfoResponse.class);
    }

    @Override
    public String save(CreateSysAnnouncementRequest createRequest) {
        var entity = BeanUtils.copyProperties(createRequest, SysAnnouncement.class);
        sysAnnouncementView.saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    public String updateById(UpdateSysAnnouncementRequest updateRequest) {
        var entity = BeanUtils.copyProperties(updateRequest, SysAnnouncement.class);
        sysAnnouncementView.saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysAnnouncementView.deleteByIds(ids);
    }
}


package com.simple.oauth.view.sysAnnouncement;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.mp.common.enums.Status;
import com.simple.common.mp.page.PageBase;
import com.simple.oauth.common.dto.sysAnnouncement.FindAllSysAnnouncementRequest;
import com.simple.oauth.common.dto.sysAnnouncement.FindOneSysAnnouncementRequest;
import com.simple.oauth.common.entity.sysAnnouncement.SysAnnouncement;
import com.simple.oauth.common.view.sysAnnouncement.SysAnnouncementView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统公告(sys_announcement)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysAnnouncementView implements SysAnnouncementView {

    @Autowired
    private SysAnnouncementRepository repository;

    @Override
    public IPage<SysAnnouncement> findAll(FindAllSysAnnouncementRequest findAllRequest) {
        LambdaQueryWrapper<SysAnnouncement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getTitle()), SysAnnouncement::getTitle, findAllRequest.getTitle())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysAnnouncement::getStatus, findAllRequest.getStatus());
        return repository.selectPage(findAllRequest.getPage(SysAnnouncement.class), queryWrapper);
    }

    @Override
    public IPage<SysAnnouncement> findAll(PageBase pageBase) {
        DateTime now = DateTime.now();
        return repository.selectPage(pageBase.getPage(SysAnnouncement.class),
                                     new LambdaQueryWrapper<SysAnnouncement>().eq(SysAnnouncement::getStatus, Status.ON)
                                                                              .le(SysAnnouncement::getBeginTime, now)
                                                                              .ge(SysAnnouncement::getEndTime, now));
    }

    @Override
    public SysAnnouncement findOne(FindOneSysAnnouncementRequest findOneRequest, FindOneSysAnnouncementRequest neRequest) {
        LambdaQueryWrapper<SysAnnouncement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysAnnouncement::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getTitle()), SysAnnouncement::getTitle, findOneRequest.getTitle())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getData()), SysAnnouncement::getData, findOneRequest.getData())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getBeginTime()), SysAnnouncement::getBeginTime, findOneRequest.getBeginTime())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getEndTime()), SysAnnouncement::getEndTime, findOneRequest.getEndTime())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getStatus()), SysAnnouncement::getStatus, findOneRequest.getStatus())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysAnnouncement::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getTitle()), SysAnnouncement::getTitle, neRequest.getTitle())
                    .ne(ObjUtil.isNotEmpty(neRequest.getData()), SysAnnouncement::getData, neRequest.getData())
                    .ne(ObjUtil.isNotEmpty(neRequest.getBeginTime()), SysAnnouncement::getBeginTime, neRequest.getBeginTime())
                    .ne(ObjUtil.isNotEmpty(neRequest.getEndTime()), SysAnnouncement::getEndTime, neRequest.getEndTime())
                    .ne(ObjUtil.isNotEmpty(neRequest.getStatus()), SysAnnouncement::getStatus, neRequest.getStatus());

        List<SysAnnouncement> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysAnnouncement findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void saveOrUpdate(SysAnnouncement sysAnnouncement) {
        if (sysAnnouncement.getId() == null) {
            repository.insert(sysAnnouncement);
        } else {
            repository.updateById(sysAnnouncement);
        }
    }

    @Override
    public void saves(List<SysAnnouncement> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}


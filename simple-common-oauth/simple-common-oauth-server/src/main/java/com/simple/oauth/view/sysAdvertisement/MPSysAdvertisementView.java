package com.simple.oauth.view.sysAdvertisement;

import java.util.Date;

import com.simple.common.core.utils.AssertUtils;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.lang.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Component;
import com.simple.oauth.common.view.sysAdvertisement.SysAdvertisementView;
import com.simple.oauth.common.entity.sysAdvertisement.SysAdvertisement;
import com.simple.oauth.common.dto.sysAdvertisement.FindAllSysAdvertisementRequest;
import com.simple.oauth.common.dto.sysAdvertisement.FindOneSysAdvertisementRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simple.common.core.utils.JsonUtils;

/**
 * 广告表(sys_advertisement)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysAdvertisementView implements SysAdvertisementView {

    @Autowired
    private SysAdvertisementRepository repository;

    @Override
    public IPage<SysAdvertisement> findAll(FindAllSysAdvertisementRequest findAllRequest) {
        LambdaQueryWrapper<SysAdvertisement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getName()), SysAdvertisement::getName, findAllRequest.getName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getClientId()), SysAdvertisement::getClientId, findAllRequest.getClientId())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getType()), SysAdvertisement::getType, findAllRequest.getType())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getIsLink()), SysAdvertisement::getIsLink, findAllRequest.getIsLink())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getStatus()), SysAdvertisement::getStatus, findAllRequest.getStatus());
        return repository.selectPage(findAllRequest.getPage(SysAdvertisement.class), queryWrapper);
    }

    @Override
    public SysAdvertisement findOne(FindOneSysAdvertisementRequest findOneRequest, FindOneSysAdvertisementRequest neRequest) {
        LambdaQueryWrapper<SysAdvertisement> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getName()), SysAdvertisement::getName, findOneRequest.getName())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getClientId()), SysAdvertisement::getClientId, findOneRequest.getClientId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getType()), SysAdvertisement::getType, findOneRequest.getType())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getImage()), SysAdvertisement::getImage, findOneRequest.getImage())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getIsLink()), SysAdvertisement::getIsLink, findOneRequest.getIsLink())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getLink()), SysAdvertisement::getLink, findOneRequest.getLink())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getSort()), SysAdvertisement::getSort, findOneRequest.getSort())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getBeginTime()), SysAdvertisement::getBeginTime, findOneRequest.getBeginTime())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getEndTime()), SysAdvertisement::getEndTime, findOneRequest.getEndTime())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getStatus()), SysAdvertisement::getStatus, findOneRequest.getStatus())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysAdvertisement::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getName()), SysAdvertisement::getName, neRequest.getName())
                    .ne(ObjUtil.isNotEmpty(neRequest.getClientId()), SysAdvertisement::getClientId, neRequest.getClientId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getType()), SysAdvertisement::getType, neRequest.getType())
                    .ne(ObjUtil.isNotEmpty(neRequest.getImage()), SysAdvertisement::getImage, neRequest.getImage())
                    .ne(ObjUtil.isNotEmpty(neRequest.getIsLink()), SysAdvertisement::getIsLink, neRequest.getIsLink())
                    .ne(ObjUtil.isNotEmpty(neRequest.getLink()), SysAdvertisement::getLink, neRequest.getLink())
                    .ne(ObjUtil.isNotEmpty(neRequest.getSort()), SysAdvertisement::getSort, neRequest.getSort())
                    .ne(ObjUtil.isNotEmpty(neRequest.getBeginTime()), SysAdvertisement::getBeginTime, neRequest.getBeginTime())
                    .ne(ObjUtil.isNotEmpty(neRequest.getEndTime()), SysAdvertisement::getEndTime, neRequest.getEndTime())
                    .ne(ObjUtil.isNotEmpty(neRequest.getStatus()), SysAdvertisement::getStatus, neRequest.getStatus());

        List<SysAdvertisement> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysAdvertisement findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void save(SysAdvertisement sysAdvertisement) {
        repository.insert(sysAdvertisement);
    }

    @Override
    public void updateById(SysAdvertisement sysAdvertisement) {
        repository.updateById(sysAdvertisement);
    }

    @Override
    public void saves(List<SysAdvertisement> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}


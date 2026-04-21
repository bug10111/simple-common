package com.simple.oauth.service.sysAdvertisement;

import com.simple.common.core.utils.BeanUtils;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.common.mp.common.enums.Status;
import com.simple.oauth.common.dto.sysAdvertisement.*;
import com.simple.oauth.common.entity.sysAdvertisement.SysAdvertisement;
import com.simple.oauth.common.entity.sysClientDetails.SysClientDetails;
import com.simple.oauth.common.service.sysAdvertisement.SysAdvertisementService;
import com.simple.oauth.common.view.sysAdvertisement.SysAdvertisementView;
import com.simple.oauth.common.view.sysClientDetails.SysClientDetailsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 广告表(sys_advertisement)默认接口实现
 *
 * @author qty
 */
@Service
@Transactional
class DefaultSysAdvertisementService implements SysAdvertisementService {

    @Autowired
    private SysAdvertisementView sysAdvertisementView;

    @Autowired
    private SysClientDetailsView sysClientDetailsView;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public IPage<SysAdvertisementPageResponse> findAll(FindAllSysAdvertisementRequest findAllRequest) {

        //非admin客户端的时候，只查询当前客户端的数据
        HashSet<String> loginRole = LoginUserUtils.getUserTemporary().getLoginRole();
        if (ObjUtil.isNotEmpty(loginRole) && loginRole.contains("oauth")) {
            findAllRequest.setClientId(null);
        }

        var pageInfo = sysAdvertisementView.findAll(findAllRequest);
        IPage<SysAdvertisementPageResponse> convert = pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysAdvertisementPageResponse.class));

        for (SysAdvertisementPageResponse response : convert.getRecords()) {
            if (response != null) {
                SysClientDetails clientDetails = sysClientDetailsView.findByClientId(response.getClientId());
                if(clientDetails != null) {
                    response.setClientName(clientDetails.getClientName());
                }
            }
        }
        return convert;
    }

    @Override
    public SysAdvertisementInfoResponse findById(String id) {
        var sysAdvertisement = sysAdvertisementView.findById(id);
        AssertUtils.notEmptyParams(sysAdvertisement, "主键为[{}]的数据为空", id);
        return BeanUtils.copyProperties(sysAdvertisement, SysAdvertisementInfoResponse.class);
    }

    @Override
    public String save(CreateSysAdvertisementRequest createRequest) {

        SysAdvertisement one = sysAdvertisementView.findOne(new FindOneSysAdvertisementRequest().setName(createRequest.getName()));
        AssertUtils.isTrue(one == null, "广告名称重复");

        FindOneSysAdvertisementRequest request = new FindOneSysAdvertisementRequest();
        request.setType(createRequest.getType());
        request.setClientId(createRequest.getClientId());
        request.setSort(createRequest.getSort());
        SysAdvertisement sort = sysAdvertisementView.findOne(request);
        AssertUtils.isTrue(sort == null, "当前客户端同类型下的广告，排序重复");

        var entity = BeanUtils.copyProperties(createRequest, SysAdvertisement.class);
        entity.setStatus(Status.ON);
        sysAdvertisementView.save(entity);
        return entity.getId();
    }

    @Override
    public String updateById(UpdateSysAdvertisementRequest updateRequest) {

        SysAdvertisement one = sysAdvertisementView.findOne(new FindOneSysAdvertisementRequest().setName(updateRequest.getName()),
                                                            new FindOneSysAdvertisementRequest().setId(updateRequest.getId()));
        AssertUtils.isTrue(one == null, "广告名称重复");

        FindOneSysAdvertisementRequest request = new FindOneSysAdvertisementRequest();
        request.setType(updateRequest.getType());
        request.setClientId(updateRequest.getClientId());
        request.setSort(updateRequest.getSort());
        SysAdvertisement sort = sysAdvertisementView.findOne(request, new FindOneSysAdvertisementRequest().setId(updateRequest.getId()));
        AssertUtils.isTrue(sort == null, "当前客户端同类型下的广告，排序重复");

        var entity = BeanUtils.copyProperties(updateRequest, SysAdvertisement.class);
        sysAdvertisementView.updateById(entity);
        return entity.getId();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysAdvertisementView.deleteByIds(ids);
    }

    @Override
    public void disable(String id) {
        SysAdvertisement byId = sysAdvertisementView.findById(id);
        AssertUtils.notEmpty(byId,"广告不存在");

        byId.setStatus(Status.OFF);
        sysAdvertisementView.updateById(byId);
    }

    @Override
    public void enable(String id) {
        SysAdvertisement byId = sysAdvertisementView.findById(id);
        AssertUtils.notEmpty(byId,"广告不存在");

        byId.setStatus(Status.ON);
        sysAdvertisementView.updateById(byId);
    }
}


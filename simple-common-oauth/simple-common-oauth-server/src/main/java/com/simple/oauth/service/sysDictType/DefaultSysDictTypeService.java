package com.simple.oauth.service.sysDictType;

import com.simple.common.core.utils.BeanUtils;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import com.simple.oauth.common.dto.sysDictType.*;
import com.simple.oauth.common.entity.sysDictType.SysDictType;
import com.simple.oauth.common.service.sysDictType.SysDictTypeService;
import com.simple.oauth.common.view.sysDictType.SysDictTypeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典类型(sys_dict_type)默认接口实现
 *
 * @author 兄台丶请冷静
 */
@Service
@Transactional
class DefaultSysDictTypeService implements SysDictTypeService {

    @Autowired
    private SysDictTypeView sysDictTypeView;

    @Autowired
    private EventBusService eventBusService;

    @Override
    public IPage<SysDictTypePageResponse> findAll(FindAllSysDictTypeRequest findAllRequest) {
        var pageInfo = sysDictTypeView.findAll(findAllRequest);
        return pageInfo.convert(entity -> BeanUtils.copyProperties(entity, SysDictTypePageResponse.class));
    }

    @Override
    public SysDictTypeInfoResponse findById(String id) {
        var sysDictType = sysDictTypeView.findById(id);
        AssertUtils.notEmptyParams(sysDictType, "主键为[{}]的数据为空", id);
        return BeanUtils.copyProperties(sysDictType, SysDictTypeInfoResponse.class);
    }

    @Override
    public String save(CreateSysDictTypeRequest createRequest) {
        var entity = BeanUtils.copyProperties(createRequest, SysDictType.class);
        SysDictType one = sysDictTypeView.findOne(new FindOneSysDictTypeRequest().setDictType(entity.getDictType()));
        AssertUtils.isTrueParams(ObjUtil.isEmpty(one), "字典类型[{}]已经存在", entity.getDictType());
        sysDictTypeView.saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    public String updateById(UpdateSysDictTypeRequest updateRequest) {
        var entity = BeanUtils.copyProperties(updateRequest, SysDictType.class);
        SysDictType byId = sysDictTypeView.findById(updateRequest.getId());
        AssertUtils.notEmpty(byId, "字典类型不存在");

        SysDictType one = sysDictTypeView.findOne(new FindOneSysDictTypeRequest().setDictType(byId.getDictType()),
                                                  new FindOneSysDictTypeRequest().setId(byId.getId()));
        AssertUtils.isTrueParams(ObjUtil.isEmpty(one), "字典类型[{}]已经存在", entity.getDictType());

        sysDictTypeView.saveOrUpdate(entity);
        return entity.getId();
    }

    @Override
    public void deleteByIds(List<String> ids) {
        sysDictTypeView.deleteByIds(ids);
    }
}


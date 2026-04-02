package com.simple.oauth.view.sysDictType;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.oauth.common.dto.sysDictType.FindAllSysDictTypeRequest;
import com.simple.oauth.common.dto.sysDictType.FindOneSysDictTypeRequest;
import com.simple.oauth.common.entity.sysDictType.SysDictType;
import com.simple.oauth.common.view.sysDictType.SysDictTypeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典类型(sys_dict_type)数据库视图实现
 *
 * @author 兄台丶请冷静
 */
@Component
class MPSysDictTypeView implements SysDictTypeView {

    @Autowired
    private SysDictTypeRepository repository;

    @Override
    public IPage<SysDictType> findAll(FindAllSysDictTypeRequest findAllRequest) {
        LambdaQueryWrapper<SysDictType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getDictName()), SysDictType::getDictName, findAllRequest.getDictName())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getDictType()), SysDictType::getDictType, findAllRequest.getDictType());
        return repository.selectPage(findAllRequest.getPage(SysDictType.class), queryWrapper);
    }

    @Override
    public SysDictType findOne(FindOneSysDictTypeRequest findOneRequest, FindOneSysDictTypeRequest neRequest) {
        LambdaQueryWrapper<SysDictType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysDictType::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getDictName()), SysDictType::getDictName, findOneRequest.getDictName())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getDictType()), SysDictType::getDictType, findOneRequest.getDictType())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getRemark()), SysDictType::getRemark, findOneRequest.getRemark())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysDictType::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getDictName()), SysDictType::getDictName, neRequest.getDictName())
                    .ne(ObjUtil.isNotEmpty(neRequest.getDictType()), SysDictType::getDictType, neRequest.getDictType())
                    .ne(ObjUtil.isNotEmpty(neRequest.getRemark()), SysDictType::getRemark, neRequest.getRemark());

        List<SysDictType> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysDictType findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void saveOrUpdate(SysDictType sysDictType) {
        if (sysDictType.getId() == null) {
            repository.insert(sysDictType);
        } else {
            repository.updateById(sysDictType);
        }
    }

    @Override
    public void saves(List<SysDictType> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}


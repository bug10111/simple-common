package com.simple.oauth.view.sysDictData;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.oauth.common.dto.sysDictData.FindAllSysDictDataRequest;
import com.simple.oauth.common.dto.sysDictData.FindOneSysDictDataRequest;
import com.simple.oauth.common.entity.sysDictData.SysDictData;
import com.simple.oauth.common.view.sysDictData.SysDictDataView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典数据(sys_dict_data)数据库视图实现
 *
 * @author qty
 */
@Component
class MPSysDictDataView implements SysDictDataView {

    @Autowired
    private SysDictDataRepository repository;

    @Override
    public IPage<SysDictData> findAll(FindAllSysDictDataRequest findAllRequest) {
        LambdaQueryWrapper<SysDictData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ObjUtil.isNotEmpty(findAllRequest.getDictLabel()), SysDictData::getDictLabel, findAllRequest.getDictLabel())
                    .likeRight(ObjUtil.isNotEmpty(findAllRequest.getDictValue()), SysDictData::getDictValue, findAllRequest.getDictValue())
                    .eq(ObjUtil.isNotEmpty(findAllRequest.getDictType()), SysDictData::getDictType, findAllRequest.getDictType());
        return repository.selectPage(findAllRequest.getPage(SysDictData.class), queryWrapper);
    }

    @Override
    public List<SysDictData> labelList(String DictType) {
        return repository.selectList(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, DictType).select(SysDictData::getDictValue, SysDictData::getDictLabel));
    }

    @Override
    public SysDictData findOne(FindOneSysDictDataRequest findOneRequest, FindOneSysDictDataRequest neRequest) {
        LambdaQueryWrapper<SysDictData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotEmpty(findOneRequest.getId()), SysDictData::getId, findOneRequest.getId())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getSerial()), SysDictData::getSerial, findOneRequest.getSerial())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getDictLabel()), SysDictData::getDictLabel, findOneRequest.getDictLabel())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getDictValue()), SysDictData::getDictValue, findOneRequest.getDictValue())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getDictType()), SysDictData::getDictType, findOneRequest.getDictType())
                    .eq(ObjUtil.isNotEmpty(findOneRequest.getRemark()), SysDictData::getRemark, findOneRequest.getRemark())
                    .ne(ObjUtil.isNotEmpty(neRequest.getId()), SysDictData::getId, neRequest.getId())
                    .ne(ObjUtil.isNotEmpty(neRequest.getSerial()), SysDictData::getSerial, neRequest.getSerial())
                    .ne(ObjUtil.isNotEmpty(neRequest.getDictLabel()), SysDictData::getDictLabel, neRequest.getDictLabel())
                    .ne(ObjUtil.isNotEmpty(neRequest.getDictValue()), SysDictData::getDictValue, neRequest.getDictValue())
                    .ne(ObjUtil.isNotEmpty(neRequest.getDictType()), SysDictData::getDictType, neRequest.getDictType())
                    .ne(ObjUtil.isNotEmpty(neRequest.getRemark()), SysDictData::getRemark, neRequest.getRemark());

        List<SysDictData> list = repository.selectList(queryWrapper);
        if (list.isEmpty()) {
            return null;
        } else if (list.size() > 1) {
            AssertUtils.error("数据异常", "参数为[{}]查询只需要一条数据，但是查询出来多条", JsonUtils.toJsonStr(findOneRequest));
        }
        return list.get(0);
    }

    @Override
    public SysDictData findById(String id) {
        return repository.selectById(id);
    }

    @Override
    public void saveOrUpdate(SysDictData sysDictData) {
        if (sysDictData.getId() == null) {
            repository.insert(sysDictData);
        } else {
            repository.updateById(sysDictData);
        }
    }

    @Override
    public void saves(List<SysDictData> list) {
        repository.insertBatch(list);
    }

    @Override
    public void deleteByIds(List<String> ids) {
        repository.deleteByIds(ids);
    }
}


package com.simple.oauth.common.view.sysDictData;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysDictData.FindAllSysDictDataRequest;
import com.simple.oauth.common.dto.sysDictData.FindOneSysDictDataRequest;
import com.simple.oauth.common.entity.sysDictData.SysDictData;

import java.util.List;

/**
 * 字典数据(sys_dict_data)数据库视图接口
 *
 * @author qty
 */
public interface SysDictDataView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysDictData> findAll(FindAllSysDictDataRequest findAllRequest);

    /**
     * 获取数据
     *
     * @param DictType key
     */
    List<SysDictData> labelList(String DictType);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysDictData 原始表数据
     */
    SysDictData findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysDictData 原始表数据
     */
    SysDictData findOne(FindOneSysDictDataRequest findOneRequest, FindOneSysDictDataRequest neRequest);

    /**
     * 新增,或者根据id修改
     *
     * @param sysDictData 字典数据对象
     */
    void saveOrUpdate(SysDictData sysDictData);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysDictData> list);

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
     * @return SysDictData 原始表数据
     */
    default SysDictData findOne(FindOneSysDictDataRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysDictDataRequest());
    }

}


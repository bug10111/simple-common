package com.simple.oauth.common.view.sysDictType;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysDictType.FindAllSysDictTypeRequest;
import com.simple.oauth.common.dto.sysDictType.FindOneSysDictTypeRequest;
import com.simple.oauth.common.entity.sysDictType.SysDictType;

import java.util.List;

/**
 * 字典类型(sys_dict_type)数据库视图接口
 *
 * @author qty
 */
public interface SysDictTypeView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysDictType> findAll(FindAllSysDictTypeRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysDictType 原始表数据
     */
    SysDictType findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysDictType 原始表数据
     */
    SysDictType findOne(FindOneSysDictTypeRequest findOneRequest, FindOneSysDictTypeRequest neRequest);

    /**
     * 新增,或者根据id修改
     *
     * @param sysDictType 字典类型对象
     */
    void saveOrUpdate(SysDictType sysDictType);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysDictType> list);

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
     * @return SysDictType 原始表数据
     */
    default SysDictType findOne(FindOneSysDictTypeRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysDictTypeRequest());
    }

}


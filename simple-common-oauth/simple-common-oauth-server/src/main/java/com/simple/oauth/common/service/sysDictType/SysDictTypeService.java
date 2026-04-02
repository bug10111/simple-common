package com.simple.oauth.common.service.sysDictType;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysDictType.*;

import java.util.List;

/**
 * 字典类型(sys_dict_type)接口
 *
 * @author 兄台丶请冷静
 */
public interface SysDictTypeService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysDictTypePageResponse> findAll(FindAllSysDictTypeRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysDictTypeFullInfoResponse  字典类型 详细数据
     */
    SysDictTypeInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 字典类型 请求对象
     */
    String save(CreateSysDictTypeRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param updateRequest 字典类型 请求对象
     */
    String updateById(UpdateSysDictTypeRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);
}


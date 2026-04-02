package com.simple.oauth.common.service.sysDictData;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysDictData.*;

import java.util.List;
import java.util.Map;

/**
 * 字典数据(sys_dict_data)接口
 *
 * @author 兄台丶请冷静
 */
public interface SysDictDataService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysDictDataPageResponse> findAll(FindAllSysDictDataRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysDictDataFullInfoResponse  字典数据 详细数据
     */
    SysDictDataInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 字典数据 请求对象
     */
    String save(CreateSysDictDataRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param updateRequest 字典数据 请求对象
     */
    String updateById(UpdateSysDictDataRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 获取字典
     *
     * @param dictValues keys
     */
    Map<String,List<SysDictDatasResponse>> labelList(List<String> dictValues);
}


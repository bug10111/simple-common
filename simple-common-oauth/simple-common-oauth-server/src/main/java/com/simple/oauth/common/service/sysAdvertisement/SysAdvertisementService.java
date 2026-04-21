package com.simple.oauth.common.service.sysAdvertisement;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysAdvertisement.SysAdvertisementPageResponse;
import com.simple.oauth.common.dto.sysAdvertisement.SysAdvertisementInfoResponse;
import com.simple.oauth.common.dto.sysAdvertisement.CreateSysAdvertisementRequest;
import com.simple.oauth.common.dto.sysAdvertisement.UpdateSysAdvertisementRequest;
import com.simple.oauth.common.dto.sysAdvertisement.FindAllSysAdvertisementRequest;

/**
 * 广告表(sys_advertisement)接口
 *
 * @author qty
 */
public interface SysAdvertisementService {

    /**
     * 分页列表
     *
     * @param findAllRequest 请求参数
     * @return 分页数据
     */
    IPage<SysAdvertisementPageResponse> findAll(FindAllSysAdvertisementRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysAdvertisementFullInfoResponse  广告表 详细数据
     */
    SysAdvertisementInfoResponse findById(String id);

    /**
     * 新增
     *
     * @param createRequest 广告表 请求对象
     */
    String save(CreateSysAdvertisementRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param updateRequest 广告表 请求对象
     */
    String updateById(UpdateSysAdvertisementRequest updateRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 禁用
     * @param id 主键
     */
    void disable(String id);

    /**
     * 启用
     * @param id 主键
     */
    void enable(String id);
}


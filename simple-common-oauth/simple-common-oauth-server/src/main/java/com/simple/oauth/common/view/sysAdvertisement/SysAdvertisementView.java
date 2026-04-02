package com.simple.oauth.common.view.sysAdvertisement;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.simple.oauth.common.entity.sysAdvertisement.SysAdvertisement;
import com.simple.oauth.common.dto.sysAdvertisement.FindAllSysAdvertisementRequest;
import com.simple.oauth.common.dto.sysAdvertisement.FindOneSysAdvertisementRequest;

/**
 * 广告表(sys_advertisement)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysAdvertisementView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysAdvertisement> findAll(FindAllSysAdvertisementRequest findAllRequest);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysAdvertisement 原始表数据
     */
    SysAdvertisement findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysAdvertisement 原始表数据
     */
    SysAdvertisement findOne(FindOneSysAdvertisementRequest findOneRequest, FindOneSysAdvertisementRequest neRequest);

    /**
     * 新增
     *
     * @param sysAdvertisement 广告表对象
     */
    void save(SysAdvertisement sysAdvertisement);

    /**
     * 根据id修改
     *
     * @param sysAdvertisement 广告表对象
     */
    void updateById(SysAdvertisement sysAdvertisement);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysAdvertisement> list);

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
     * @return SysAdvertisement 原始表数据
     */
    default SysAdvertisement findOne(FindOneSysAdvertisementRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysAdvertisementRequest());
    }

}


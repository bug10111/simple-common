package com.simple.oauth.common.view.sysAnnex;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysAnnex.FindAllSysAnnexRequest;
import com.simple.oauth.common.dto.sysAnnex.FindOneSysAnnexRequest;
import com.simple.oauth.common.entity.sysAnnex.SysAnnex;

import java.util.List;

/**
 * 附件(sys_annex)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysAnnexView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysAnnex> findAll(FindAllSysAnnexRequest findAllRequest);

    /**
     * 分页列表
     *
     * @param ids 主键集合
     */
    List<SysAnnex> findAll(List<String> ids);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysAnnex 原始表数据
     */
    SysAnnex findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysAnnex 原始表数据
     */
    SysAnnex findOne(FindOneSysAnnexRequest findOneRequest, FindOneSysAnnexRequest neRequest);

    /**
     * 新增,或者根据id修改
     *
     * @param sysAnnex 附件对象
     */
    void saveOrUpdate(SysAnnex sysAnnex);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysAnnex> list);

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
     * @return SysAnnex 原始表数据
     */
    default SysAnnex findOne(FindOneSysAnnexRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysAnnexRequest());
    }

}


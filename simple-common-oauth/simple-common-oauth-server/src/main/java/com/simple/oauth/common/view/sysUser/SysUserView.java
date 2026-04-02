package com.simple.oauth.common.view.sysUser;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.simple.oauth.common.dto.sysUser.FindAllSysUserRequest;
import com.simple.oauth.common.dto.sysUser.FindOneSysUserRequest;
import com.simple.oauth.common.entity.sysUser.SysUser;

import java.util.List;

/**
 * 用户(sys_user)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysUserView {

    /**
     * 分页列表
     *
     * @param findAllRequest 分页参数
     * @return 分页数据
     */
    IPage<SysUser> findAll(FindAllSysUserRequest findAllRequest);

    /**
     * 获取用户列表
     *
     * @param roleKey roleKey
     */
    List<SysUser> findOneByRoleKey(String roleKey);
    List<SysUser> findOneByRoleId(String roleId);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysUser 原始表数据
     */
    SysUser findById(String id);

    /**
     * 获取单条数据
     *
     * @param findOneRequest 查询条件
     * @param neRequest      排除条件
     * @return SysUser 原始表数据
     */
    SysUser findOne(FindOneSysUserRequest findOneRequest, FindOneSysUserRequest neRequest);

    /**
     * 新增,或者根据id修改
     *
     * @param sysUser 用户对象
     */
    void saveOrUpdate(SysUser sysUser);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysUser> list);

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
     * @return SysUser 原始表数据
     */
    default SysUser findOne(FindOneSysUserRequest findOneRequest) {
        return findOne(findOneRequest, new FindOneSysUserRequest());
    }

}


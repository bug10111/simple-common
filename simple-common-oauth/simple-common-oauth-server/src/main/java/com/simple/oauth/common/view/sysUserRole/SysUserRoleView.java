package com.simple.oauth.common.view.sysUserRole;

import com.simple.oauth.common.entity.sysUserRole.SysUserRole;

import java.util.List;

/**
 * 用户和角色关联(sys_user_role)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysUserRoleView {

    /**
     * 获取单条数据
     *
     * @param id         主键
     * @param allowEmpty 允许空值返回
     * @return SysUserRole 原始表数据
     */
    SysUserRole findById(String id, Boolean allowEmpty);

    /**
     * 根据用户Id获取用户角色关联
     *
     * @param userId 用户ID
     */
    List<SysUserRole> findByUserId(String userId);

    /**
     * 根据角色Id获取用户角色关联
     *
     * @param roleId 角色
     */
    List<SysUserRole> findByRoleId(String roleId);

    /**
     * 新增,或者根据id修改
     *
     * @param sysUserRole 用户和角色关联对象
     */
    void saveOrUpdate(SysUserRole sysUserRole);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysUserRole> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

    /**
     * 根据用户ID删除用户角色绑定
     *
     * @param userId 用户ID
     */
    void deleteByUserId(String userId);

    /**
     * 根据用户ID和角色Id删除用户角色绑定
     *
     * @param userId 用户ID
     * @param roleId 角色Id
     */
    void deleteByUserIdAndRoleId(String userId, String roleId);

}


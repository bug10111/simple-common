package com.simple.oauth.common.view.sysRoleMenu;

import com.simple.oauth.common.entity.sysRoleMenu.SysRoleMenu;

import java.util.List;

/**
 * 角色和菜单关联(sys_role_menu)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysRoleMenuView {

    /**
     * 获取单条数据
     *
     * @param id         主键
     * @param allowEmpty 允许空值返回
     * @return SysRoleMenu 原始表数据
     */
    SysRoleMenu findById(String id, Boolean allowEmpty);

    /**
     * 新增,或者根据id修改
     *
     * @param sysRoleMenu 角色和菜单关联对象
     */
    void saveOrUpdate(SysRoleMenu sysRoleMenu);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysRoleMenu> list);

    /**
     * 删除
     *
     * @param roleId 角色主键
     */
    void deleteByRoleId(String roleId);

    /**
     * 删除
     *
     * @param menuId 菜单ID
     */
    void deleteByMenuId(String menuId);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

}


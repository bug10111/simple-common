package com.simple.oauth.common.view.sysMenu;

import com.simple.oauth.common.entity.sysMenu.SysMenu;

import java.util.List;

/**
 * 菜单权限(sys_menu)数据库视图接口
 *
 * @author 兄台丶请冷静
 */
public interface SysMenuView {

    /**
     * 获取所有数据
     *
     * @return 数据
     */
    List<SysMenu> findAll(String clientId);

    /**
     * 获取所有数据
     * @param ids 主键集合
     */
    List<SysMenu> findAll(List<String> ids);

    /**
     * 获取所有数据
     *
     * @param roleId   角色ID
     * @param clientId 客户端ID
     * @return 数据
     */
    List<SysMenu> findAllByRoleId(String roleId,String clientId);

    /**
     * 获取所有数据
     *
     * @param roleKey   角色ID
     * @return 数据
     */
    List<SysMenu> findAllByRoleKey(String roleKey);

    /**
     * 获取所有数据
     *
     * @param userId   用户ID
     * @param clientId 客户端ID
     * @return 数据
     */
    List<SysMenu> findAllByUserId(String userId, String clientId);

    /**
     * 获取单条数据
     *
     * @param id         主键
     * @param allowEmpty 允许空值返回
     * @return SysMenu 原始表数据
     */
    SysMenu findById(String id, Boolean allowEmpty);

    /**
     * 获取单条数据
     *
     * @param code code
     * @return SysMenu 原始表数据
     */
    SysMenu findByCode(String code);

    /**
     * 获取单条数据
     *
     * @param perms 权限标志
     * @return SysMenu 原始表数据
     */
    SysMenu findByPerms(String perms);

    /**
     * 获取单条数据
     *
     * @param code code
     * @param id   id
     * @return SysMenu 原始表数据
     */
    SysMenu findByCodeAndNeId(String code, String id);

    /**
     * 获取单条数据
     *
     * @param perms 权限标志
     * @param id    id
     * @return SysMenu 原始表数据
     */
    SysMenu findByPermsAndNeId(String perms, String id);

    /**
     * 新增,或者根据id修改
     *
     * @param sysMenu 菜单权限对象
     */
    void saveOrUpdate(SysMenu sysMenu);

    /**
     * 批量新增
     *
     * @param list 对象
     */
    void saves(List<SysMenu> list);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);

}


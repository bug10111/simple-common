package com.simple.oauth.common.service.sysMenu;

import cn.hutool.core.lang.tree.Tree;
import com.simple.oauth.common.dto.sysMenu.CreateSysMenuRequest;
import com.simple.oauth.common.dto.sysMenu.SysMenuInfoResponse;
import com.simple.oauth.common.dto.sysMenu.SysMenuPageResponse;
import com.simple.oauth.common.dto.sysMenu.UpdateSysMenuRequest;

import java.util.List;
import java.util.Set;

/**
 * 菜单权限(sys_menu)接口
 *
 * @author qty
 */
public interface SysMenuService {

    /**
     * 所有权限数据
     *
     * @return 分页数据
     */
    List<Tree<String>> findAll(String clientId);

    /**
     * 所有权限数据
     *
     * @return 分页数据
     */
    Set<SysMenuPageResponse> findAllByLoginUser(Set<String> loginRole, String userId,String clientId);

    /**
     * 所有权限数据
     *
     * @return 分页数据
     */
    List<Tree<String>> findAllByRoleId(String roleId,String clientId);

    /**
     * 获取单条数据
     *
     * @param id 主键
     * @return SysMenuFullInfoResponse  菜单权限 详细数据
     */
    SysMenuInfoResponse findById(String id);

    /**
     * 新增，或者根据主键修改
     *
     * @param createRequest 菜单权限 请求对象
     */
    String save(CreateSysMenuRequest createRequest);

    /**
     * 根据主键修改
     *
     * @param createRequest 菜单权限 请求对象
     */
    void updateById(UpdateSysMenuRequest createRequest);

    /**
     * 删除
     *
     * @param ids 主键
     */
    void deleteByIds(List<String> ids);
}


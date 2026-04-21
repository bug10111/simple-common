package com.simple.oauth.view.sysMenu;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simple.oauth.common.entity.sysMenu.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单权限(sys_menu)数据库访问层
 *
 * @author qty
 */
@Mapper
public interface SysMenuRepository extends BaseMapper<SysMenu> {

    /**
     * 批量新增数据（MyBatis原生foreach方法，MP表的自动化操作都无效，需要手动为集合对象赋值）
     *
     * @param entities List<SysMenu> 实例对象列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<SysMenu> entities);

    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     */
    List<SysMenu> findAllByRoleId(@Param("roleId") String roleId,@Param("clientId") String clientId);

    /**
     * 根据用户ID获取权限菜单
     *
     * @param userId 用户ID
     */
    List<SysMenu> findAllByUserId(@Param("userId") String userId,@Param("clientId") String clientId);

    /**
     * 根据角色key获取菜单列表
     *
     * @param roleKey 角色ID
     */
    List<SysMenu> findAllByRoleKey(@Param("roleKey") String roleKey);

}


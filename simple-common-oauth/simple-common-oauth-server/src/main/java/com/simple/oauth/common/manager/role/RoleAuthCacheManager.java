package com.simple.oauth.common.manager.role;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 角色权限缓存
 *
 * @author 兄台丶请冷静
 */
public interface RoleAuthCacheManager {

    /**
     * 缓存
     * @param roleKey 角色code
     * @param sysMenuIds 权限菜单ID
     */
    void put(String roleKey, List<String> sysMenuIds);

    /**
     * 获取
     *
     * @param roleKey 角色code
     * @return
     */
    Map<Object, Object> get(String roleKey);

    /**
     * 更新全部角色缓存
     */
    void update();

}

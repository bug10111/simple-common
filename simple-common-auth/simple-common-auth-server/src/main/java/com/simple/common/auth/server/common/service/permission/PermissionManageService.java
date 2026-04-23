package com.simple.common.auth.server.common.service.permission;

import java.util.Map;

/**
 * 权限管理服务接口
 * <p>
 * 提供角色权限的增删改查功能，权限修改后会自动发布事件通知所有客户端刷新缓存。
 * 用户集成后直接注入此服务即可进行权限管理操作。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private PermissionManageService permissionManageService;
 * 
 * // 更新角色权限
 * Map<String, String> permissions = new HashMap<>();
 * permissions.put("user:create", "创建用户");
 * permissions.put("user:delete", "删除用户");
 * permissionManageService.updateRolePermission("admin", permissions);
 * }</pre>
 *
 * @author qty
 */
public interface PermissionManageService {

    /**
     * 更新角色权限
     * <p>
     * 更新指定角色的权限配置，并发布事件通知所有客户端刷新缓存。
     * </p>
     *
     * @param roleKey     角色标识，如 "admin"、"editor"
     * @param permissions 新的权限配置，key 为权限标识，value 为权限描述或配置值
     */
    void updateRolePermission(String roleKey, Map<String, String> permissions);

    /**
     * 批量更新多个角色的权限
     * <p>
     * 同时更新多个角色的权限配置，并发布一次事件通知所有客户端。
     * 适用于批量调整权限的场景，减少事件发布次数。
     * </p>
     *
     * @param roleKeys 需要刷新的角色标识列表
     */
    void batchRefreshPermissions(java.util.List<String> roleKeys);

    /**
     * 删除角色的某个权限
     *
     * @param roleKey       角色标识
     * @param permissionKey 要删除的权限标识
     */
    void deletePermission(String roleKey, String permissionKey);

    /**
     * 获取指定角色的权限
     *
     * @param roleKey 角色标识
     * @return 权限 Map，key 为权限标识，value 为权限描述或配置值
     */
    Map<Object, Object> getRolePermission(String roleKey);
}

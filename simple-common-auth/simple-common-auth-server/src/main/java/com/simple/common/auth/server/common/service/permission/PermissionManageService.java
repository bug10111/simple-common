package com.simple.common.auth.server.common.service.permission;

import java.util.List;
import java.util.Map;

/**
 * 权限管理服务接口
 * <p>
 * 提供角色权限的增删改查功能，支持项目维度隔离。
 * 权限修改后会自动发布事件通知所有客户端刷新缓存。
 * </p>
 *
 * <h3>核心设计理念：</h3>
 * <ul>
 *   <li><strong>项目隔离</strong>：每个项目的权限变更只影响该项目下的角色和客户端</li>
 *   <li><strong>精准推送</strong>：事件只发送变更的数据，不发送全量权限</li>
 *   <li><strong>高性能</strong>：避免不必要的缓存查询，减少 Redis I/O</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Autowired
 * private PermissionManageService permissionManageService;
 * 
 * // 按项目新增权限
 * Map<String, String> newPerms = new HashMap<>();
 * newPerms.put("user:create", "创建用户");
 * permissionManageService.addPermissions("xiaoyue-web", "admin", newPerms);
 * }</pre>
 *
 * @author qty
 */
public interface PermissionManageService {

    /**
     * 新增权限（按项目维度）
     * <p>
     * 向指定项目的指定角色添加新权限，不影响现有权限。
     * 如果权限已存在，则覆盖其描述信息。
     * </p>
     *
     * @param projectCode 项目编码（clientName），如 "xiaoyue-web"
     * @param roleKey     角色标识，如 "admin"、"editor"
     * @param permissions 要添加的权限配置，key 为权限标识，value 为权限描述
     */
    void addPermissions(String projectCode, String roleKey, Map<String, String> permissions);

    /**
     * 全量替换角色权限（按项目维度）
     * <p>
     * 先删除角色的所有旧权限，再设置新的权限配置。
     * 适用于需要完全重置角色权限的场景。
     * </p>
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 新的权限配置，key 为权限标识，value 为权限描述
     */
    void updateRolePermission(String projectCode, String roleKey, Map<String, String> permissions);

    /**
     * 删除单个权限（按项目维度）
     * <p>
     * 从指定项目的指定角色中删除某个权限标识。
     * </p>
     *
     * @param projectCode   项目编码
     * @param roleKey       角色标识
     * @param permissionKey 要删除的权限标识
     */
    void deletePermission(String projectCode, String roleKey, String permissionKey);

    /**
     * 批量删除权限（按项目维度）
     * <p>
     * 从指定项目的指定角色中删除多个权限标识。
     * </p>
     *
     * @param projectCode    项目编码
     * @param roleKey        角色标识
     * @param permissionKeys 要删除的权限标识列表
     */
    void deletePermissions(String projectCode, String roleKey, List<String> permissionKeys);

    /**
     * 清空角色所有权限（按项目维度）
     * <p>
     * 删除指定项目的指定角色的所有权限配置。
     * </p>
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     */
    void clearPermissions(String projectCode, String roleKey);
}
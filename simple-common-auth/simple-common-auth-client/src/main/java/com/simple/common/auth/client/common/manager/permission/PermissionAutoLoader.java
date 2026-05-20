package com.simple.common.auth.client.common.manager.permission;

import java.util.Map;

/**
 * 权限自动加载器 SPI 接口。
 * <p>
 * 当缓存中的权限数据不存在或过期时，框架会通过此接口回调业务方，
 * 由业务方从自身数据库查询并返回指定角色在指定项目下的权限数据。
 * 业务方实现此接口后，框架会自动注入并使用。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>授权中心登录后预缓存权限时，通过此接口查询业务数据库</li>
 *   <li>权限缓存过期后，客户端侧自动补全权限数据</li>
 *   <li>Redis 重启导致权限数据丢失后的自动恢复</li>
 * </ul>
 *
 * <h3>实现示例：</h3>
 * <pre>{@code
 * @Component
 * public class SysRoleMenuPermissionLoader implements PermissionAutoLoader {
 *     @Autowired
 *     private SysRoleMenuView sysRoleMenuView;
 *
 *     @Override
 *     public Map<String, String> loadPermissions(String roleKey, String projectCode) {
 *         List<String> perms = sysRoleMenuView.findPermsByRoleKeyAndProjectCode(roleKey, projectCode);
 *         if (perms == null || perms.isEmpty()) {
 *             return Map.of();
 *         }
 *         return perms.stream()
 *             .filter(p -> p != null && !p.isEmpty())
 *             .collect(Collectors.toMap(p -> p, p -> ""));
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
@FunctionalInterface
public interface PermissionAutoLoader {

    /**
     * 加载指定角色在指定项目下的权限数据。
     * <p>
     * 当框架检测到缓存中缺少权限数据时，会调用此方法获取最新的权限配置。
     * 返回的 Map 中，key 为权限标识（如 "sys:user:create"），value 为权限描述。
     * 如果角色在该项目下没有权限，应返回空 Map。
     * </p>
     *
     * @param roleKey     角色标识，如 "admin"、"editor"
     * @param projectCode 项目编码（clientId），如 "xiaoyue-web-client"
     * @return 权限标识 -> 权限描述的 Map，不可返回 null
     */
    Map<String, String> loadPermissions(String roleKey, String projectCode);
}

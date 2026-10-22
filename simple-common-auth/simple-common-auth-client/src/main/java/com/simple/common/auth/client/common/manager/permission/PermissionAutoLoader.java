package com.simple.common.auth.client.common.manager.permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 权限自动加载器 SPI 接口。
 * <p>
 * 当缓存中的权限数据不存在或过期时，框架会通过此接口回调业务方，
 * 由业务方从自身数据库查询并返回指定角色在指定项目（及租户）下的权限数据。
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
 * <h3>实现注意事项：</h3>
 * <ul>
 *   <li>roleKey/projectCode 由框架保证非空（TokenConstant#getAuthKey 已断言），tenantId 可能为 null
 *       （无租户归属的历史角色），实现方按 tenantId 判空分支处理；</li>
 *   <li>多租户部署必须覆写三参 {@link #loadPermissions(String, String, String)}，按 tenantId 过滤——
 *       未覆写时退回项目级查询会把全部租户的权限聚合写入租户专属缓存键（跨租户越权放大）；</li>
 *   <li>返回 Map 不可为 null，无权限返回空 Map；</li>
 *   <li>示例中的收集器必须带 merge 函数（重复权限串在角色-菜单联查/重复行场景常见，缺省收集器会抛
 *       IllegalStateException: Duplicate key）。</li>
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
 *             .collect(Collectors.toMap(p -> p, p -> "", (a, b) -> a));
 *     }
 * }
 * }</pre>
 *
 * @author qty
 */
@FunctionalInterface
public interface PermissionAutoLoader {

    /**
     * 租户维度未覆写告警的幂等标记（接口常量，进程内共享）
     */
    AtomicBoolean TENANT_DELEGATE_WARNED = new AtomicBoolean(false);

    /**
     * SPI 接口告警 logger
     */
    Logger LOG = LoggerFactory.getLogger(PermissionAutoLoader.class);

    /**
     * 加载指定角色在指定项目下的权限数据。
     * <p>
     * 当框架检测到缓存中缺少权限数据时，会调用此方法获取最新的权限配置。
     * 返回的 Map 中，key 为权限标识（如 "sys:user:create"），value 为权限描述。
     * 如果角色在该项目下没有权限，应返回空 Map（不可返回 null）。
     * </p>
     *
     * @param roleKey     角色标识，如 "admin"、"editor"（框架保证非空）
     * @param projectCode 项目编码（clientId），如 "xiaoyue-web-client"（框架保证非空）
     * @return 权限标识 -> 权限描述的 Map，不可返回 null
     */
    Map<String, String> loadPermissions(String roleKey, String projectCode);

    /**
     * 加载指定角色在指定项目、指定租户下的权限数据（多租户形态；默认委托两参实现保持兼容）。
     * <p>
     * 业务实现可覆写本方法按租户过滤；未覆写时<b>忽略租户维度</b>退回项目级查询——
     * 框架会以一次性告警提示（tenantId 非空时），多租户部署必须覆写以避免跨租户权限聚合。
     * </p>
     *
     * @param roleKey     角色标识（非空）
     * @param projectCode 项目编码（clientId，非空）
     * @param tenantId    租户 ID（可能为 null——无租户归属的历史角色）
     * @return 权限标识 -> 权限描述的 Map，不可返回 null
     */
    default Map<String, String> loadPermissions(String roleKey, String projectCode, String tenantId) {
        if (tenantId != null) {
            TENANT_DELEGATE_WARNED.compareAndSet(false, true);
            if (TENANT_DELEGATE_WARNED.get()) {
                // 只告警一次：未覆写租户维度时退回项目级查询，权限将聚合到租户专属缓存键（跨租户越权风险）
                LOG.warn("PermissionAutoLoader 未覆写三参 loadPermissions（tenantId={}），已退回项目级查询；" +
                        "多租户部署必须覆写并按租户过滤，否则权限缓存将跨租户聚合", tenantId);
            }
        }
        return loadPermissions(roleKey, projectCode);
    }
}

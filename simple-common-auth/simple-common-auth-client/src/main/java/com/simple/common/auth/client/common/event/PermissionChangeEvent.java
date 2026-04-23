package com.simple.common.auth.client.common.event;

import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 权限变更事件
 * <p>
 * 当服务端修改角色权限时，发布此事件通知所有客户端刷新权限缓存。
 * 事件中包含完整的权限数据，客户端接收后可直接更新缓存，无需额外HTTP请求。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>管理员修改角色权限配置</li>
 *   <li>批量调整多个角色的权限</li>
 *   <li>删除角色或权限标识</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 服务端修改权限后发布事件
 * Map<String, String> permissions = new HashMap<>();
 * permissions.put("user:create", "创建用户");
 * permissions.put("user:delete", "删除用户");
 * 
 * PermissionChangeEvent event = new PermissionChangeEvent()
 *     .setRoleKey("admin")
 *     .setPermissions(permissions)
 *     .setChangeType("UPDATE")
 *     .setTimestamp(System.currentTimeMillis());
 * 
 * eventBusService.push(event);
 * }</pre>
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
@Event(targets = {EventConstant.TARGET_ALL_X}) // 发送到所有系统
public class PermissionChangeEvent {

    /**
     * 变更的角色标识
     * <p>
     * 单个角色的权限变更，如 "admin"、"editor"
     * </p>
     */
    private String roleKey;

    /**
     * 变更的权限数据
     * <p>
     * key 为权限标识，value 为权限描述或配置值
     * 对于 DELETE 操作，此字段可为空或包含要删除的权限列表
     * </p>
     */
    private Map<String, String> permissions;

    /**
     * 变更类型
     * <ul>
     *   <li>ADD - 新增权限</li>
     *   <li>UPDATE - 更新权限（全量替换）</li>
     *   <li>DELETE - 删除权限或角色</li>
     * </ul>
     */
    private String changeType;

    /**
     * 操作时间戳（毫秒）
     * <p>
     * 用于日志记录和事件追踪
     * </p>
     */
    private Long timestamp;
}

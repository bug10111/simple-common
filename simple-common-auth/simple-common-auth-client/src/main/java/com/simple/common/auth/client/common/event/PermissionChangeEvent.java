package com.simple.common.auth.client.common.event;

import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 权限变更事件
 * <p>
 * 当服务端修改角色权限时，发布此事件通知所有客户端刷新权限缓存。
 * 事件中包含完整的权限数据、变更类型和项目编码，客户端根据类型执行不同的缓存更新策略。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>管理员新增角色权限（ADD）</li>
 *   <li>管理员全量替换角色权限（UPDATE）</li>
 *   <li>管理员删除角色部分权限（DELETE）</li>
 *   <li>管理员清空角色所有权限（CLEAR）</li>
 *   <li>批量刷新多个角色权限（REFRESH）</li>
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
 *     .setProjectCode("xiaoyue")
 *     .setRoleKey("admin")
 *     .setPermissions(permissions)
 *     .setChangeType(PermissionChangeTypeEnum.UPDATE)
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
     * 项目编码（必填）
     * <p>
     * 用于标识权限所属的项目，实现项目维度的权限隔离。
     * 即使是单体架构应用，也应使用 Spring Application Name 作为项目编码。
     * </p>
     */
    private String projectCode;

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
     * 对于 CLEAR 操作，此字段为空 Map
     * </p>
     */
    private Map<String, String> permissions;

    /**
     * 变更类型
     * <ul>
     *   <li>ADD - 新增权限（增量添加）</li>
     *   <li>UPDATE - 更新权限（全量替换）</li>
     *   <li>DELETE - 删除权限</li>
     *   <li>CLEAR - 清空所有权限</li>
     *   <li>REFRESH - 刷新权限</li>
     * </ul>
     */
    private PermissionChangeTypeEnum changeType;

    /**
     * 操作时间戳（毫秒）
     * <p>
     * 用于日志记录和事件追踪
     * </p>
     */
    private Long timestamp;
}

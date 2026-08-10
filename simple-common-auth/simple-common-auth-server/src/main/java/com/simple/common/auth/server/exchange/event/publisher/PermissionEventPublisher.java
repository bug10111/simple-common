package com.simple.common.auth.server.exchange.event.publisher;

import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import com.simple.common.auth.client.exchange.event.event.PermissionChangeEvent;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限变更事件发布器。
 * <p>
 * 专职负责构造并发布权限变更事件，通知所有客户端刷新权限缓存。
 * 发布器只负责构造 + push 事件，缓存清理等业务操作仍留在原 service，保持职责单一。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class PermissionEventPublisher {

    @Autowired
    private EventBusService eventBusService;

    /**
     * 发布权限变更事件，广播到所有客户端刷新权限缓存。
     * <p>
     * 事件携带完整的权限数据、变更类型与时间戳，客户端根据变更类型执行不同的缓存更新策略。
     * </p>
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 权限数据（key 为权限标识，value 为权限描述）
     * @param changeType  变更类型
     */
    public void publish(String projectCode, String roleKey, Map<String, String> permissions, PermissionChangeTypeEnum changeType) {
        PermissionChangeEvent event = new PermissionChangeEvent().setRoleKey(roleKey)
                                                                 .setProjectCode(projectCode)
                                                                 .setPermissions(permissions != null ? permissions : new HashMap<>())
                                                                 .setChangeType(changeType)
                                                                 .setTimestamp(System.currentTimeMillis());

        eventBusService.push(event);
    }
}

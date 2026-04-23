package com.simple.common.auth.client.listener;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.event.PermissionChangeEvent;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.eventbus.common.annotation.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限变更事件处理器
 * <p>
 * 监听服务端的权限变更事件，直接从事件中获取权限数据并更新本地缓存。
 * 无需额外HTTP请求，避免客户端过多时产生的并发雪崩问题。
 * </p>
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>管理员在服务端修改角色权限（调用 PermissionManageService）</li>
 *   <li>服务端发布 PermissionChangeEvent 事件（携带完整权限数据）</li>
 *   <li>所有客户端接收到事件</li>
 *   <li>客户端直接从事件中获取权限数据并更新 CacheManager 缓存</li>
 * </ol>
 *
 * @author qty
 */
@Slf4j
@Component
public class PermissionChangeEventHandler {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    /**
     * 处理权限变更事件
     * <p>
     * 当接收到权限变更事件时，直接从事件中获取权限数据并更新缓存。
     * 采用主动推送方式，无需额外HTTP请求，避免大量客户端同时请求服务端。
     * </p>
     *
     * @param event 权限变更事件，包含角色标识和完整的权限数据
     */
    @EventHandler
    public void onPermissionChange(PermissionChangeEvent event) {
        log.info("收到权限变更事件，角色: {}, 类型: {}", event.getRoleKey(), event.getChangeType());

        String roleKey = event.getRoleKey();
        if (roleKey == null || roleKey.isEmpty()) {
            log.warn("权限变更事件中角色标识为空，忽略处理");
            return;
        }

        Map<String, String> permissions = event.getPermissions();
        if (permissions == null) {
            log.warn("角色 [{}] 的权限数据为空，忽略处理", roleKey);
            return;
        }

        try {
            // 直接更新缓存（CacheManager 会根据配置自动选择 Redis 或 Local）
            String authKey = TokenConstant.getAuthKey(roleKey);
            
            // 将 Map<String, String> 转换为 Map<Object, Object>
            Map<Object, Object> permissionMap = new HashMap<>(permissions);
            cacheManager.hashPutAll(authKey, permissionMap);

            log.info("角色 [{}] 的权限缓存已同步更新，共 {} 个权限", roleKey, permissions.size());
        } catch (Exception e) {
            log.error("同步角色 [{}] 权限缓存失败", roleKey, e);
        }
    }
}

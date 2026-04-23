package com.simple.common.auth.server.service.permission;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.event.PermissionChangeEvent;
import com.simple.common.auth.server.common.service.permission.PermissionManageService;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限管理服务实现
 * <p>
 * 提供角色权限的增删改查功能，权限修改后会自动发布事件通知所有客户端刷新缓存。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultPermissionManageService implements PermissionManageService {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private EventBusService eventBusService;

    /**
     * 更新角色权限并发布变更事件
     *
     * @param roleKey     角色标识
     * @param permissions 新的权限配置
     */
    @Override
    public void updateRolePermission(String roleKey, Map<String, String> permissions) {
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notNull(permissions, "权限配置不能为空");

        // 1. 更新 Redis 中的权限缓存
        String authKey = TokenConstant.getAuthKey(roleKey);
        Map<Object, Object> permissionMap = new HashMap<>(permissions);
        cacheManager.hashPutAll(authKey, permissionMap);

        // 2. 设置过期时间（例如 30 分钟）
        cacheManager.expire(authKey, 30 * 60);

        // 3. 发布权限变更事件，携带完整权限数据，客户端可直接更新缓存
        PermissionChangeEvent event = new PermissionChangeEvent()
                .setRoleKey(roleKey)
                .setPermissions(permissions)
                .setChangeType("UPDATE")
                .setTimestamp(System.currentTimeMillis());

        eventBusService.push(event);

        log.info("角色 [{}] 权限已更新，共 {} 个权限，事件已发布", roleKey, permissions.size());
    }

    /**
     * 批量刷新多个角色的权限
     * <p>
     * 注意：批量操作不携带具体权限数据，客户端接收后需要自行从服务端拉取
     * </p>
     *
     * @param roleKeys 需要刷新的角色标识列表
     */
    @Override
    public void batchRefreshPermissions(List<String> roleKeys) {
        AssertUtils.notEmpty(roleKeys, "角色列表不能为空");

        // 批量操作时，逐个发布事件，让客户端重新拉取最新权限
        for (String roleKey : roleKeys) {
            Map<Object, Object> permissions = getRolePermission(roleKey);
            Map<String, String> permMap = new HashMap<>();
            permissions.forEach((k, v) -> permMap.put(k.toString(), v != null ? v.toString() : ""));
            
            PermissionChangeEvent event = new PermissionChangeEvent()
                    .setRoleKey(roleKey)
                    .setPermissions(permMap)
                    .setChangeType("UPDATE")
                    .setTimestamp(System.currentTimeMillis());

            eventBusService.push(event);
        }

        log.info("批量刷新权限事件已发布，角色数量: {}", roleKeys.size());
    }

    /**
     * 删除角色的某个权限
     *
     * @param roleKey       角色标识
     * @param permissionKey 要删除的权限标识
     */
    @Override
    public void deletePermission(String roleKey, String permissionKey) {
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(permissionKey, "权限标识不能为空");

        String authKey = TokenConstant.getAuthKey(roleKey);
        cacheManager.hashDelete(authKey, permissionKey);

        // 获取删除后的最新权限数据
        Map<Object, Object> remainingPermissions = cacheManager.hashGetAll(authKey);
        Map<String, String> permMap = new HashMap<>();
        remainingPermissions.forEach((k, v) -> permMap.put(k.toString(), v != null ? v.toString() : ""));

        // 发布权限变更事件，携带删除后的最新权限数据
        PermissionChangeEvent event = new PermissionChangeEvent()
                .setRoleKey(roleKey)
                .setPermissions(permMap)
                .setChangeType("DELETE")
                .setTimestamp(System.currentTimeMillis());

        eventBusService.push(event);

        log.info("角色 [{}] 的权限 [{}] 已删除，事件已发布", roleKey, permissionKey);
    }

    /**
     * 获取指定角色的权限
     *
     * @param roleKey 角色标识
     * @return 权限 Map
     */
    @Override
    public Map<Object, Object> getRolePermission(String roleKey) {
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");

        String authKey = TokenConstant.getAuthKey(roleKey);
        return cacheManager.hashGetAll(authKey);
    }
}

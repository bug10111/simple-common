package com.simple.common.auth.server.service.permission;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import com.simple.common.auth.client.common.event.PermissionChangeEvent;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
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
 * 提供角色权限的增删改查功能，支持项目维度隔离。
 * 权限修改后会自动发布事件通知所有客户端刷新缓存。
 * </p>
 *
 * <h3>核心设计理念：</h3>
 * <ul>
 *   <li><strong>项目隔离</strong>：每个项目的权限变更只影响该项目下的角色和客户端</li>
 *   <li><strong>精准推送</strong>：菜单变更时，只刷新关联项目的角色权限</li>
 *   <li><strong>强制项目维度</strong>：所有操作必须指定 projectCode，确保权限隔离</li>
 * </ul>
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

    @Autowired
    private AuthProperties authProperties;

    /**
     * 新增权限（按项目维度）
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 要添加的权限配置
     */
    @Override
    public void addPermissions(String projectCode, String roleKey, Map<String, String> permissions) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notNull(permissions, "权限配置不能为空");
        AssertUtils.isTrue(!permissions.isEmpty(), "权限配置不能为空Map");

        String authKey = TokenConstant.getAuthKey(projectCode, roleKey);
        
        // 1. 增量添加权限（不删除现有权限）
        Map<Object, Object> permissionMap = new HashMap<>(permissions);
        cacheManager.hashPutAll(authKey, permissionMap);

        // 2. 设置过期时间（从配置文件读取，默认24小时）
        Integer expireSeconds = authProperties.getPermissionCacheExpire();
        cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : 60 * 60 * 24);

        // 3. 获取最新权限数据并发布事件
        Map<Object, Object> latestPermissions = cacheManager.hashGetAll(authKey);
        publishPermissionEvent(projectCode, roleKey, latestPermissions, PermissionChangeTypeEnum.ADD);

        log.info("项目 [{}] 角色 [{}] 新增了 {} 个权限，事件已发布", projectCode, roleKey, permissions.size());
    }

    /**
     * 更新角色权限（按项目维度，全量替换）
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 新的权限配置
     */
    @Override
    public void updateRolePermission(String projectCode, String roleKey, Map<String, String> permissions) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notNull(permissions, "权限配置不能为空");

        String authKey = TokenConstant.getAuthKey(projectCode, roleKey);
        
        // 1. 先删除所有旧权限
        cacheManager.delete(authKey);
        
        // 2. 再设置新权限
        Map<Object, Object> permissionMap = new HashMap<>(permissions);
        cacheManager.hashPutAll(authKey, permissionMap);

        // 3. 设置过期时间（从配置文件读取，默认24小时）
        Integer expireSeconds = authProperties.getPermissionCacheExpire();
        cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : 60 * 60 * 24);

        // 4. 发布权限变更事件
        Map<Object, Object> permObjMap = new HashMap<>(permissions);
        publishPermissionEvent(projectCode, roleKey, permObjMap, PermissionChangeTypeEnum.UPDATE);

        log.info("项目 [{}] 角色 [{}] 权限已全量更新，共 {} 个权限，事件已发布", projectCode, roleKey, permissions.size());
    }

    /**
     * 批量刷新多个角色的权限（按项目维度）
     *
     * @param projectCode 项目编码
     * @param roleKeys    需要刷新的角色标识列表
     */
    @Override
    public void batchRefreshPermissions(String projectCode, List<String> roleKeys) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKeys, "角色列表不能为空");

        for (String roleKey : roleKeys) {
            Map<Object, Object> permissions = getRolePermission(projectCode, roleKey);
            publishPermissionEvent(projectCode, roleKey, permissions, PermissionChangeTypeEnum.REFRESH);
        }

        log.info("项目 [{}] 批量刷新权限事件已发布，角色数量: {}", projectCode, roleKeys.size());
    }

    /**
     * 删除单个权限（按项目维度）
     *
     * @param projectCode   项目编码
     * @param roleKey       角色标识
     * @param permissionKey 要删除的权限标识
     */
    @Override
    public void deletePermission(String projectCode, String roleKey, String permissionKey) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(permissionKey, "权限标识不能为空");

        String authKey = TokenConstant.getAuthKey(projectCode, roleKey);
        cacheManager.hashDelete(authKey, permissionKey);

        // 获取删除后的最新权限数据
        Map<Object, Object> remainingPermissions = cacheManager.hashGetAll(authKey);
        publishPermissionEvent(projectCode, roleKey, remainingPermissions, PermissionChangeTypeEnum.DELETE);

        log.info("项目 [{}] 角色 [{}] 的权限 [{}] 已删除，事件已发布", projectCode, roleKey, permissionKey);
    }

    /**
     * 批量删除权限（按项目维度）
     *
     * @param projectCode    项目编码
     * @param roleKey        角色标识
     * @param permissionKeys 要删除的权限标识列表
     */
    @Override
    public void deletePermissions(String projectCode, String roleKey, List<String> permissionKeys) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(permissionKeys, "权限标识列表不能为空");

        String authKey = TokenConstant.getAuthKey(projectCode, roleKey);
        
        // 批量删除指定权限
        for (String permissionKey : permissionKeys) {
            cacheManager.hashDelete(authKey, permissionKey);
        }

        // 获取删除后的最新权限数据
        Map<Object, Object> remainingPermissions = cacheManager.hashGetAll(authKey);
        publishPermissionEvent(projectCode, roleKey, remainingPermissions, PermissionChangeTypeEnum.DELETE);

        log.info("项目 [{}] 角色 [{}] 批量删除了 {} 个权限，事件已发布", projectCode, roleKey, permissionKeys.size());
    }

    /**
     * 清空角色所有权限（按项目维度）
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     */
    @Override
    public void clearPermissions(String projectCode, String roleKey) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");

        String authKey = TokenConstant.getAuthKey(projectCode, roleKey);
        cacheManager.delete(authKey);

        // 发布空权限事件
        publishPermissionEvent(projectCode, roleKey, new HashMap<>(), PermissionChangeTypeEnum.CLEAR);

        log.info("项目 [{}] 角色 [{}] 的所有权限已清空，事件已发布", projectCode, roleKey);
    }

    /**
     * 获取指定角色的权限（按项目维度）
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @return 权限 Map
     */
    @Override
    public Map<Object, Object> getRolePermission(String projectCode, String roleKey) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");

        String authKey = TokenConstant.getAuthKey(projectCode, roleKey);
        return cacheManager.hashGetAll(authKey);
    }

    /**
     * 发布权限变更事件
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 权限数据
     * @param changeType  变更类型枚举
     */
    private void publishPermissionEvent(String projectCode, String roleKey, Map<Object, Object> permissions, PermissionChangeTypeEnum changeType) {
        Map<String, String> permMap = new HashMap<>();
        if (permissions != null && !permissions.isEmpty()) {
            permissions.forEach((k, v) -> permMap.put(k.toString(), v != null ? v.toString() : ""));
        }

        PermissionChangeEvent event = new PermissionChangeEvent()
                .setProjectCode(projectCode)
                .setRoleKey(roleKey)
                .setPermissions(permMap)
                .setChangeType(changeType)
                .setTimestamp(System.currentTimeMillis());

        eventBusService.push(event);
    }
}

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
        addPermissions(projectCode, roleKey, permissions, true);
    }

    /**
     * 新增权限（按项目维度，支持控制是否发布事件）
     *
     * @param projectCode  项目编码
     * @param roleKey      角色标识
     * @param permissions  要添加的权限配置
     * @param publishEvent 是否发布权限变更事件广播到所有客户端
     */
    @Override
    public void addPermissions(String projectCode, String roleKey, Map<String, String> permissions, boolean publishEvent) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notNull(permissions, "权限配置不能为空");
        AssertUtils.isTrue(!permissions.isEmpty(), "权限配置不能为空Map");

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode);

        // 1. 增量添加权限（不删除现有权限）
        cacheManager.hashPutAll(authKey, new HashMap<>(permissions));

        // 2. 设置过期时间（从配置文件读取，默认24小时）
        cacheManager.expire(authKey, authProperties.getPermissionCacheExpire());

        // 3. 根据参数决定是否发布事件
        if (publishEvent) {
            publishPermissionEvent(projectCode, roleKey, permissions, PermissionChangeTypeEnum.ADD);
            log.info("项目 [{}] 角色 [{}] 新增了 {} 个权限，事件已发布", projectCode, roleKey, permissions.size());
        } else {
            log.debug("项目 [{}] 角色 [{}] 新增了 {} 个权限（未发布事件）", projectCode, roleKey, permissions.size());
        }
    }

    /**
     * 全量替换角色权限（按项目维度）
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

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode);

        // 1. 先删除所有旧权限
        cacheManager.delete(authKey);

        // 2. 再设置新权限
        cacheManager.hashPutAll(authKey, new HashMap<>(permissions));

        // 3. 设置过期时间（从配置文件读取，默认24小时）
        cacheManager.expire(authKey, authProperties.getPermissionCacheExpire());

        // 4. 发布权限变更事件
        publishPermissionEvent(projectCode, roleKey, permissions, PermissionChangeTypeEnum.UPDATE);

        log.info("项目 [{}] 角色 [{}] 权限已全量更新，共 {} 个权限，事件已发布", projectCode, roleKey, permissions.size());
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

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode);
        cacheManager.hashDelete(authKey, permissionKey);

        // 发布事件（只发送被删除的权限标识）
        Map<String, String> deletedPerm = new HashMap<>();
        deletedPerm.put(permissionKey, "");
        publishPermissionEvent(projectCode, roleKey, deletedPerm, PermissionChangeTypeEnum.DELETE);

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

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode);

        // 批量删除指定权限（使用可变参数一次删除，消除多次网络IO）
        cacheManager.hashDelete(authKey, permissionKeys.toArray(new String[0]));

        // 发布事件（只发送被删除的权限列表）
        Map<String, String> deletedPerms = new HashMap<>();
        for (String key : permissionKeys) {
            deletedPerms.put(key, "");
        }
        publishPermissionEvent(projectCode, roleKey, deletedPerms, PermissionChangeTypeEnum.DELETE);

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

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode);
        cacheManager.delete(authKey);

        // 发布空权限事件
        publishPermissionEvent(projectCode, roleKey, new HashMap<>(), PermissionChangeTypeEnum.CLEAR);

        log.info("项目 [{}] 角色 [{}] 的所有权限已清空，事件已发布", projectCode, roleKey);
    }

    /**
     * 发布权限变更事件
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 权限数据（key为权限标识，value为权限描述）
     * @param changeType  变更类型枚举
     */
    private void publishPermissionEvent(String projectCode, String roleKey, Map<String, String> permissions, PermissionChangeTypeEnum changeType) {
        PermissionChangeEvent event = new PermissionChangeEvent().setRoleKey(roleKey)
                                                                 .setProjectCode(projectCode)
                                                                 .setPermissions(permissions != null ? permissions : new HashMap<>())
                                                                 .setChangeType(changeType)
                                                                 .setTimestamp(System.currentTimeMillis());

        eventBusService.push(event);
    }
}
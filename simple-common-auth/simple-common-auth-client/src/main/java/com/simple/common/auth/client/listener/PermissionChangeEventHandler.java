package com.simple.common.auth.client.listener;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import com.simple.common.auth.client.common.event.PermissionChangeEvent;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
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
 * 监听服务端的权限变更事件，根据变更类型执行不同的缓存更新策略。
 * 无需额外HTTP请求，避免客户端过多时产生的并发雪崩问题。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>管理员在服务端修改角色权限（调用 PermissionManageService）</li>
 *   <li>服务端发布 PermissionChangeEvent 事件（携带完整权限数据和变更类型）</li>
 *   <li>所有客户端接收到事件</li>
 *   <li>客户端根据 changeType 执行不同的缓存更新策略：</li>
 *   <ul>
 *     <li>ADD - 增量添加权限到现有缓存</li>
 *     <li>UPDATE - 清空旧缓存，全量设置新权限</li>
 *     <li>DELETE - 从缓存中删除指定权限</li>
 *     <li>CLEAR - 清空角色的所有权限缓存</li>
 *     <li>REFRESH - 重新拉取最新权限数据（当前实现同 UPDATE）</li>
 *   </ul>
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

    @Autowired
    private AuthProperties authProperties;

    /**
     * 处理权限变更事件
     * <p>
     * 根据变更类型执行不同的缓存更新策略：
     * <ul>
     *   <li>ADD - 增量添加权限（不影响现有权限）</li>
     *   <li>UPDATE - 全量替换权限（先清空再设置）</li>
     *   <li>DELETE - 删除指定权限</li>
     *   <li>CLEAR - 清空所有权限</li>
     *   <li>REFRESH - 刷新权限（同 UPDATE）</li>
     * </ul>
     * </p>
     *
     * <h3>项目过滤机制：</h3>
     * <p>
     * 客户端只会处理与自身项目编码匹配的事件，避免跨项目权限污染。
     * 如果未配置项目编码，则默认处理所有事件（单体架构场景）。
     * </p>
     *
     * @param event 权限变更事件，包含角色标识、权限数据和变更类型
     */
    @EventHandler
    public void onPermissionChange(PermissionChangeEvent event) {
        String roleKey = event.getRoleKey();
        if (roleKey == null || roleKey.isEmpty()) {
            log.warn("权限变更事件中角色标识为空，忽略处理");
            return;
        }

        String projectCode = event.getProjectCode();
        if (projectCode == null || projectCode.isEmpty()) {
            log.warn("角色 [{}] 的项目编码为空，忽略处理", roleKey);
            return;
        }

        // 项目过滤：只处理与自身项目匹配的事件
        String currentProjectCode = authProperties.getProjectCode();
        if (currentProjectCode != null && !currentProjectCode.isEmpty() 
                && !currentProjectCode.equals(projectCode)) {
            log.debug("跳过非本项目 [{}] 的权限变更事件，当前项目为 [{}]", projectCode, currentProjectCode);
            return;
        }

        PermissionChangeTypeEnum changeType = event.getChangeType();
        if (changeType == null) {
            log.warn("项目 [{}] 角色 [{}] 的变更类型为空，忽略处理", projectCode, roleKey);
            return;
        }

        Map<String, String> permissions = event.getPermissions();
        String authKey = TokenConstant.getAuthKey(roleKey, projectCode);

        try {
            switch (changeType) {
                case ADD:
                    handleAddPermission(authKey, permissions);
                    break;
                case UPDATE:
                case REFRESH:
                    handleUpdatePermission(authKey, permissions);
                    break;
                case DELETE:
                    handleDeletePermission(authKey, permissions);
                    break;
                case CLEAR:
                    handleClearPermission(authKey);
                    break;
                default:
                    log.warn("未知的变更类型: {}", changeType);
                    break;
            }
        } catch (Exception e) {
            log.error("处理角色 [{}] 权限变更事件失败, 类型: {}", roleKey, changeType, e);
        }
    }

    /**
     * 处理新增权限（增量添加）
     */
    private void handleAddPermission(String authKey, Map<String, String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            log.warn("新增权限数据为空，忽略处理");
            return;
        }

        Map<Object, Object> permissionMap = new HashMap<>(permissions);
        cacheManager.hashPutAll(authKey, permissionMap);

        // 设置过期时间
        Integer expireSeconds = authProperties.getPermissionCacheExpire();
        cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : 60 * 60 * 24);

        log.info("角色权限缓存已增量添加 {} 个权限", permissions.size());
    }

    /**
     * 处理更新权限（全量替换）
     */
    private void handleUpdatePermission(String authKey, Map<String, String> permissions) {
        if (permissions == null) {
            log.warn("更新权限数据为null，忽略处理");
            return;
        }

        // 先清空旧缓存
        cacheManager.delete(authKey);

        // 再设置新权限
        if (!permissions.isEmpty()) {
            Map<Object, Object> permissionMap = new HashMap<>(permissions);
            cacheManager.hashPutAll(authKey, permissionMap);

            // 设置过期时间
            Integer expireSeconds = authProperties.getPermissionCacheExpire();
            cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : 60 * 60 * 24);
        }

        log.info("角色权限缓存已全量更新，共 {} 个权限", permissions.size());
    }

    /**
     * 处理删除权限
     */
    private void handleDeletePermission(String authKey, Map<String, String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            log.warn("删除权限数据为空，忽略处理");
            return;
        }

        // 删除指定的权限字段
        for (String permissionKey : permissions.keySet()) {
            cacheManager.hashDelete(authKey, permissionKey);
        }

        log.info("角色权限缓存已删除 {} 个权限", permissions.size());
    }

    /**
     * 处理清空所有权限
     */
    private void handleClearPermission(String authKey) {
        cacheManager.delete(authKey);
        log.info("角色权限缓存已清空");
    }
}

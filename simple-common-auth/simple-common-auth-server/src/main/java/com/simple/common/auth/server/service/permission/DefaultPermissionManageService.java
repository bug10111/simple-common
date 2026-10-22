package com.simple.common.auth.server.service.permission;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayloadType;
import com.simple.common.auth.server.common.service.permission.PermissionManageService;
import com.simple.common.auth.server.exchange.sync.producer.SyncDeliveryProducer;
import com.simple.common.core.utils.AssertUtils;
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
    private SyncDeliveryProducer syncDeliveryProducer;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 权限缓存 TTL 缺省值（秒）：配置项被显式置空时的兜底（与客户端侧 DefaultSyncPayloadApplier 口径一致），避免 Integer 装箱拆箱 NPE
     */
    private static final int DEFAULT_PERMISSION_CACHE_EXPIRE_SECONDS = 60 * 60 * 24;

    /**
     * 新增权限（按项目维度）
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     * @param permissions 要添加的权限配置
     */
    @Override
    public void addPermissions(String projectCode, String tenantId, String roleKey, Map<String, String> permissions) {
        addPermissions(projectCode, tenantId, roleKey, permissions, true);
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
    public void addPermissions(String projectCode, String tenantId, String roleKey, Map<String, String> permissions, boolean publishEvent) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(tenantId, "租户ID不能为空（权限缓存键的租户段，管理端必须显式传入；无租户归属的历史角色不经过本管理入口）");
        AssertUtils.notNull(permissions, "权限配置不能为空");
        AssertUtils.isTrue(!permissions.isEmpty(), "权限配置不能为空Map");

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);

        // 1. 增量添加权限（不删除现有权限）
        cacheManager.hashPutAll(authKey, new HashMap<>(permissions));

        // 2. 设置过期时间（从配置文件读取，默认24小时）
        Integer expireSeconds = authProperties.getPermissionCacheExpire();
        cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : DEFAULT_PERMISSION_CACHE_EXPIRE_SECONDS);

        // 3. 根据参数决定是否发布事件
        if (publishEvent) {
            publishPermissionChange(projectCode, tenantId, roleKey, permissions, PermissionChangeTypeEnum.ADD);
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
    public void updateRolePermission(String projectCode, String tenantId, String roleKey, Map<String, String> permissions) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(tenantId, "租户ID不能为空（权限缓存键的租户段，管理端必须显式传入；无租户归属的历史角色不经过本管理入口）");
        AssertUtils.notNull(permissions, "权限配置不能为空");

        // 空权限等价于清空语义：显式转 clearPermissions（删键并发布 CLEAR 事件）。
        // 若沿用“先删后写”，空 Map 时 hashPutAll 为 no-op，键被删除后不再重建，expire 落在已删除键上，
        // 角色权限被静默清空且永不恢复
        if (permissions.isEmpty()) {
            clearPermissions(projectCode, tenantId, roleKey);
            return;
        }
        String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);

        // 1. 先删除所有旧权限
        cacheManager.delete(authKey);

        // 2. 再设置新权限
        cacheManager.hashPutAll(authKey, new HashMap<>(permissions));

        // 3. 设置过期时间（从配置文件读取，默认24小时）
        Integer expireSeconds = authProperties.getPermissionCacheExpire();
        cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : DEFAULT_PERMISSION_CACHE_EXPIRE_SECONDS);

        // 4. 发布权限变更事件
        publishPermissionChange(projectCode, tenantId, roleKey, permissions, PermissionChangeTypeEnum.UPDATE);

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
    public void deletePermission(String projectCode, String tenantId, String roleKey, String permissionKey) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(tenantId, "租户ID不能为空（权限缓存键的租户段，管理端必须显式传入；无租户归属的历史角色不经过本管理入口）");
        AssertUtils.notEmpty(permissionKey, "权限标识不能为空");

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);
        cacheManager.hashDelete(authKey, permissionKey);

        // 发布事件（只发送被删除的权限标识）
        Map<String, String> deletedPerm = new HashMap<>();
        deletedPerm.put(permissionKey, "");
        publishPermissionChange(projectCode, tenantId, roleKey, deletedPerm, PermissionChangeTypeEnum.DELETE);

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
    public void deletePermissions(String projectCode, String tenantId, String roleKey, List<String> permissionKeys) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");
        AssertUtils.notEmpty(tenantId, "租户ID不能为空（权限缓存键的租户段，管理端必须显式传入；无租户归属的历史角色不经过本管理入口）");
        AssertUtils.notEmpty(permissionKeys, "权限标识列表不能为空");

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);

        // 过滤非法权限标识（null/空白）：HDEL 传入 null/空字段会使整批删除失败，而 DELETE 事件仍会发布，造成客户端与服务端状态错位
        List<String> validKeys = permissionKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .toList();
        if (validKeys.isEmpty()) {
            log.warn("项目 [{}] 角色 [{}] 待删除的权限标识均为空白，忽略本次处理", projectCode, roleKey);
            return;
        }
        cacheManager.hashDelete(authKey, validKeys.toArray(new String[0]));

        // 发布事件（只发送被删除的权限列表）
        Map<String, String> deletedPerms = new HashMap<>();
        for (String key : validKeys) {
            deletedPerms.put(key, "");
        }
        publishPermissionChange(projectCode, tenantId, roleKey, deletedPerms, PermissionChangeTypeEnum.DELETE);

        log.info("项目 [{}] 角色 [{}] 批量删除了 {} 个权限，事件已发布", projectCode, roleKey, validKeys.size());
    }

    /**
     * 清空角色所有权限（按项目维度）
     *
     * @param projectCode 项目编码
     * @param roleKey     角色标识
     */
    @Override
    public void clearPermissions(String projectCode, String tenantId, String roleKey) {
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        AssertUtils.notEmpty(roleKey, "角色标识不能为空");

        String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);
        cacheManager.delete(authKey);

        // 发布空权限事件
        publishPermissionChange(projectCode, tenantId, roleKey, new HashMap<>(), PermissionChangeTypeEnum.CLEAR);

        log.info("项目 [{}] 角色 [{}] 的所有权限已清空，事件已发布", projectCode, roleKey);
    }
    /**
     * 发布权限变更投递（经投递引擎定向回调目标客户端，落地角色权限缓存）
     *
     * @param projectCode 项目编码（即目标客户端编码）
     * @param roleKey     角色标识
     * @param permissions 权限数据
     * @param changeType  变更类型
     */
    private void publishPermissionChange(String projectCode, String tenantId, String roleKey, Map<String, String> permissions, PermissionChangeTypeEnum changeType) {
        SyncPayload payload = new SyncPayload();
        payload.setPayloadType(SyncPayloadType.PERMISSION);
        payload.setRoleKey(roleKey);
        payload.setTenantId(tenantId);
        payload.setPermissions(permissions);
        payload.setChangeType(changeType);
        payload.setTimestamp(System.currentTimeMillis());
        try {
            syncDeliveryProducer.dispatchToClient(payload, projectCode);
        } catch (Exception e) {
            // 本地缓存已变更、投递入队失败：仅记告警留痕（客户端在 TTL 过期或下次变更时收敛）；
            // 不把投递异常抛回调用方——本地权威数据已落定，抛出只会让调用方误判整体失败
            log.error("权限变更投递入队失败: projectCode={} roleKey={} changeType={}", projectCode, roleKey, changeType, e);
        }
    }
}

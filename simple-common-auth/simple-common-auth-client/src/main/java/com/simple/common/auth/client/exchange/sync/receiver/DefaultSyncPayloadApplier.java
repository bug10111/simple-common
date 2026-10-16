package com.simple.common.auth.client.exchange.sync.receiver;

import com.simple.common.auth.client.common.constant.TokenConstant;import com.simple.common.auth.client.common.enums.PermissionChangeTypeEnum;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.sign.SignManager;
import com.simple.common.auth.client.common.manager.token.TokenManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayloadType;
import com.simple.common.auth.client.util.JwtUtils;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 同步载荷落地器默认实现
 * <p>
 * SECRET → TokenManager/SignManager 密钥缓存（JWT 双工具缓存与旧密钥事件链路同批落定）；
 * PERMISSION → 角色权限缓存（增量/全量/删除/清空，与权限事件链路同策略）；
 * LOGOUT → 清除该用户本地令牌缓存（jti 明细 + 用户令牌集合）。
 * 载荷项目归属与本实例不符时跳过并照常 ACK（实例配置漂移不制造重投风暴）。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class DefaultSyncPayloadApplier implements SyncPayloadApplier {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private SignManager signManager;

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private AuthProperties authProperties;

    @Override
    public void apply(SyncPayload payload) {
        // 载荷本体校验
        AssertUtils.notNull(payload, "同步载荷不能为空");
        if (payload.getPayloadType() == null) {
            log.warn("同步载荷缺少类型，忽略处理");
            return;
        }

        // 项目归属校验（载荷项目与本实例配置不符=实例配置漂移，跳过并照常 ACK）
        String currentProjectCode = authProperties.getProjectCode();
        if (!currentProjectCode.equals(payload.getProjectCode())) {
            log.warn("同步载荷项目[{}]与本实例项目[{}]不符，跳过处理", payload.getProjectCode(), currentProjectCode);
            return;
        }

        // 按类型分流落地
        switch (payload.getPayloadType()) {
            case SECRET -> applySecret(payload);
            case PERMISSION -> applyPermission(payload);
            case LOGOUT -> applyLogout(payload);
            default -> log.warn("未知的同步载荷类型: {}，忽略处理", payload.getPayloadType());
        }
    }

    /**
     * 落地密钥载荷（JWT 双工具缓存 + 签名缓存，与旧密钥事件链路同批落定）
     *
     * @param payload 密钥同步载荷
     */
    private void applySecret(SyncPayload payload) {
        Map<String, String> secrets = payload.getSecrets();
        if (secrets == null || secrets.isEmpty()) {
            log.warn("密钥同步载荷缺少密钥数据，忽略处理: project={}", payload.getProjectCode());
            return;
        }

        String jwtSecret = secrets.get("jwt");
        String signSecret = secrets.get("sign");

        // JWT 密钥落地（JWT 双工具缓存同批更新，签名验签与令牌验签共用）
        if (jwtSecret != null && !jwtSecret.isEmpty()) {
            tokenManager.addSecret(jwtSecret);
            JwtUtils.saveSecret(jwtSecret);
            log.info("项目 [{}] 的 JWT 密钥已同步更新", payload.getProjectCode());
        }

        // 签名密钥落地
        if (signSecret != null && !signSecret.isEmpty()) {
            signManager.addSecret(signSecret);
            log.info("项目 [{}] 的签名密钥已同步更新", payload.getProjectCode());
        }
    }

    /**
     * 落地权限载荷（按变更类型执行增量/全量/删除/清空缓存策略）
     *
     * @param payload 权限同步载荷
     */
    private void applyPermission(SyncPayload payload) {
        String roleKey = payload.getRoleKey();
        if (roleKey == null || roleKey.isEmpty()) {
            log.warn("权限同步载荷角色标识为空，忽略处理");
            return;
        }

        PermissionChangeTypeEnum changeType = payload.getChangeType();
        if (changeType == null) {
            log.warn("项目 [{}] 角色 [{}] 的权限变更类型为空，忽略处理", payload.getProjectCode(), roleKey);
            return;
        }

        Map<String, String> permissions = payload.getPermissions();
        String authKey = TokenConstant.getAuthKey(roleKey, payload.getProjectCode(), payload.getTenantId());

        // 按变更类型分流落地
        switch (changeType) {
            case ADD -> handleAddPermission(authKey, permissions);
            case UPDATE, REFRESH -> handleUpdatePermission(authKey, permissions);
            case DELETE -> handleDeletePermission(authKey, permissions);
            case CLEAR -> handleClearPermission(authKey);
            default -> log.warn("未知的权限变更类型: {}，忽略处理", changeType);
        }
    }

    /**
     * 落地登出载荷（清除该用户本地令牌缓存：jti 明细 + 用户令牌集合）
     *
     * @param payload 登出同步载荷
     */
    private void applyLogout(SyncPayload payload) {
        String userId = payload.getUserId();
        if (userId == null || userId.isEmpty()) {
            log.warn("登出同步载荷用户ID为空，忽略处理");
            return;
        }

        // 清除用户所有 jti 对应的登录信息
        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        Set<String> members = cacheManager.setMembers(userTokenKey);
        if (members != null && !members.isEmpty()) {
            for (String jti : members) {
                cacheManager.delete(TokenConstant.getUserInfoKey(jti));
            }
        }

        // 清除用户 Token 关联 Set
        cacheManager.delete(userTokenKey);
        log.info("用户 [{}] 退出登录通知已落地，本地令牌缓存已清除", userId);
    }

    /**
     * 处理新增权限（增量添加）
     *
     * @param authKey     权限缓存键
     * @param permissions 权限数据
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
     *
     * @param authKey     权限缓存键
     * @param permissions 权限数据
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
     *
     * @param authKey     权限缓存键
     * @param permissions 待删除权限数据（key 参与删除）
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
     *
     * @param authKey 权限缓存键
     */
    private void handleClearPermission(String authKey) {
        cacheManager.delete(authKey);
        log.info("角色权限缓存已清空");
    }

}

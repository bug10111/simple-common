package com.simple.common.auth.server.manager;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.permission.PermissionAutoLoader;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 服务端登录用户操作管理器。
 *
 * @author qty
 */
@Slf4j
@Component(LoginInfoManager.server_manager_name)
public class ServerLoginUserOperationManager implements LoginUserOperationManager {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 权限自动加载器（可选）
     * 业务方实现后，当缓存中权限数据不存在时，自动从数据库加载
     */
    @Autowired(required = false)
    private PermissionAutoLoader permissionAutoLoader;

    /**
     * 保存用户登录信息（包括用户详情、权限关联等）。
     *
     * @param tokenData Token 数据对象
     * @param isLogin   是否为新登录（true）或刷新（false）
     */
    @Override
    public void saveUserInfo(TokenData tokenData, boolean isLogin) {
        // 入参防御：必填字段缺失时快速失败并给出明确原因，避免深入实现后 NPE
        AssertUtils.notNull(tokenData, "保存登录信息时 tokenData 不能为空");
        Map<Object, Object> saveInfoMap = tokenData.getSaveInfoMap();
        Map<String, Object> refreshTokenMap = tokenData.getRefreshTokenMap();
        Object userIdObj = saveInfoMap == null ? null : saveInfoMap.get(TokenConstant.userIdKey);
        Object retObj = saveInfoMap == null ? null : saveInfoMap.get(TokenConstant.rEtKey);
        Object jtiObj = refreshTokenMap == null ? null : refreshTokenMap.get(TokenConstant.jtiKey);
        AssertUtils.notNull(userIdObj, "保存登录信息缺少用户ID");
        AssertUtils.notNull(retObj, "保存登录信息缺少刷新令牌有效期(ret)");
        AssertUtils.notNull(jtiObj, "保存登录信息缺少jti");
        String userId = userIdObj.toString();
        long timeOut;
        try {
            timeOut = Long.parseLong(retObj.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("刷新令牌有效期(ret)数据非法: " + retObj, e);
        }
        String jti = jtiObj.toString();

        String infoKey = TokenConstant.getUserInfoKey(jti);
        cacheManager.hashPutAll(infoKey, tokenData.getSaveInfoMap());
        cacheManager.expire(infoKey, timeOut, TimeUnit.SECONDS);

        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        cacheManager.setAdd(userTokenKey, jti);
        cacheManager.expire(userTokenKey, timeOut, TimeUnit.SECONDS);
    }

    /**
     * 用户完全登出，清除该用户所有登录状态。
     *
     * @param userId 用户ID
     */
    @Override
    public void loginOut(String userId) {
        Set<String> members = getUserToken(userId);
        if (members != null && !members.isEmpty()) {
            members.forEach(jti -> cacheManager.delete(TokenConstant.getUserInfoKey(jti)));
        }
        cacheManager.delete(TokenConstant.getUserTokenKey(userId));
    }

    /**
     * 用户部分登出，仅清除指定的 token 关联（不影响同账号其他设备）。
     *
     * @param userId 用户ID
     * @param jti    要移除的 token 唯一标识
     */
    /**
     * 用户部分登出，仅清除指定的 token 关联（不影响同账号其他设备）。
     *
     * @param userId 用户ID
     * @param jti    要移除的 token 唯一标识
     */
    @Override
    public void loginOut(String userId, String jti) {
        // 归属校验：仅当 jti 确实归属该用户的 token 集合时才清除其内省数据，
        // 防止以任意 userId+jti 组合使他人会话失效
        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        if (Boolean.TRUE.equals(cacheManager.setIsMember(userTokenKey, jti))) {
            cacheManager.delete(TokenConstant.getUserInfoKey(jti));
            cacheManager.setRemove(userTokenKey, jti);
        }
    }

    /**
     * 原子消费指定 jti（刷新令牌轮换的单赢家 CAS）。
     * <p>
     * 先从用户 token 集合原子移除旧 jti（SREM 原子语义，仅首个请求移除成功），
     * 移除成功才拥有本次轮换权并删除其内省数据；未移除到任何元素即已失效或被并发消费。
     * </p>
     *
     * @param userId 用户ID
     * @param jti    待消费的 jti
     * @return true 消费成功（本次轮换权归属当前请求）；false 已失效或已被并发消费
     */
    @Override
    public boolean consumeToken(String userId, String jti) {
        Long removed = cacheManager.setRemove(TokenConstant.getUserTokenKey(userId), jti);
        if (removed == null || removed == 0) {
            return false;
        }
        cacheManager.delete(TokenConstant.getUserInfoKey(jti));
        return true;
    }

    /**
     * 获取用户内省信息。
     *
     * @param key jti（登录唯一标识）
     * @return 用户信息 Map
     */
    @Override
    public Map<Object, Object> getUserInfo(String key) {
        return cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
    }

    /**
     * 获取用户权限信息（角色 -> 权限 Map）。
     *
     * @param loginRole 用户角色集合
     * @return 角色 -> 权限 Map 的映射
     */
    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(Set<String> loginRole) {
        if (loginRole == null || loginRole.isEmpty()) {
            return Collections.emptyMap();
        }
        
        String projectCode = LoginUserUtils.getUserTemporary().getClientId();
        AssertUtils.notEmpty(projectCode, "登录上下文缺少客户端编码（token 未携带 aud 或未经认证拦截器构建上下文），无法定位权限缓存");
        
        return getAuthoritiesByProjectCode(loginRole, projectCode);
    }

    /**
     * 获取用户权限信息（支持项目维度）。
     *
     * @param loginRole   用户角色集合
     * @param projectCode 项目编码（client_id）
     * @return 角色 -> 权限 Map 的映射
     */
    @Override
    public Map<Object, Map<Object, Object>> getAuthoritiesByProjectCode(Set<String> loginRole, String projectCode) {
        if (loginRole == null || loginRole.isEmpty()) {
            return Collections.emptyMap();
        }
        
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");

        Map<Object, Map<Object, Object>> result = new HashMap<>();
        // 整次查询固定同一租户上下文，避免读写缓存键的租户段漂移
        String tenantId = currentTenantId();
        for (String role : loginRole) {
            Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode, tenantId));
            if (perms != null && !perms.isEmpty()) {
                result.put(role, perms);
            } else if (permissionAutoLoader != null) {
                // 缓存中没有权限数据，通过 SPI 从数据库自动加载
                autoLoadAndCachePermissions(role, projectCode, tenantId);
                // 重新查询缓存
                perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode, tenantId));
                if (perms != null && !perms.isEmpty()) {
                    result.put(role, perms);
                }
            }
        }
        return result;
    }

    /**
     * 当前请求的租户上下文（权限缓存键的租户维度来源；无租户归属返回 null 走旧键格式）。
     *
     * @return 租户 ID（可能为 null）
     */
    private String currentTenantId() {
        var dataPermission = LoginUserUtils.getUserTemporary().getDataPermission();
        return dataPermission == null ? null : dataPermission.getTenantId();
    }

    /**
     * 自动从数据库加载并缓存权限数据。
     * 当缓存过期或 Redis 重启导致权限数据丢失时，通过 PermissionAutoLoader 回源加载。
     *
     * @param roleKey     角色标识
     * @param projectCode 项目编码
     */
    private void autoLoadAndCachePermissions(String roleKey, String projectCode, String tenantId) {
        try {
            Map<String, String> perms = permissionAutoLoader.loadPermissions(roleKey, projectCode, tenantId);
            if (perms != null && !perms.isEmpty()) {
                String authKey = TokenConstant.getAuthKey(roleKey, projectCode, tenantId);
                cacheManager.hashPutAll(authKey, new HashMap<>(perms));
                // expire 与 hashPutAll 非原子：expire 失败会留下永不过期的权限键（占内存且永不刷新），
                // 失败即删键回退，下次请求重新回源加载，杜绝孤儿权限数据
                try {
                    Integer expireSeconds = authProperties.getPermissionCacheExpire();
                    cacheManager.expire(authKey, expireSeconds != null ? expireSeconds : 60 * 60 * 24);
                } catch (Exception expireEx) {
                    cacheManager.delete(authKey);
                    log.error("权限缓存设置过期失败，已删键回退待重载: authKey={}", authKey, expireEx);
                }
                log.info("已自动加载并缓存角色 [{}] 在项目 [{}] 下的 {} 个权限", roleKey, projectCode, perms.size());
            }
        } catch (Exception e) {
            log.error("自动加载角色 [{}] 在项目 [{}] 下的权限失败", roleKey, projectCode, e);
        }
    }

    /**
     * 获取用户关联的所有 token（jti 集合）。
     *
     * @param userId 用户ID
     * @return token 集合
     */
    @Override
    public Set<String> getUserToken(String userId) {
        return cacheManager.setMembers(TokenConstant.getUserTokenKey(userId));
    }

    /**
     * 判断用户是否拥有指定权限。
     *
     * @param loginRole 用户角色
     * @param authority 权限标识数组
     * @return 是否有权限
     */
    @Override
    public Boolean hasAuth(Set<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) return true;
        if (loginRole == null || loginRole.isEmpty()) return false;

        String projectCode = authProperties.getProjectCode();
        AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");

        // 直接通过 CacheManager 查询权限（由配置决定使用 Redis 或 Local）；
        // 整次校验固定同一租户上下文，避免读写缓存键的租户段漂移
        String tenantId = currentTenantId();
        for (String role : loginRole) {
            Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode, tenantId));
            if (perms == null || perms.isEmpty()) {
                // 缓存中没有数据，尝试从数据库自动加载
                if (permissionAutoLoader != null) {
                    autoLoadAndCachePermissions(role, projectCode, tenantId);
                    perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode, tenantId));
                }
            }
            if (perms != null) {
                for (String auth : authority) {
                    if (perms.containsKey(auth)) return true;
                }
            }
        }
        return false;
    }
}
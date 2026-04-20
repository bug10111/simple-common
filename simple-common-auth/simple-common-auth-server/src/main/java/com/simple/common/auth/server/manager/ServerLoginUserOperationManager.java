package com.simple.common.auth.server.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 服务端登录用户操作管理器。
 * <p>
 * 优化：
 * 1. 增加本地权限缓存（permissionCache），按 jti 缓存用户权限集合，减少 Redis 查询。
 * 2. 实现 getAuthorities 方法，从缓存或 Redis 加载角色权限。
 * 3. hasAuth 方法优先使用本地缓存，提升性能。
 *
 * @author Admin (优化版本)
 */
@Slf4j
@Component
public class ServerLoginUserOperationManager implements LoginUserOperationManager {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    /**
     * 本地权限缓存：Key = jti，Value = 权限标识 Set。
     * 使用 Caffeine，过期时间默认 30 分钟，与 Token 刷新时间对齐。
     */
    private final Cache<String, Set<String>> permissionCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    /**
     * 保存用户登录信息（包括用户详情、权限关联等）。
     *
     * @param tokenData Token 数据对象
     * @param isLogin   是否为新登录（true）或刷新（false）
     */
    @Override
    public void saveUserInfo(TokenData tokenData, boolean isLogin) {
        String userId = tokenData.getSaveInfoMap().get(TokenConstant.userIdKey).toString();
        long timeOut = Long.parseLong(tokenData.getSaveInfoMap().get(TokenConstant.rEtKey).toString());
        String jti = tokenData.getRefreshTokenMap().get(TokenConstant.jtiKey).toString();

        String infoKey = TokenConstant.getUserInfoKey(jti);
        cacheManager.hashPutAll(infoKey, tokenData.getSaveInfoMap());
        cacheManager.expire(infoKey, timeOut, TimeUnit.SECONDS);

        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        cacheManager.setAdd(userTokenKey, jti);
        cacheManager.expire(userTokenKey, timeOut, TimeUnit.SECONDS);

        // 仅在新登录时重建权限缓存，刷新时不重建（权限未变，避免重复 Redis 查询）
        if (isLogin) {
            rebuildPermissionCache(jti, tokenData.getSaveInfoMap());
        }
    }

    /**
     * 重建用户权限本地缓存。
     *
     * @param jti      登录唯一标识
     * @param userInfo 用户信息 Map
     */
    private void rebuildPermissionCache(String jti, Map<Object, Object> userInfo) {
        Object loginRoleObj = userInfo.get(TokenConstant.loginRole);
        if (loginRoleObj == null) return;

        Set<String> allPermissions = new HashSet<>();
        try {
            List<String> roles = JsonUtils.toList(loginRoleObj.toString(), String.class);
            for (String role : roles) {
                Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
                if (perms != null) {
                    allPermissions.addAll(perms.keySet().stream().map(Object::toString).collect(Collectors.toSet()));
                }
            }
            permissionCache.put(jti, allPermissions);
        } catch (Exception e) {
            log.error("重建权限缓存失败，jti={}", jti, e);
        }
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
            members.forEach(jti -> {
                cacheManager.delete(TokenConstant.getUserInfoKey(jti));
                permissionCache.invalidate(jti);
            });
        }
        cacheManager.delete(TokenConstant.getUserTokenKey(userId));
    }

    /**
     * 用户部分登出，仅清除指定的 token 关联（不影响同账号其他设备）。
     *
     * @param userId 用户ID
     * @param jti    要移除的 token 唯一标识
     */
    @Override
    public void loginOut(String userId, String jti) {
        cacheManager.delete(TokenConstant.getUserInfoKey(jti));
        cacheManager.setRemove(TokenConstant.getUserTokenKey(userId), jti);
        permissionCache.invalidate(jti);
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
     * 优先从本地缓存读取，未命中则从 Redis 加载并回填本地缓存。
     *
     * @param loginRole 用户角色集合
     * @return 角色 -> 权限 Map 的映射
     */
    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(HashSet<String> loginRole) {
        if (loginRole == null || loginRole.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Object, Map<Object, Object>> result = new HashMap<>();
        for (String role : loginRole) {
            Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
            if (perms != null && !perms.isEmpty()) {
                // 类型安全转换：确保 Map 的值都是 Map 类型
                Map<Object, Object> safePerms = new HashMap<>();
                perms.forEach((k, v) -> {
                    if (v instanceof Map) {
                        safePerms.put(k, (Map<?, ?>) v);
                    } else {
                        safePerms.put(k, v);
                    }
                });
                result.put(role, safePerms);
            }
        }
        return result;
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
     * 判断用户是否拥有指定权限（服务端实现，供客户端远程调用）。
     * 优化：优先从本地权限缓存读取，若未命中则降级查询 Redis。
     *
     * @param loginRole 用户角色
     * @param authority 权限标识数组
     * @return 是否有权限
     */
    @Override
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) return true;
        if (loginRole == null || loginRole.isEmpty()) return false;

        // 尝试从当前线程获取 jti，利用本地缓存加速
        try {
            String jti = LoginUserUtils.getUserTemporary().getJti();
            if (jti != null) {
                return hasAuthByJti(jti, authority);
            }
        } catch (Exception e) {
            // 忽略，降级到普通查询
        }

        // 降级：直接查询 Redis
        for (String role : loginRole) {
            Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
            if (perms != null) {
                for (String auth : authority) {
                    if (perms.containsKey(auth)) return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据 jti 判断用户是否拥有指定权限（优化后的方法，供内部使用）。
     *
     * @param jti       登录唯一标识
     * @param authority 权限标识数组
     * @return 是否有权限
     */
    public Boolean hasAuthByJti(String jti, String[] authority) {
        if (authority == null || authority.length == 0) return true;
        Set<String> permissions = permissionCache.getIfPresent(jti);
        if (permissions != null) {
            for (String auth : authority) {
                if (permissions.contains(auth)) return true;
            }
            return false;
        }

        // 缓存未命中，降级查询
        Map<Object, Object> userInfo = getUserInfo(jti);
        if (userInfo.isEmpty()) return false;
        Object loginRoleObj = userInfo.get(TokenConstant.loginRole);
        if (loginRoleObj == null) return false;

        try {
            List<String> roles = JsonUtils.toList(loginRoleObj.toString(), String.class);
            for (String role : roles) {
                Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
                if (perms != null) {
                    for (String auth : authority) {
                        if (perms.containsKey(auth)) {
                            // 回填缓存
                            Set<String> allPerms = perms.keySet().stream().map(Object::toString).collect(Collectors.toSet());
                            permissionCache.put(jti, allPerms);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("权限校验异常", e);
        }
        return false;
    }
}
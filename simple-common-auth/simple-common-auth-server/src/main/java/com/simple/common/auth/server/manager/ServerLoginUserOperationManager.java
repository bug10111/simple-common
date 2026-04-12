package com.simple.common.auth.server.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 服务端登录用户操作管理器。
 * <p>
 * 优化：
 * 1. 增加本地权限缓存（permissionCache），减少 Redis 查询。
 * 2. 实现 getAuthorities 方法，从缓存或 Redis 加载角色权限。
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
     * 本地权限缓存：Key = 角色名，Value = 权限标识 Set。
     * 使用 Caffeine，过期时间默认 30 分钟，与 Token 刷新时间对齐。
     */
    private final Cache<String, Map<Object, Object>> permissionCache = Caffeine.newBuilder()
                                                                               .maximumSize(5000)
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
        String userId = tokenData.getSaveInfoMap().get(TokenConstant.userIdKey);
        long timeOut = Long.parseLong(tokenData.getSaveInfoMap().get(TokenConstant.rEtKey));
        String jti = tokenData.getRefreshTokenMap().get(TokenConstant.jtiKey).toString();

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
    @Override
    public void loginOut(String userId, String jti) {
        cacheManager.delete(TokenConstant.getUserInfoKey(jti));
        cacheManager.setRemove(TokenConstant.getUserTokenKey(userId), jti);
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
            Map<Object, Object> perms = permissionCache.getIfPresent(role);
            if (perms == null) {
                perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
                if (perms != null && !perms.isEmpty()) {
                    permissionCache.put(role, perms);
                }
            }
            if (perms != null && !perms.isEmpty()) {
                result.put(role, perms);
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
     * 判断用户是否拥有指定权限（服务端实现中未使用，预留）。
     *
     * @param loginRole 用户角色
     * @param authority 权限标识数组
     * @return 是否有权限
     */
    @Override
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) return true;
        if (loginRole == null || loginRole.isEmpty()) return false;
        for (String role : loginRole) {
            Map<Object, Object> perms = permissionCache.getIfPresent(role);
            if (perms == null) {
                perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
                if (perms != null) permissionCache.put(role, perms);
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
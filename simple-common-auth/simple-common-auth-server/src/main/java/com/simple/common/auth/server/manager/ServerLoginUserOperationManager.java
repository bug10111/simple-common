package com.simple.common.auth.server.manager;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.core.utils.AssertUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 服务端登录用户操作管理器。
 *
 * @author qty
 */
@Slf4j
@Component
public class ServerLoginUserOperationManager implements LoginUserOperationManager {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private AuthProperties authProperties;

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
     *
     * @param loginRole 用户角色集合
     * @return 角色 -> 权限 Map 的映射
     */
    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(HashSet<String> loginRole) {
        if (loginRole == null || loginRole.isEmpty()) {
            return Collections.emptyMap();
        }
        
        String projectCode = authProperties.getProjectCode();
        AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");
        
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
    public Map<Object, Map<Object, Object>> getAuthoritiesByProjectCode(HashSet<String> loginRole, String projectCode) {
        if (loginRole == null || loginRole.isEmpty()) {
            return Collections.emptyMap();
        }
        
        AssertUtils.notEmpty(projectCode, "项目编码不能为空");
        
        Map<Object, Map<Object, Object>> result = new HashMap<>();
        for (String role : loginRole) {
            Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode));
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
     * 判断用户是否拥有指定权限。
     *
     * @param loginRole 用户角色
     * @param authority 权限标识数组
     * @return 是否有权限
     */
    @Override
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) return true;
        if (loginRole == null || loginRole.isEmpty()) return false;

        String projectCode = authProperties.getProjectCode();
        AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");

        // 直接通过 CacheManager 查询权限（由配置决定使用 Redis 或 Local）
        for (String role : loginRole) {
            Map<Object, Object> perms = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode));
            if (perms != null) {
                for (String auth : authority) {
                    if (perms.containsKey(auth)) return true;
                }
            }
        }
        return false;
    }
}
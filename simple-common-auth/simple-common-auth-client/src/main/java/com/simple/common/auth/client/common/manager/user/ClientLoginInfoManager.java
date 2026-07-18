package com.simple.common.auth.client.common.manager.user;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.manager.permission.PermissionAutoLoader;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.exchange.AuthCenterHttpClient;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 客户端登录信息管理器。
 *
 * @author qty
 */
@Slf4j
@Component(value = LoginInfoManager.client_manager_name)
public class ClientLoginInfoManager implements LoginInfoManager, CoreLoginUserService {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private LockService lockService;

    @Autowired
    private AuthCenterHttpClient authCenterHttpClient;

    @Autowired
    private AuthProperties authProperties;

    /**
     * 权限自动加载器（可选）
     * 业务方实现后，当 auth-server 返回的权限数据不存在时，从本地数据库加载
     */
    @Autowired(required = false)
    private PermissionAutoLoader permissionAutoLoader;

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        String userInfoKey = TokenConstant.getUserInfoKey(key);

        // 一级缓存命中直接返回，避免加锁开销
        Map<Object, Object> cached = cacheManager.hashGetAll(userInfoKey);
        if (!cached.isEmpty()) {
            return cached;
        }

        // 未命中时加锁从 auth-server 拉取，防止并发重复请求
        return lockAndFetch(userInfoKey, key);
    }

    /**
     * 加锁后二次检查缓存，若仍为空则从 auth-server 拉取用户信息并缓存。
     *
     * @param userInfoKey 用户信息缓存 key
     * @param jti         JWT Token 唯一标识
     * @return 用户信息 Map
     */
    private Map<Object, Object> lockAndFetch(String userInfoKey, String jti) {
        ReturnValueFunction lockFunction = () -> {
            // 二次检查缓存，可能被其他线程已填充
            Map<Object, Object> userInfo = cacheManager.hashGetAll(userInfoKey);
            if (!userInfo.isEmpty()) {
                return userInfo;
            }

            // 调用 auth-server 获取用户信息并缓存
            return fetchAndCacheFromAuthServer(jti, userInfoKey);
        };

        return (Map<Object, Object>) lockService.lockHaveValue(jti, lockFunction);
    }

    /**
     * 从 auth-server 拉取用户信息、token 关联、权限数据，写入本地缓存。
     * <p>
     * 一次 HTTP 调用完成三类数据同步，减少网络往返。
     * </p>
     *
     * @param jti         JWT Token 唯一标识
     * @param userInfoKey 用户信息缓存 key
     * @return 用户信息 Map
     */
    private Map<Object, Object> fetchAndCacheFromAuthServer(String jti, String userInfoKey) {
        // 调用 auth-server 内省接口
        HttpResponse httpResponse = authCenterHttpClient.getUserInfo();
        String body = httpResponse.body();
        AssertUtils.notEmpty(body, LoginException.LOGIN_EXPIRED);

        // 解析响应
        R<?> r = JsonUtils.toJsonObj(body, R.class);
        AssertUtils.isTrue(httpResponse.getStatus() == DefaultExceptionEnum.OK.getCodeInt(),
                LoginException.LOGIN_EXPIRED, r.getMessage());

        Object dataObj = r.getData();
        AssertUtils.notEmpty(dataObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 data 为空");
        Map<Object, Object> responseMap = JsonUtils.toJsonObj(dataObj.toString(), Map.class);

        // 解析 userInfo
        Object userInfoObj = responseMap.get(TokenConstant.userInfoName);
        AssertUtils.notEmpty(userInfoObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userInfo 为空");
        Map<Object, Object> userInfo = JsonUtils.toJsonObj(userInfoObj.toString(), Map.class);
        String userId = (String) userInfo.get(TokenConstant.userIdKey);

        // 写入用户信息缓存
        cacheManager.hashPutAll(userInfoKey, userInfo);

        // 解析 userToken 关联并缓存
        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        cacheUserTokens(responseMap, userTokenKey);

        // 统一设置过期时间
        long expireSeconds = parseRetExpire(userInfo);
        cacheManager.expire(userInfoKey, expireSeconds);
        cacheManager.expire(userTokenKey, expireSeconds);

        // 加载权限数据
        cacheAuthorities(responseMap, userInfo, expireSeconds);

        return userInfo;
    }

    /**
     * 从 auth-server 响应中解析 userToken 列表并写入缓存。
     *
     * @param responseMap  auth-server 响应数据
     * @param userTokenKey 用户 token 关联缓存 key
     */
    private void cacheUserTokens(Map<Object, Object> responseMap, String userTokenKey) {
        Object userTokenObj = responseMap.get(TokenConstant.userTokenName);
        AssertUtils.notEmpty(userTokenObj, LoginException.LOGIN_EXPIRED, "auth-server 返回的 userToken 为空");
        List<String> tokenList = JsonUtils.toList(userTokenObj.toString(), String.class);
        cacheManager.setAdd(userTokenKey, tokenList.toArray(new String[0]));
    }

    /**
     * 加载权限数据：优先使用 auth-server 返回的权限，
     * 若为空则通过 PermissionAutoLoader 从本地数据库加载。
     *
     * @param responseMap   auth-server 响应数据
     * @param userInfo      用户信息 Map
     * @param expireSeconds 缓存过期时间（秒）
     */
    private void cacheAuthorities(Map<Object, Object> responseMap, Map<Object, Object> userInfo, long expireSeconds) {
        String projectCode = authProperties.getProjectCode();
        AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");

        // 优先使用 auth-server 返回的权限数据
        boolean hasAuthFromServer = loadAuthFromServerResponse(responseMap, projectCode, expireSeconds);

        // auth-server 无权限数据时，从本地数据库加载
        if (!hasAuthFromServer && permissionAutoLoader != null) {
            loadAuthFromLocal(userInfo, projectCode, expireSeconds);
        }
    }

    /**
     * 从 auth-server 响应中解析并缓存权限数据。
     *
     * @param responseMap   auth-server 响应数据
     * @param projectCode   项目编码
     * @param expireSeconds 缓存过期时间
     * @return true 表示成功加载了权限，false 表示无权限数据
     */
    private boolean loadAuthFromServerResponse(Map<Object, Object> responseMap, String projectCode, long expireSeconds) {
        if (!responseMap.containsKey(TokenConstant.userAuthName)) {
            return false;
        }
        Object userAuthObj = responseMap.get(TokenConstant.userAuthName);
        if (userAuthObj == null) {
            return false;
        }
        Map<Object, Object> authMap = JsonUtils.toJsonObj(userAuthObj.toString(), Map.class);
        if (authMap.isEmpty()) {
            return false;
        }

        // 按角色维度缓存权限
        for (Map.Entry<Object, Object> entry : authMap.entrySet()) {
            String roleKey = entry.getKey().toString();
            Map<Object, Object> auth = JsonUtils.toJsonObj(entry.getValue().toString(), Map.class);
            String authKey = TokenConstant.getAuthKey(roleKey, projectCode);
            cacheManager.hashPutAll(authKey, auth);
            cacheManager.expire(authKey, expireSeconds);
        }

        log.info("已从 auth-Server 加载并缓存 [{}] 下 {} 个角色的权限", projectCode, authMap.size());
        return true;
    }

    /**
     * 通过 PermissionAutoLoader 从本地数据库加载权限。
     *
     * @param userInfo      用户信息 Map
     * @param projectCode   项目编码
     * @param expireSeconds 缓存过期时间
     */
    private void loadAuthFromLocal(Map<Object, Object> userInfo, String projectCode, long expireSeconds) {
        log.info("auth-Server 未返回权限数据，尝试通过 PermissionAutoLoader 从本地数据库加载 projectCode=[{}]", projectCode);
        try {
            Object loginRoleObj = userInfo.get(TokenConstant.loginRole);
            if (loginRoleObj == null) {
                return;
            }
            Set<String> loginRoles = parseLoginRoles(loginRoleObj);
            for (String roleKey : loginRoles) {
                Map<String, String> perms = permissionAutoLoader.loadPermissions(roleKey, projectCode);
                if (perms == null || perms.isEmpty()) {
                    continue;
                }
                String authKey = TokenConstant.getAuthKey(roleKey, projectCode);
                cacheManager.hashPutAll(authKey, new HashMap<>(perms));
                cacheManager.expire(authKey, expireSeconds);
                log.debug("已通过 PermissionAutoLoader 缓存角色 [{}] 在项目 [{}] 下的 {} 个权限",
                        roleKey, projectCode, perms.size());
            }
        } catch (Exception e) {
            log.error("通过 PermissionAutoLoader 加载权限失败", e);
        }
    }

    /**
     * 解析用户角色数据，兼容 Collection 和 JSON 字符串两种格式。
     *
     * @param loginRoleObj 角色数据对象
     * @return 角色标识集合
     */
    private Set<String> parseLoginRoles(Object loginRoleObj) {
        if (loginRoleObj instanceof Collection) {
            return new HashSet<>((Collection<String>) loginRoleObj);
        }
        List<String> roleList = JsonUtils.toList(loginRoleObj.toString(), String.class);
        return new HashSet<>(roleList);
    }

    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(Set<String> loginRole) {
        String projectCode = authProperties.getProjectCode();
        AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");
        
        Map<Object, Map<Object, Object>> map = new HashMap<>();
        if (loginRole != null) {
            for (String role : loginRole) {
                Map<Object, Object> entries = cacheManager.hashGetAll(TokenConstant.getAuthKey(role, projectCode));
                if (ObjUtil.isNotEmpty(entries)) {
                    map.put(role, entries);
                }
            }
        }
        return map;
    }

    @Override
    public Set<String> getUserToken(String userId) {
        return cacheManager.setMembers(TokenConstant.getUserTokenKey(userId));
    }

    @Override
    public String getUserId() {
        return LoginUserUtils.getUserTemporary().getUserId();
    }

    @Override
    public Boolean hasAuth(Set<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) {
            return true;
        }

        String projectCode = authProperties.getProjectCode();
        AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");

        // 直接通过 CacheManager 查询权限（由配置决定使用 Redis 或 Local）
        if (loginRole != null) {
            for (String role : loginRole) {
                for (String perm : authority) {
                    Boolean has = cacheManager.hashHasKey(TokenConstant.getAuthKey(role, projectCode), perm);
                    if (Boolean.TRUE.equals(has)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 从用户信息中解析缓存过期时间（秒）。
     * 若 auth-server 响应未携带 ret 字段，使用默认值 3600 秒。
     *
     * @param userInfo 用户信息 Map
     * @return 过期时间（秒）
     */
    private long parseRetExpire(Map<Object, Object> userInfo) {
        Object retObj = userInfo.get(TokenConstant.rEtKey);
        if (retObj == null) {
            return 3600L;
        }
        return Long.parseLong(retObj.toString());
    }
}
package com.simple.common.auth.client.common.manager.user;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.auth.client.common.properties.AuthProperties;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;
import com.simple.common.core.common.service.lock.LockService;
import com.simple.common.core.exception.DefaultExceptionEnum;
import com.simple.common.core.function.ReturnValueFunction;
import com.simple.common.core.response.R;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 客户端登录信息管理器。
 * <p>
 * 优化内容：
 * 1. 增加本地权限缓存（permissionCache），减少 CacheManager 调用。
 * 2. hasAuth 方法优先从本地 Set 判断，性能提升 O(1)。
 * 3. 保持与 Redis/Local CacheManager 的兼容。
 *
 * @author qty (优化版本)
 */
@Slf4j
@Component(value = LoginInfoManager.client_manager_name)
public class ClientLoginInfoManager implements LoginInfoManager, CoreLoginUserService {

    @Autowired
    @Qualifier("authCacheManager")
    private CacheManager cacheManager;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private LockService lockService;

    /**
     * 本地权限缓存：Key = jti（登录唯一标识），Value = 权限标识 Set。
     * 使用 Caffeine 实现，过期时间与用户信息缓存保持一致。
     */
    private final Cache<String, Set<String>> permissionCache;

    public ClientLoginInfoManager() {
        // 初始化本地权限缓存，最大 10000 条，写入后 30 分钟过期（默认）
        this.permissionCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 获取用户信息（包含权限数据），优先从缓存读取，缓存未命中则远程加载。
     *
     * @param key jti（登录唯一标识）
     * @return 用户信息 Map
     */
    @Override
    public Map<Object, Object> getUserInfo(String key) {
        // 先从 CacheManager 读取用户信息（可能来自 Redis 或本地 Caffeine）
        Map<Object, Object> entries = cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
        if (!entries.isEmpty()) {
            return entries;
        }

        // 分布式锁保护，防止缓存击穿
        ReturnValueFunction lockFunction = () -> {
            Map<Object, Object> userInfo = cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
            if (userInfo.isEmpty()) {
                // 远程调用授权中心获取用户数据
                HttpResponse execute = getRemoteHttpResponse();
                String body = execute.body();
                AssertUtils.notEmpty(body, LoginException.LOGIN_EXPIRED);

                R<?> jsonObj2 = JsonUtils.toJsonObj(body, R.class);
                AssertUtils.isTrue(execute.getStatus() == DefaultExceptionEnum.OK.getCodeInt(),
                        LoginException.LOGIN_EXPIRED, jsonObj2.getMessage());

                Map<Object, Object> response = JsonUtils.toJsonObj(jsonObj2.getData().toString(), Map.class);

                // 解析用户信息
                userInfo = JsonUtils.toJsonObj(response.get(TokenConstant.userInfoName).toString(), Map.class);
                String userId = (String) userInfo.get(TokenConstant.userIdKey);

                // 缓存用户信息到 CacheManager
                String userInfoKey = TokenConstant.getUserInfoKey(key);
                String userTokenKey = TokenConstant.getUserTokenKey(userId);
                cacheManager.hashPutAll(userInfoKey, userInfo);

                List<String> jsonObj = JsonUtils.toList(response.get(TokenConstant.userTokenName).toString(), String.class);
                cacheManager.setAdd(userTokenKey, jsonObj.toArray(new String[0]));

                // 设置过期时间（与服务端返回的剩余有效时间一致）
                long times = Long.parseLong(userInfo.get(TokenConstant.rEtKey).toString());
                cacheManager.expire(userInfoKey, times);
                cacheManager.expire(userTokenKey, times);

                // 解析并缓存权限数据
                Map<?, ?> authMap = JsonUtils.toJsonObj(response.get(TokenConstant.userAuthName).toString(), Map.class);
                Set<String> allPermissions = new HashSet<>();
                for (Object obj : authMap.keySet()) {
                    String roleKey = obj.toString();
                    Map<Object, Object> auth = JsonUtils.toJsonObj(authMap.get(roleKey).toString(), Map.class);
                    // 存储角色权限到 CacheManager（保留兼容）
                    cacheManager.hashPutAll(roleKey, auth);
                    cacheManager.expire(roleKey, times);
                    // 收集所有权限标识到本地 Set
                    allPermissions.addAll(auth.keySet().stream().map(Object::toString).collect(Collectors.toSet()));
                }

                // 将权限 Set 存入本地缓存，过期时间与用户信息一致
                permissionCache.put(key, allPermissions);
            }
            return userInfo;
        };

        return (Map<Object, Object>) lockService.lockHaveValue(key, lockFunction);
    }

    /**
     * 远程请求，获取授权中心用户信息。
     *
     * @return HTTP 响应对象
     */
    protected HttpResponse getRemoteHttpResponse() {
        return HttpRequest.get(authProperties.getServerUrl() + "/api/user")
                .header(TokenConstant.Authorization, HttpServletUtils.getRequest().getHeader(TokenConstant.Authorization))
                .execute();
    }

    /**
     * 获取用户的所有权限数据（角色 -> 权限映射）。
     * <p>
     * 优化：直接从 CacheManager 读取，因为本地权限缓存仅存储 Set，此处保持原逻辑。
     *
     * @param loginRole 用户拥有的角色集合
     * @return 角色 -> 权限 Map 的映射
     */
    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(HashSet<String> loginRole) {
        Map<Object, Map<Object, Object>> map = new HashMap<>();
        if (loginRole != null) {
            for (String role : loginRole) {
                Map<Object, Object> entries = cacheManager.hashGetAll(TokenConstant.getAuthKey(role));
                if (ObjUtil.isNotEmpty(entries)) {
                    map.put(role, entries);
                }
            }
        }
        return map;
    }

    /**
     * 获取用户关联的所有 token（用于单点登录控制）。
     *
     * @param userId 用户ID
     * @return token 集合
     */
    @Override
    public Set<String> getUserToken(String userId) {
        return cacheManager.setMembers(TokenConstant.getUserTokenKey(userId));
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 用户ID
     */
    @Override
    public String getUserId() {
        return LoginUserUtils.getUserTemporary().getUserId();
    }

    /**
     * 判断当前用户是否拥有指定权限。
     * <p>
     * 优化：优先从本地权限缓存（permissionCache）中判断，避免频繁访问 CacheManager。
     *
     * @param loginRole 用户角色集合（当前未使用，权限已扁平化）
     * @param authority 待校验的权限标识数组
     * @return true 拥有任一权限，false 无权限
     */
    @Override
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) {
            return true;
        }

        // 获取当前用户的 jti（登录唯一标识）
        String jti = LoginUserUtils.getUserTemporary().getJti();
        if (jti == null) {
            log.warn("当前线程未绑定用户 jti，无法进行权限校验");
            return false;
        }

        // 优先从本地权限缓存获取
        Set<String> permissions = permissionCache.getIfPresent(jti);
        if (permissions != null) {
            // 只要拥有任意一个权限即返回 true
            for (String perm : authority) {
                if (permissions.contains(perm)) {
                    return true;
                }
            }
            return false;
        }

        // 本地缓存未命中，降级到 CacheManager 查询（通常发生在缓存刚过期时）
        log.debug("本地权限缓存未命中 jti={}，降级查询 CacheManager", jti);
        if (loginRole != null) {
            for (String role : loginRole) {
                for (String perm : authority) {
                    Boolean has = cacheManager.hashHasKey(TokenConstant.getAuthKey(role), perm);
                    if (Boolean.TRUE.equals(has)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
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
 * 2. hasAuth 方法优先从本地 Set 判断，性能 O(1)。
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
     * 本地权限缓存：Key = jti，Value = 权限标识 Set。
     */
    private final Cache<String, Set<String>> permissionCache;

    public ClientLoginInfoManager() {
        this.permissionCache = Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(30, TimeUnit.MINUTES).build();
    }

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        Map<Object, Object> entries = cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
        if (!entries.isEmpty()) {
            return entries;
        }

        ReturnValueFunction lockFunction = () -> {
            Map<Object, Object> userInfo = cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
            if (userInfo.isEmpty()) {
                HttpResponse execute = getRemoteHttpResponse();
                String body = execute.body();
                AssertUtils.notEmpty(body, LoginException.LOGIN_EXPIRED);

                R<?> jsonObj2 = JsonUtils.toJsonObj(body, R.class);
                AssertUtils.isTrue(execute.getStatus() == DefaultExceptionEnum.OK.getCodeInt(), LoginException.LOGIN_EXPIRED, jsonObj2.getMessage());

                Map<Object, Object> response = JsonUtils.toJsonObj(jsonObj2.getData().toString(), Map.class);

                userInfo = JsonUtils.toJsonObj(response.get(TokenConstant.userInfoName).toString(), Map.class);
                String userId = (String) userInfo.get(TokenConstant.userIdKey);

                String userInfoKey = TokenConstant.getUserInfoKey(key);
                String userTokenKey = TokenConstant.getUserTokenKey(userId);
                cacheManager.hashPutAll(userInfoKey, userInfo);

                List<String> jsonObj = JsonUtils.toList(response.get(TokenConstant.userTokenName).toString(), String.class);
                cacheManager.setAdd(userTokenKey, jsonObj.toArray(new String[0]));

                long times = Long.parseLong(userInfo.get(TokenConstant.rEtKey).toString());
                cacheManager.expire(userInfoKey, times);
                cacheManager.expire(userTokenKey, times);

                Map<?, ?> authMap = JsonUtils.toJsonObj(response.get(TokenConstant.userAuthName).toString(), Map.class);
                Set<String> allPermissions = new HashSet<>();
                for (Object obj : authMap.keySet()) {
                    String roleKey = obj.toString();
                    Map<Object, Object> auth = JsonUtils.toJsonObj(authMap.get(roleKey).toString(), Map.class);
                    cacheManager.hashPutAll(roleKey, auth);
                    cacheManager.expire(roleKey, times);
                    allPermissions.addAll(auth.keySet().stream().map(Object::toString).collect(Collectors.toSet()));
                }

                permissionCache.put(key, allPermissions);
            }
            return userInfo;
        };

        return (Map<Object, Object>) lockService.lockHaveValue(key, lockFunction);
    }

    protected HttpResponse getRemoteHttpResponse() {
        return HttpRequest.get(authProperties.getServerUrl() + "/api/user")
                          .header(TokenConstant.Authorization, HttpServletUtils.getRequest().getHeader(TokenConstant.Authorization))
                          .execute();
    }

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

    @Override
    public Set<String> getUserToken(String userId) {
        return cacheManager.setMembers(TokenConstant.getUserTokenKey(userId));
    }

    @Override
    public String getUserId() {
        return LoginUserUtils.getUserTemporary().getUserId();
    }

    @Override
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
        if (authority == null || authority.length == 0) {
            return true;
        }

        String jti = LoginUserUtils.getUserTemporary().getJti();
        if (jti == null) {
            log.warn("当前线程未绑定用户 jti，无法进行权限校验");
            return false;
        }

        // 优先从本地缓存获取
        Set<String> permissions = permissionCache.getIfPresent(jti);
        if (permissions != null) {
            for (String perm : authority) {
                if (permissions.contains(perm)) {
                    return true;
                }
            }
            return false;
        }

        // 降级查询 CacheManager
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
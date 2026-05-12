package com.simple.common.auth.client.common.manager.user;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
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

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        Map<Object, Object> entries = cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
        if (!entries.isEmpty()) {
            return entries;
        }

        ReturnValueFunction lockFunction = () -> {
            Map<Object, Object> userInfo = cacheManager.hashGetAll(TokenConstant.getUserInfoKey(key));
            if (userInfo.isEmpty()) {
                HttpResponse execute = authCenterHttpClient.getUserInfo();
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

                // 从 auth-Server 返回的数据中解析并缓存权限（按 projectCode 过滤）
                String projectCode = authProperties.getProjectCode();
                AssertUtils.notEmpty(projectCode, "项目编码未配置，请在 application.yml 中配置 simple.auth.project-code");
                
                if (response.containsKey(TokenConstant.userAuthName)) {
                    Object userAuthObj = response.get(TokenConstant.userAuthName);
                    if (userAuthObj != null) {
                        Map<Object, Object> authMap = JsonUtils.toJsonObj(userAuthObj.toString(), Map.class);
                        
                        // 缓存每个角色的权限
                        for (Object obj : authMap.keySet()) {
                            String roleKey = obj.toString();
                            Map<Object, Object> auth = JsonUtils.toJsonObj(authMap.get(roleKey).toString(), Map.class);
                            String authKey = TokenConstant.getAuthKey(roleKey, projectCode);
                            cacheManager.hashPutAll(authKey, auth);
                            cacheManager.expire(authKey, times);
                        }
                        
                        log.info("已从 auth-Server 加载并缓存 [{}] 下 {} 个角色的权限", projectCode, authMap.size());
                    }
                }
            }
            return userInfo;
        };

        return (Map<Object, Object>) lockService.lockHaveValue(key, lockFunction);
    }

    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(HashSet<String> loginRole) {
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
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
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
}
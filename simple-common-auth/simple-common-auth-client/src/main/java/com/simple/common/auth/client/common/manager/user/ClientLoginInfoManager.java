package com.simple.common.auth.client.common.manager.user;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.common.constant.LoginInfoConstant;
import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.enums.login.LoginException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component(value = LoginInfoManager.client_manager_name)
public class ClientLoginInfoManager implements LoginInfoManager, CoreLoginUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private LockService lockService;

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(TokenConstant.getUserInfoKey(key));
        if (!entries.isEmpty()) {
            return entries;
        }

        ReturnValueFunction lockFunction = () -> {
            Map<Object, Object> userInfo = redisTemplate.opsForHash().entries(TokenConstant.getUserInfoKey(key));
            if (userInfo.isEmpty()) {

                HttpResponse execute = getRemoteHttpResponse();

                //获取授权中心返回值
                String body = execute.body();
                AssertUtils.notEmpty(body, LoginException.LOGIN_EXPIRED);

                R<?> jsonObj2 = JsonUtils.toJsonObj(body, R.class);
                AssertUtils.isTrue(execute.getStatus() == DefaultExceptionEnum.OK.getCodeInt(), LoginException.LOGIN_EXPIRED, jsonObj2.getMessage());

                Map<Object, Object> response = JsonUtils.toJsonObj(jsonObj2.getData().toString(), Map.class);

                //获取用户信息
                userInfo = JsonUtils.toJsonObj(response.get(LoginInfoConstant.user_info_name).toString(), Map.class);
                String userId = (String) userInfo.get(TokenConstant.userIdKey);

                //缓存数据
                String userInfoKey = TokenConstant.getUserInfoKey(key);
                String userTokenKey = TokenConstant.getUserTokenKey(userId);

                redisTemplate.opsForHash().putAll(userInfoKey, userInfo);
                List<String> jsonObj = JsonUtils.toList(response.get(LoginInfoConstant.user_token_name).toString(), String.class);
                redisTemplate.opsForSet().add(userTokenKey, jsonObj.toArray(new String[0]));

                //设置缓存时间，这里过期时间不和服务端强一致
                long times = Long.parseLong(userInfo.get(TokenConstant.rEtKey).toString());
                redisTemplate.expire(userInfoKey, times, TimeUnit.SECONDS);
                redisTemplate.expire(userTokenKey, times, TimeUnit.SECONDS);

                //收集权限信息
                Map<?,?> authMap = JsonUtils.toJsonObj(response.get(LoginInfoConstant.user_auth_name).toString(), Map.class);
                for (Object obj : authMap.keySet()) {
                    String roleKey = obj.toString();
                    Map<?,?> auth = JsonUtils.toJsonObj(authMap.get(roleKey).toString(), Map.class);
                    redisTemplate.opsForHash().putAll(roleKey, auth);
                    redisTemplate.expire(roleKey, times, TimeUnit.SECONDS);
                }
            }
            return userInfo;
        };

        return (Map<Object, Object>) lockService.lockHaveValue(key, lockFunction);
    }

    /**
     * 远程请求，获取授权中心用户信息
     */
    protected HttpResponse getRemoteHttpResponse() {
        return HttpRequest.get(authProperties.getServerUrl() + "/api/user")
                          .header(TokenConstant.Authorization, HttpServletUtils.getRequest().getHeader(TokenConstant.Authorization))
                          .execute();
    }

    @Override
    public Map<Object, Map<Object, Object>> getAuthorities(HashSet<String> loginRole) {
        Map<Object, Map<Object, Object>> map = new HashMap<>();
        loginRole.forEach(s -> {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(TokenConstant.getAuthKey(s));
            if (ObjUtil.isNotEmpty(entries)) {
                map.put(s, entries);
            }
        });
        return map;
    }

    @Override
    public Set<String> getUserToken(String userId) {
        return redisTemplate.opsForSet().members(TokenConstant.getUserTokenKey(userId));
    }

    @Override
    public String getUserId() {
        return LoginUserUtils.getUserTemporary().getUserId();
    }

    @Override
    public Boolean hasAuth(HashSet<String> loginRole, String[] authority) {
        for (String ignored : authority) {
            if(ObjUtil.isNotEmpty(loginRole)) {
                for (String s : loginRole) {
                    Boolean b = redisTemplate.opsForHash().hasKey(TokenConstant.getAuthKey(s), ignored);
                    if (b) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

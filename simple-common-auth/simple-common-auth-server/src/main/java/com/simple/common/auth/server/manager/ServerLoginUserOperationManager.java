package com.simple.common.auth.server.manager;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.manager.user.ClientLoginInfoManager;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.cache.CacheManager;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 服务端登录用户操作管理器
 *
 * @author Admin
 */
@Component
public class ServerLoginUserOperationManager implements LoginUserOperationManager {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void saveUserInfo(TokenData tokenData, boolean isLogin) {

        //用户id
        String userId = tokenData.getSaveInfoMap().get(TokenConstant.userIdKey);

        //过期时间
        long timeOut = Long.parseLong(tokenData.getSaveInfoMap().get(TokenConstant.rEtKey));

        //jti
        String jti = tokenData.getRefreshTokenMap().get(TokenConstant.jtiKey).toString();

        //保存用户信息，过期时间为refresh过期时间
        String infoKey = TokenConstant.getUserInfoKey(jti);
        cacheManager.opsForHash().putAll(infoKey, tokenData.getSaveInfoMap());
        cacheManager.expire(infoKey, timeOut, TimeUnit.SECONDS);

        //保存用户token关联信息
        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        cacheManager.opsForSet().add(userTokenKey, jti);
        cacheManager.expire(userTokenKey, timeOut, TimeUnit.SECONDS);
    }

    @Override
    public void loginOut(String userId) {
        Set<String> members = getUserToken(userId);
        if (members != null && !members.isEmpty()) {
            members.forEach(jti -> cacheManager.delete(TokenConstant.getUserInfoKey(jti)));
        }
        cacheManager.delete(TokenConstant.getUserTokenKey(userId));
    }

    @Override
    public void loginOut(String userId, String jti) {
        cacheManager.delete(TokenConstant.getUserInfoKey(jti));
        cacheManager.opsForSet().remove(TokenConstant.getUserTokenKey(userId), jti);
    }

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        return cacheManager.opsForHash().entries(TokenConstant.getUserInfoKey(key));
    }

    @Override
    public void saveLoginUser(String userId, String token, long expire) {
        String key = LOGIN_USER_KEY_PREFIX + userId;
        cacheManager.set(key, token, expire);
    }

    @Override
    public String getLoginUser(String userId) {
        String key = LOGIN_USER_KEY_PREFIX + userId;
        return cacheManager.get(key);
    }

    @Override
    public void deleteLoginUser(String userId) {
        String key = LOGIN_USER_KEY_PREFIX + userId;
        cacheManager.delete(key);
    }

}
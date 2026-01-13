package com.simple.common.auth.server.manager;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.auth.client.common.manager.user.ClientLoginInfoManager;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * 默认的登录成功操作的实现
 *
 * @author qty
 */
@Component(value = LoginInfoManager.server_manager_name)
public class ServerLoginUserOperationManager extends ClientLoginInfoManager implements LoginUserOperationManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public void saveUserInfo(TokenData tokenData, boolean isLogin) {

        //用户id
        String userId = tokenData.getSaveInfoMap().get(TokenConstant.userIdKey);

        //过期时间
        long timeOut = Long.parseLong(tokenData.getSaveInfoMap().get(TokenConstant.rEtKey));

        //jti
        String jti = tokenData.getRefreshTokenMap().get(TokenConstant.jtiKey).toString();

        //只允许一个人登录，先退出
        if (clientAuthInfo.getOneLogin()) {
            loginOut(userId);
        }

        //保存用户信息，过期时间为refresh过期时间
        String infoKey = TokenConstant.getUserInfoKey(jti);
        redisTemplate.opsForHash().putAll(infoKey, tokenData.getSaveInfoMap());
        redisTemplate.expire(infoKey, timeOut, TimeUnit.SECONDS);

        //保存用户token关联信息
        String userTokenKey = TokenConstant.getUserTokenKey(userId);
        redisTemplate.opsForSet().add(userTokenKey, jti);
        redisTemplate.expire(userTokenKey, timeOut, TimeUnit.SECONDS);
    }

    @Override
    public void loginOut(String userId) {
        Set<String> members = getUserToken(userId);
        if (members != null && !members.isEmpty()) {
            members.forEach(jti -> redisTemplate.delete(TokenConstant.getUserInfoKey(jti)));
        }
        redisTemplate.delete(TokenConstant.getUserTokenKey(userId));
    }

    @Override
    public void loginOut(String userId, String jti) {
        redisTemplate.delete(TokenConstant.getUserInfoKey(jti));
        redisTemplate.opsForSet().remove(TokenConstant.getUserTokenKey(userId), jti);
    }

    @Override
    public Map<Object, Object> getUserInfo(String key) {
        return redisTemplate.opsForHash().entries(TokenConstant.getUserInfoKey(key));
    }

}

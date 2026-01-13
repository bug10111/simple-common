package com.simple.common.auth.server.service.user;

import com.simple.common.auth.client.common.constant.LoginInfoConstant;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.auth.server.common.service.user.LoginUserService;
import com.simple.common.core.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultLoginUserService implements LoginUserService {

    @Autowired
    private LoginUserOperationManager loginUserOperationManager;

    @Override
    public Map<String, String> getUserInformation() {
        String jti = LoginUserUtils.getUserTemporary().getJti();
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        HashSet<String> loginRole = LoginUserUtils.getUserTemporary().getLoginRole();

        Map<Object, Object> userInfo = loginUserOperationManager.getUserInfo(jti);
        Map<Object, Map<Object,Object>> authorities = loginUserOperationManager.getAuthorities(loginRole);
        Set<String> userTokenUserToken = loginUserOperationManager.getUserToken(userId);

        Map<String, String> userInfoMap = new HashMap<>();
        userInfoMap.put(LoginInfoConstant.user_info_name, JsonUtils.toJsonStr(userInfo));
        userInfoMap.put(LoginInfoConstant.user_auth_name, JsonUtils.toJsonStr(authorities));
        userInfoMap.put(LoginInfoConstant.user_token_name, JsonUtils.toJsonStr(userTokenUserToken));
        return userInfoMap;
    }
}

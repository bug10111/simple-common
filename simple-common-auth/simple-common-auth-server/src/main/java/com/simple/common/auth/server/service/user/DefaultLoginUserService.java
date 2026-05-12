package com.simple.common.auth.server.service.user;

import com.simple.common.auth.client.common.constant.TokenConstant;
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

        Map<Object, Object> userInfo = loginUserOperationManager.getUserInfo(jti);
        Set<String> userTokenUserToken = loginUserOperationManager.getUserToken(userId);
        
        // 获取当前用户的角色列表
        HashSet<String> loginRole = (HashSet<String>) userInfo.get(TokenConstant.loginRole);
        
        // 根据当前服务的 projectCode 获取权限
        Map<Object, Map<Object, Object>> authorities = loginUserOperationManager.getAuthorities(loginRole);

        Map<String, String> userInfoMap = new HashMap<>();
        userInfoMap.put(TokenConstant.userInfoName, JsonUtils.toJsonStr(userInfo));
        userInfoMap.put(TokenConstant.userTokenName, JsonUtils.toJsonStr(userTokenUserToken));
        // 添加权限信息（按 projectCode 过滤）
        userInfoMap.put(TokenConstant.userAuthName, JsonUtils.toJsonStr(authorities));
        return userInfoMap;
    }
}

package com.simple.common.auth.server.service.user;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.auth.server.common.service.user.LoginUserService;
import com.simple.common.core.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 登录用户信息服务默认实现。
 * <p>
 * 内省接口 (/auth/api/user) 的核心逻辑，从 Redis 中读取用户信息、
 * token 关联、权限数据并返回给客户端。
 * </p>
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

        // 从 Redis 读取用户信息
        Map<Object, Object> userInfo = loginUserOperationManager.getUserInfo(jti);
        Set<String> userTokenSet = loginUserOperationManager.getUserToken(userId);

        // 解析角色列表（缓存中可能为 JSON 字符串或 HashSet，需兼容）
        HashSet<String> loginRole = parseLoginRoles(userInfo.get(TokenConstant.loginRole));

        // 根据角色获取权限
        Map<Object, Map<Object, Object>> authorities = loginUserOperationManager.getAuthorities(loginRole);

        // 组装返回数据
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put(TokenConstant.userInfoName, JsonUtils.toJsonStr(userInfo));
        resultMap.put(TokenConstant.userTokenName, JsonUtils.toJsonStr(userTokenSet));
        resultMap.put(TokenConstant.userAuthName, JsonUtils.toJsonStr(authorities));
        return resultMap;
    }

    /**
     * 解析用户角色数据，兼容 HashSet 和 JSON 字符串两种格式。
     * <p>
     * Redis 缓存中 Collection 类型值会序列化为 JSON 字符串，
     * 取出后需反序列化回集合。
     * </p>
     *
     * @param loginRoleObj 角色数据（HashSet 或 JSON 字符串）
     * @return 角色标识集合
     */
    private HashSet<String> parseLoginRoles(Object loginRoleObj) {
        if (loginRoleObj == null) {
            return new HashSet<>();
        }
        if (loginRoleObj instanceof HashSet) {
            return (HashSet<String>) loginRoleObj;
        }
        // Redis 序列化后为 JSON 字符串格式
        List<String> roleList = JsonUtils.toList(loginRoleObj.toString(), String.class);
        return new HashSet<>(roleList);
    }
}

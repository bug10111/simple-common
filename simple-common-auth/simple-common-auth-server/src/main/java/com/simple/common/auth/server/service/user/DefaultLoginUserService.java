package com.simple.common.auth.server.service.user;

import com.simple.common.auth.client.common.constant.TokenConstant;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.manager.user.LoginUserOperationManager;
import com.simple.common.auth.server.common.service.user.LoginUserService;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 登录用户信息服务默认实现。
 * <p>
 * 内省接口 (/auth/api/user) 的核心逻辑，从缓存中读取用户信息、
 * token 关联、权限数据并返回给客户端。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultLoginUserService implements LoginUserService {

    @Autowired
    private LoginUserOperationManager loginUserOperationManager;

    @Override
    public Map<String, String> getUserInformation() {
        // 登录上下文仅解析一次 ThreadLocal；内省端点必须由有效登录态驱动，
        // 禁止以 null 拼出 "login:info:null" 这类垃圾缓存键
        UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
        String jti = userTemporary.getJti();
        String userId = userTemporary.getUserId();
        AssertUtils.notEmpty(jti, LoginException.LOGIN_EXPIRED, "未检测到登录用户");
        AssertUtils.notEmpty(userId, LoginException.LOGIN_EXPIRED, "未检测到登录用户");

        // 从缓存读取用户信息
        Map<Object, Object> userInfo = loginUserOperationManager.getUserInfo(jti);
        // 内省数据快速失败：JWT 可能仍在有效期内而缓存已过期/被逐出——必须显式报登录失效，
        // 而不是返回 {"userInfo":"{}","userToken":"[]","userAuth":"{}"} 让调用方无从分辨
        AssertUtils.notEmpty(userInfo, LoginException.LOGIN_EXPIRED, "登录状态已失效，请重新登录");
        Set<String> userTokenSet = loginUserOperationManager.getUserToken(userId);

        // 解析角色列表（缓存中可能为集合对象或 JSON 字符串，需兼容）
        Set<String> loginRoles = parseLoginRoles(userInfo.get(TokenConstant.loginRole));

        // 根据角色获取权限
        Map<Object, Map<Object, Object>> authorities = loginUserOperationManager.getAuthorities(loginRoles);

        // 组装返回数据
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put(TokenConstant.userInfoName, JsonUtils.toJsonStr(userInfo));
        resultMap.put(TokenConstant.userTokenName, JsonUtils.toJsonStr(userTokenSet));
        resultMap.put(TokenConstant.userAuthName, JsonUtils.toJsonStr(authorities));
        return resultMap;
    }

    /**
     * 解析用户角色数据，兼容集合对象和 JSON 字符串两种格式。
     * <p>
     * 缓存中 Collection 类型值可能为真实集合对象（本地缓存）或 JSON 字符串（Redis 序列化），
     * 统一按 Collection 判定并拷贝为新集合返回（不回传缓存内部引用，避免调用方篡改缓存态）；
     * JSON 解析失败或非数组值降级为空集合并告警，不让脏缓存数据打断内省请求。
     * </p>
     *
     * @param loginRoleObj 角色数据（集合对象或 JSON 字符串）
     * @return 角色标识集合
     */
    private Set<String> parseLoginRoles(Object loginRoleObj) {
        if (loginRoleObj == null) {
            return new HashSet<>();
        }
        if (loginRoleObj instanceof Collection<?> loginRoleCollection) {
            // 拷贝为新的 HashSet，规避对具体实现类型的强转与缓存内部引用外泄
            Set<String> roles = new HashSet<>();
            for (Object role : loginRoleCollection) {
                if (role != null) {
                    roles.add(role.toString());
                }
            }
            return roles;
        }
        // 缓存序列化后为 JSON 字符串格式
        try {
            List<String> roleList = JsonUtils.toList(loginRoleObj.toString(), String.class);
            return roleList == null ? new HashSet<>() : new HashSet<>(roleList);
        } catch (Exception e) {
            log.warn("解析缓存角色数据失败，降级为空集合: value={}", loginRoleObj, e);
            return new HashSet<>();
        }
    }
}

package com.simple.common.auth.client.common.manager.user;

import com.simple.common.auth.client.common.entity.login.UserTemporary;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 登录用户信息管理器接口。
 * <p>
 * 用于管理当前登录用户的临时信息和身份标识。
 * 默认实现 {@link com.simple.common.auth.client.common.manager.user.ClientLoginInfoManager} 基于 ThreadLocal 存储。
 * 如需自定义用户信息存储方式，可实现此接口并替换默认实现。
 * </p>
 *
 * @author qty
 */
public interface LoginInfoManager {

    String client_manager_name = "clientLoginInfoManager";
    String server_manager_name = "serverLoginInfoManager";

    /**
     * 获取token内省的用户信息
     *
     * @param key key
     * @return 用户信息
     */
    Map<Object, Object> getUserInfo(String key);

    /**
     * 获取登录用户权限信息
     *
     * @param loginRole 登录角色
     * @return 用户权限信息
     */
    Map<Object, Map<Object,Object>> getAuthorities(HashSet<String> loginRole);

    /**
     * 获取用户token关联
     *
     * @param userId 用户id
     */
    Set<String> getUserToken(String userId);

    /**
     * 判断当前用户是否包含目标权限信息
     *
     * @param loginRole 登录的角色
     */
    Boolean hasAuth(HashSet<String> loginRole, String[] authority);

}
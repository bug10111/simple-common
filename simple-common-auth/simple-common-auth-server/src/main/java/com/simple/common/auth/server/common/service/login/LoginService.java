package com.simple.common.auth.server.common.service.login;

import com.simple.common.auth.server.common.adapter.LoginTypeAdapter;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * 登录相关接口
 *
 * @author 兄台丶请冷静
 */
public interface LoginService {

    /**
     * 登录
     *
     * @param adapter   请求对象实体
     * @param loginType 登录类型
     */
    Map<String, String> login(Object adapter, LoginTypeAdapter loginType);

    /**
     * 刷新登录
     *
     * @param refreshTokenStr 刷新token
     */
    Map<String, String> refresh(String refreshTokenStr);

    /**
     * 退出登录
     *
     * @param userId 用户id
     */
    void logout(String userId);

    /**
     * 退出登录
     */
    void logout();

}

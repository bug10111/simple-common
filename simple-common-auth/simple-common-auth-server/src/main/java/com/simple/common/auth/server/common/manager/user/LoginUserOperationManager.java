package com.simple.common.auth.server.common.manager.user;

import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.entity.TokenData;

/**
 * Created with IntelliJ IDEA
 * 登录用户数据处理接口
 *
 * @author qty
 */
public interface LoginUserOperationManager extends LoginInfoManager {

    /**
     * 登录数据保存
     *
     * @param tokenData 记录token的数据对象
     * @param isLogin 是登陆-true,刷新登录-false
     */
    void saveUserInfo(TokenData tokenData,boolean isLogin);

    /**
     * 退出登录，清除所有登录信息和权限信息
     *
     * @param userId 用户ID
     */
    void loginOut(String userId);

    /**
     * 退出登录，不清除权限，只退出自己，不退出其他人的相同账号
     *
     * @param userId 用户ID
     * @param jti    jti
     */
    void loginOut(String userId, String jti);

    /**
     * 退出登录，清除当前用户和权限信息
     */
    default void loginOut() {
        String jti = LoginUserUtils.getUserTemporary().getJti();
        String userId = LoginUserUtils.getUserTemporary().getUserId();
        loginOut(userId, jti);
    }

}

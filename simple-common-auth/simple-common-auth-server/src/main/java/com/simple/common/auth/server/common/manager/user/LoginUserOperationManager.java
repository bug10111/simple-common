package com.simple.common.auth.server.common.manager.user;

import com.simple.common.auth.client.common.entity.login.UserTemporary;
import com.simple.common.auth.client.common.enums.login.LoginException;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;
import com.simple.common.auth.client.util.LoginUserUtils;
import com.simple.common.auth.server.common.entity.TokenData;
import com.simple.common.core.utils.AssertUtils;

/**
 * 登录用户操作管理器接口。
 * <p>
 * 用于管理登录用户的信息存储和操作记录。
 * 默认实现 {@link com.simple.common.auth.server.manager.ServerLoginUserOperationManager} 将用户信息存储到Redis缓存。
 * </p>
 *
 * @author qty
 */
public interface LoginUserOperationManager extends LoginInfoManager {

    /**
     * 登录数据保存
     * <p>
     * tokenData 不能为 {@code null}，且必须已包含：saveInfoMap 中的 userId、ret（刷新令牌有效期秒数）、
     * refreshTokenMap 中的 jti；任一缺失时实现方应以明确异常快速失败，不得深入实现后 NPE。
     * </p>
     *
     * @param tokenData 记录token的数据对象
     * @param isLogin   是登录-true，刷新登录-false
     */
    void saveUserInfo(TokenData tokenData, boolean isLogin);

    /**
     * 原子消费指定 jti（刷新令牌轮换的单赢家 CAS）。
     * <p>
     * 实现契约：仅当该 jti 确实归属该用户且尚未被消费时，原子移除其归属记录并清除内省数据，返回 true；
     * 并发/重放请求一律返回 false。缺省实现为非原子的兼容语义（直接登出后返回成功），
     * 建议实现方覆写为原子的移除语义，否则刷新令牌轮换存在并发重放窗口。
     * </p>
     *
     * @param userId 用户ID
     * @param jti    待消费的 jti
     * @return true 消费成功（本次轮换权归属当前请求）；false 已失效或已被并发消费
     */
    default boolean consumeToken(String userId, String jti) {
        loginOut(userId, jti);
        return true;
    }

    /**
     * 退出登录，清除该用户所有登录信息和权限信息
     *
     * @param userId 用户ID
     */
    void loginOut(String userId);

    /**
     * 退出登录，不清除权限，只退出自己（当前 jti 会话），不退出其他人的相同账号
     *
     * @param userId 用户ID
     * @param jti    要退出的会话标识
     */
    void loginOut(String userId, String jti);

    /**
     * 退出当前登录上下文对应的会话。
     * <p>
     * 仅退出当前 jti 会话（不清除权限，不影响同账号其他会话）；需要全端下线请调用 {@link #loginOut(String)}。
     * 登录上下文仅解析一次 ThreadLocal，且 jti/userId 任一缺失即以登录失效异常失败，避免以 null 拼出垃圾缓存键。
     * </p>
     */
    default void loginOut() {
        UserTemporary userTemporary = LoginUserUtils.getUserTemporary();
        String jti = userTemporary.getJti();
        String userId = userTemporary.getUserId();
        AssertUtils.notEmpty(jti, LoginException.LOGIN_EXPIRED, "未检测到登录用户，无法退出登录");
        AssertUtils.notEmpty(userId, LoginException.LOGIN_EXPIRED, "未检测到登录用户，无法退出登录");
        loginOut(userId, jti);
    }

}

package com.simple.common.logs.client.common.manager;

/**
 * 日志用户信息管理器接口。
 * <p>
 * 用于在日志记录时获取当前操作用户的身份信息（如用户ID、用户名等）。
 * 默认实现 {@link com.simple.common.logs.client.manager.DefaultLogUserManager} 返回 null，
 * 如需记录用户信息，请继承 {@link com.simple.common.logs.client.manager.DefaultLogUserManager} 并重写相关方法。
 * </p>
 *
 * @author qty
 */
public interface LogUserManager {


    /**
     * 获取用户昵称
     *
     * @return 用户昵称,未登录时返回默认值
     */
    String loginNickName();

    /**
     * 获取登录用户ID
     *
     * @return 用户ID,未登录时返回默认值
     */
    String loginUserId();

}
package com.simple.common.logs.client.common.manager;

/**
 * 日志用户信息管理器接口。
 * <p>
 * 用于在日志记录时获取当前操作用户的身份信息（如用户ID、用户名等）。
 * 默认实现 {@link com.simple.common.logs.client.manager.DefaultLogUserManager} 返回占位用户
 * （"测试用户ID"/"测试用户名"，非 null），并仅输出一次 warn 提示；
 * 如需记录真实用户信息，请继承 DefaultLogUserManager 并同时重写
 * {@code loginUserId()} 与 {@code loginNickName()}（只覆写其一会导致日志中
 * 一半真实一半占位的混合数据且无告警），注册为 Bean 后默认实现自动退避。
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
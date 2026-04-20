package com.simple.common.auth.server.common.manager.login;

import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;
import com.simple.common.auth.server.common.entity.TokenData;

/**
 * 登录管理器接口。
 * <p>
 * 用于处理特定登录类型的用户认证逻辑。
 * 不同登录方式（如密码登录、微信登录、短信登录等）通过实现此接口来定义各自的认证流程。
 * 默认抽象实现 {@link AbsLoginManager} 提供了通用的登录流程框架。
 * </p>
 *
 * @author qty
 */
public interface LoginManager {

    /**
     * 判断参数类型是否匹配
     *
     * @param adapter 参数对象
     */
    boolean support(Object adapter);

    /**
     * 获取登录时候需要构造的用户信息
     *
     * @param clientDetails 客户端信息
     * @param adapter       登录参数
     */
    AbsUserDetails login(ClientDetails clientDetails, Object adapter);
}
package com.simple.common.auth.server.common.manager.login;

import com.simple.common.auth.server.common.entity.AbsUserDetails;
import com.simple.common.auth.server.common.entity.ClientDetails;

/**
 * Created with IntelliJ IDEA
 * 登录实现接口
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

package com.simple.common.auth.server.common.service.user;

import com.simple.common.auth.server.common.entity.AbsUserDetails;

import java.util.Map;

/**
 * 登录用户服务接口。
 * <p>
 * 提供用户详情获取功能，用于token刷新和用户信息查询。
 * 默认实现 {@link com.simple.common.auth.server.service.user.DefaultLoginUserService}
 * </p>
 *
 * @author qty
 */
public interface LoginUserService {

    /**
     * 获取登录用户内省信息
     *
     * @return 登录用户详细信息
     */
    Map<String, String> getUserInformation();

}
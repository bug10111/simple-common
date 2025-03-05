package com.simple.common.auth.server.common.service.user;

import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 登录用户服务接口
 *
 * @author 兄台丶请冷静
 */
public interface LoginUserService {

    /**
     * 获取登录用户内省信息
     *
     * @return 登录用户详细信息
     */
    Map<String, String> getUserInformation();

}

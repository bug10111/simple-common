package com.simple.common.core.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.common.service.jwt.CoreLoginUserService;

/**
 * Created with IntelliJ IDEA
 * Description: 全局获取userId帮助类
 *
 * @author qty
 */
public class UserIdUtils {

    /**
     * 获取用户ID
     */
    public static String getUserId() {
        CoreLoginUserService bean = SpringUtil.getBean(CoreLoginUserService.class);
        return bean.getUserId();
    }

}

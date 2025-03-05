package com.simple.common.auth.client.util;

import cn.hutool.core.util.ObjUtil;
import com.simple.common.auth.client.common.entity.login.UserTemporary;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: 登录用户信息工具类
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class LoginUserUtils {

    private static final ThreadLocal<UserTemporary> loginInfo = new ThreadLocal<>();

    /**
     * 添加登录用户信息
     *
     * @param info 用户信息
     */
    public static void add(UserTemporary info) {
        loginInfo.set(info);
    }

    /**
     * 获取登录用户信息
     */
    public static UserTemporary getUserTemporary() {
        UserTemporary userTemporary = loginInfo.get();
        if (ObjUtil.isNull(userTemporary)) {
            log.error("没有登陆，LoginUserUtils工具返回空的数据对象！");
            userTemporary = new UserTemporary();
        }
        return userTemporary;
    }

    /**
     * 删除登录用户信息
     */
    public static void remove() {
        loginInfo.remove();
    }

}

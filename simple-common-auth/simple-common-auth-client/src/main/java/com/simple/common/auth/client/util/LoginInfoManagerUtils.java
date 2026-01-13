package com.simple.common.auth.client.util;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.auth.client.common.manager.user.LoginInfoManager;

/**
 * Created with IntelliJ IDEA
 * Description: 登录信息实现的单例工具
 *
 * @author qty
 */
public class LoginInfoManagerUtils {

    private static volatile LoginInfoManager cliLoginInfoManager;

    private static volatile LoginInfoManager serLoginInfoManager;

    /**
     * 获取客户端登录信息管理器
     *
     * @return 客户端登录信息管理器
     */
    public static LoginInfoManager getCliLoginInfoManager() {
        if (cliLoginInfoManager == null) {
            synchronized (LoginInfoManager.class) {
                if (cliLoginInfoManager == null) {
                    cliLoginInfoManager = SpringUtil.getBean(LoginInfoManager.client_manager_name);
                }
            }
        }
        return cliLoginInfoManager;
    }

    /**
     * 获取服务端登录信息管理器
     *
     * @return 服务端登录信息管理器
     */
    public static LoginInfoManager getSerLoginInfoManager() {
        if (serLoginInfoManager == null) {
            synchronized (LoginInfoManager.class) {
                if (serLoginInfoManager == null) {
                    serLoginInfoManager = SpringUtil.getBean(LoginInfoManager.server_manager_name);
                }
            }
        }
        return serLoginInfoManager;
    }

}

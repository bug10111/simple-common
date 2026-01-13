package com.simple.common.auth.server.common.adapter;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.auth.server.common.manager.login.LoginManager;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public interface LoginTypeAdapter {

    Class<? extends LoginManager> getAClass();

    default LoginManager getLoginManager() {
        return SpringUtil.getBean(getAClass());
    }
}

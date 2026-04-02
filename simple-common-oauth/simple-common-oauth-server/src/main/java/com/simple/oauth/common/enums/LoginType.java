package com.simple.oauth.common.enums;

import com.simple.common.auth.server.common.adapter.LoginTypeAdapter;
import com.simple.common.auth.server.common.manager.login.LoginManager;
import com.simple.oauth.manager.login.PwdLoginManager;
import com.simple.oauth.manager.login.WxLoginManager;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 登录类型
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum LoginType implements LoginTypeAdapter {
    PWD_LOGIN("账号密码登录", PwdLoginManager.class),
    WX_LOGIN("小程序登录", WxLoginManager.class),

    ;

    //登录方式
    private final String name;

    //对应实现
    private final Class<? extends LoginManager> aClass;
}

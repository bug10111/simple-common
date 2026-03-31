package com.simple.common.auth.client.common.enums.login;

import com.simple.common.core.exception.AbstractException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum LoginException implements AbstractException {

    LOGIN_EXPIRED("1000", "登录失效，请重新登录"),
    RE_LOGIN_EXPIRED("1001", "登录失效，请重新登录"),
    INSUFFICIENT_PERMISSIONS("1002", "权限不足，请联系管理员"),
    LOGIN_ERROR_NUM("1003", "登录失败次数过多，请稍后再试"),
    LOGIN_IP_ERROR_NUM("1004", "该IP登录失败次数过多，请稍后再试"),

    ;

    private final String code;

    private final String message;

}

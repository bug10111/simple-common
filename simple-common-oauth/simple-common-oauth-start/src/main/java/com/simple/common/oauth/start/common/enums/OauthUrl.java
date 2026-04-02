package com.simple.common.oauth.start.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum OauthUrl {
    CREATE_USER("创建用户", "/api/user"),
    SELECT_USER("查询用户", "/api/user"),
    SELECT_USER_BY_NAME("查询用户", "/api/user/name"),
    SELECT_USER_BY_ROLE_KEY("获取用户列表", "/api/role"),
    UPDATE_USER("修改用户", "/api/user"),
    DELETE_USER("删除用户", "/api/user"),
    REST_USER("重置密码", "/api/user/reset"),

    SMS_SEND("发送短信", "/api/sms/send"),
    SMS_CHECK("校验短信", "/api/sms/check-sms"),

    SELECT_ROLE_BY_ID("查询单个角色信息", "/api/role/info"),
    SELECT_USER_BY_ROLE("根据角色id获取用户列表", "/api/role/id"),
    SELECT_CLIENT("获取服务客户端", "/api/client/list"),
    SELECT_DICT("获取字典数据", "/api/labelList"),
    ;

    private final String name;
    private final String url;
}

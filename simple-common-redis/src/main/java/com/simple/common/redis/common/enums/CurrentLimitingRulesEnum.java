package com.simple.common.redis.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA
 * Description: 限流维度枚举
 *
 * @author qty
 */
@AllArgsConstructor
@Getter
public enum CurrentLimitingRulesEnum {
    /**
     * 按请求路由限流
     */
    URL("按请求路由限流"),
    /**
     * 按当前登录用户限流
     */
    USER_ID("按当前登录用户限流"),
    /**
     * 按 IP 地址限流
     */
    IP("按 IP 地址限流"),
    ;

    /**
     * 限流维度描述（展示用）
     */
    private final String description;
}

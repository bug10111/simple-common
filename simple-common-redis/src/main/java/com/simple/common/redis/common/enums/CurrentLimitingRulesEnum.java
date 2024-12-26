package com.simple.common.redis.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA
 * Description: 限流规则
 *
 * @author 兄台丶请冷静
 */
@AllArgsConstructor
@Getter
public enum CurrentLimitingRulesEnum {
    URL("根据路由限流"), USER_ID("当前登录用户"), IP("IP地址"),
    ;

    //限流方法
    private final String name;
}

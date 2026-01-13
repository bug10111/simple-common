package com.simple.common.redis.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA
 * Description: 限流算法类型
 *
 * @author qty
 */
@AllArgsConstructor
@Getter
public enum CurrentLimitingTypeEnum {
    BARREL("令牌桶算法"),
    ;

    //限流方法
    private final String name;
}

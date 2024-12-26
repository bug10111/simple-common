package com.simple.common.redis.common.enums;

import com.simple.common.core.exception.AbstractException;
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
public enum CurrentLimitingErrorEnum implements AbstractException {
    ERROR("500", "网络繁忙，请稍后再试"),
    ;

    //限流方法
    private final String code;

    private final String message;
}

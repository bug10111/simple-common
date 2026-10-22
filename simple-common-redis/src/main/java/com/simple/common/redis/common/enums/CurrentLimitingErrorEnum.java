package com.simple.common.redis.common.enums;

import com.simple.common.core.exception.AbstractException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA
 * Description: 限流异常枚举
 * <p>
 * code 使用 429（Too Many Requests）：限流拦截与系统内部错误（500）是两类语义，
 * 复用 500 会让网关/监控把正常限流误报为服务故障，调用方也无法获得「退避重试」信号。
 *
 * @author qty
 */
@AllArgsConstructor
@Getter
public enum CurrentLimitingErrorEnum implements AbstractException {
    /**
     * 请求触发限流：HTTP 语义 429 Too Many Requests，提示调用方退避重试
     */
    ERROR("429", "请求过于频繁，请稍后重试"),
    ;

    private final String code;

    private final String message;
}

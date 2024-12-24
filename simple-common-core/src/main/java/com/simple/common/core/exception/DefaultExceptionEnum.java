package com.simple.common.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA on 2023/10/28 15:03
 * 默认异常枚举
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum DefaultExceptionEnum implements AbstractException {

    OK("200", "请求成功"), ERROR("500", "请求失败"),
    ;

    private final String code;

    private final String message;

}

package com.simple.common.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created by IntelliJ IDEA
 * 默认异常枚举
 * <p>注意：本枚举实现 {@link AbstractException}，语义是"异常/错误"，仅供抛错与错误响应使用；
 * 成功码 OK 仅供 {@link com.simple.common.core.response.R} 组装成功响应，禁止经
 * {@code AssertUtils.error(AbstractException)} 等异常通道抛出（AssertUtils 会拒绝200码）。</p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum DefaultExceptionEnum implements AbstractException {

    OK("200", "请求成功"), ERROR("500", "请求失败"),
    ;

    private final String code;

    private final String message;

}

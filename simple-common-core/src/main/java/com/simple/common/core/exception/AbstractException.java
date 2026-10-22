package com.simple.common.core.exception;

/**
 * Created by IntelliJ IDEA
 * 异常状态基类
 *
 * @author qty
 */
public interface AbstractException {

    /**
     * 获取异常的状态码
     */
    String getCode();

    /**
     * 获取异常的提示信息
     */
    String getMessage();

    /**
     * 获取整型状态码
     *
     * @return 状态码整型值
     * @throws IllegalStateException 当 {@link #getCode()} 为 null 或不是合法整数时抛出，
     *                               避免把 NumberFormatException/NPE 之类的底层异常暴露给调用方
     */
    default int getCodeInt() {
        String code = getCode();
        if (code == null || !code.matches("-?\\d+")) {
            throw new IllegalStateException("异常code必须是合法整数, 当前值: " + code);
        }
        return Integer.parseInt(code);
    }
}

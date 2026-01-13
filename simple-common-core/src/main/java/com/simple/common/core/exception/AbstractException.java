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

    default int getCodeInt() {
        return Integer.parseInt(getCode());
    }
}

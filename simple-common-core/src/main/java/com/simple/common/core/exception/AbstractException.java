package com.simple.common.core.exception;

/**
 * Created by 兄台丶请冷静 on 2023/10/28 13:43
 * 异常状态基类
 *
 * @author 兄台丶请冷静
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

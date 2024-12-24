package com.simple.common.core.function;

import com.simple.common.core.exception.AbstractException;
import com.simple.common.core.exception.DefaultException;

/**
 * Created by IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@FunctionalInterface
public interface HttpRecordFunction extends Function {

    /**
     * 获取异常信息
     * @param body http返回值
     * @return 异常信息
     */
    DefaultException handler(String body) throws Throwable;

}

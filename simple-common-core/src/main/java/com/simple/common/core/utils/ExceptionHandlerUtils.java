package com.simple.common.core.utils;

import com.simple.common.core.common.constant.CoreConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: 异常捕获公共方法
 *
 * @author qty
 */
@Slf4j
public class ExceptionHandlerUtils {

    /**
     * 异常处理
     */
    public static void errorHandler(Exception e) {
        log.error("接口[{}]请求失败！===>", HttpServletUtils.getRequest().getRequestURI(), e);
        HttpServletUtils.getRequest().setAttribute(CoreConstant.EXCEPTION, e);
    }

}

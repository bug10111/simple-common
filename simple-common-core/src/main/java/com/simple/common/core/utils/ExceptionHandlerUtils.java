package com.simple.common.core.utils;

import com.simple.common.core.common.constant.CoreConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: 异常捕获公共方法
 *
 * @author qty
 */
@Slf4j
public class ExceptionHandlerUtils {

    private ExceptionHandlerUtils() {
    }

    /**
     * 服务端异常记录：error级别记录完整堆栈，并挂到请求属性供日志链路取用
     */
    public static void errorHandler(Exception e) {
        String uri = currentRequestUri();
        log.error("接口[{}]请求失败", uri, e);
        HttpServletRequest request = currentRequestOrNull();
        if (request != null) {
            request.setAttribute(CoreConstant.EXCEPTION, e);
        }
    }

    /**
     * 客户端请求侧异常记录（400/404类）：warn级别只记消息，避免高频噪音与请求负载细节进日志；
     * 仍挂请求属性供日志链路取用
     */
    public static void warnHandler(Exception e) {
        log.warn("接口[{}]请求失败: {}", currentRequestUri(), e.getMessage());
        HttpServletRequest request = currentRequestOrNull();
        if (request != null) {
            request.setAttribute(CoreConstant.EXCEPTION, e);
        }
    }

    // 请求URI兜底"unknown"：当前线程未绑定请求上下文（异步/定时）时不抛NPE掩盖原始异常
    private static String currentRequestUri() {
        HttpServletRequest request = currentRequestOrNull();
        return request == null ? "unknown" : request.getRequestURI();
    }

    private static HttpServletRequest currentRequestOrNull() {
        try {
            return HttpServletUtils.getRequest();
        } catch (IllegalStateException e) {
            // 当前线程未绑定请求上下文，返回null交由调用方兜底
            return null;
        }
    }
}

package com.simple.common.core.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Created with IntelliJ IDEA.
 * <p>
 * 快捷获取request和response工具类
 */
@Slf4j
public final class HttpServletUtils {

    private HttpServletUtils() {
    }

    /**
     * 获取当前线程绑定的response
     *
     * @return 当前response，未绑定响应时返回null
     * @throws IllegalStateException 当前线程未绑定Servlet请求上下文（如异步/定时任务线程）
     */
    public static HttpServletResponse getResponse() {
        return currentAttributes().getResponse();
    }

    /**
     * 获取当前线程绑定的request
     *
     * @return 当前request
     * @throws IllegalStateException 当前线程未绑定Servlet请求上下文（如异步/定时任务线程），异常消息明确，
     *                               不再是裸的NullPointerException
     */
    public static HttpServletRequest getRequest() {
        return currentAttributes().getRequest();
    }

    private static ServletRequestAttributes currentAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            throw new IllegalStateException("当前线程未绑定Servlet请求上下文(非Web请求线程或异步线程)");
        }
        return servletAttributes;
    }
}

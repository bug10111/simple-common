package com.simple.common.logs.client.common.interceptor;

import com.simple.common.core.utils.IdUtils;
import com.simple.common.logs.client.common.constant.LogConstant;
import com.simple.common.logs.client.common.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 日志拦截器
 *
 * @author qty
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    @Autowired
    private LogService logService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 处理 TraceId：如果请求头中已存在则沿用，否则生成新的
        String traceId = request.getHeader(LogConstant.TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = IdUtils.getSnowflakeNextIdStr();
        }

        // 将 TraceId 放入请求属性中，供后续使用
        request.setAttribute(LogConstant.TRACE_ID_HEADER, traceId);

        //设置请求时间
        request.setAttribute(LogConstant.START_TIME, System.currentTimeMillis());

        // 放行请求
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logService.send(request, response, handler, ex);
        }
    }
}
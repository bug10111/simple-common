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

        // OPTIONS 预检请求不记录日志（统一入口判定，避免两处方法路径判断漂移）
        if (isOptionsRequest(request)) {
            return true;
        }

        // 处理 TraceId：请求头已携带且合法则沿用，否则生成新的
        String traceId = resolveTraceId(request);

        // 将 TraceId 放入请求属性中，供后续使用（专用属性键，避免与 HTTP 头命名空间混用冲突）
        request.setAttribute(LogConstant.TRACE_ID_ATTR, traceId);

        // 将 TraceId 添加到响应头，方便调用方排查问题
        response.setHeader(LogConstant.TRACE_ID_HEADER, traceId);

        // 设置请求开始时间
        request.setAttribute(LogConstant.START_TIME, System.currentTimeMillis());

        // 放行请求
        return true;
    }

    /**
     * 解析 TraceId：仅接受合法字符集与长度（≤64，[A-Za-z0-9_-]），
     * 防止客户端传入畸形值造成响应头/日志注入；不合法时回退新生成
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(LogConstant.TRACE_ID_HEADER);
        if (traceId != null) {
            traceId = traceId.trim();
        }
        if (traceId == null || traceId.isEmpty() || traceId.length() > 64
                || !traceId.matches("[A-Za-z0-9_-]+")) {
            // 使用 fastUUID 生成全局唯一的 TraceId，避免 Snowflake 需要配置 workerId 的问题
            return IdUtils.getFastUUID();
        }
        return traceId;
    }

    private boolean isOptionsRequest(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (isOptionsRequest(request)) {
            return;
        }
        try {
            logService.send(request, response, handler, ex);
        } catch (Exception e) {
            // 日志发送失败不能影响业务异常传递与响应
            log.warn("发送日志失败", e);
        }
    }
}
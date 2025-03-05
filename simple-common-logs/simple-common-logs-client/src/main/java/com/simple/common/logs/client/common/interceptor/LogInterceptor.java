package com.simple.common.logs.client.common.interceptor;

import com.simple.common.core.common.service.thread.ThreadService;
import com.simple.common.logs.client.common.constant.LogConstant;
import com.simple.common.logs.client.common.manager.LogManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Created with IntelliJ IDEA
 * Description: 日志拦截器
 *
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    @Autowired
    private LogManager logManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //设置请求时间
        request.setAttribute(LogConstant.START_TIME, System.currentTimeMillis());

        // 放行请求
        return true;

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logManager.create(request, response, handler, ex);
        }
    }
}

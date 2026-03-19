package com.simple.common.auth.client.common.interceptor;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Created with IntelliJ IDEA
 * Description:Cookie处理拦截器
 *
 * @author qty
 */
@Slf4j
@Component
public class CookieProcessingHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    private ClientAuthInfo clientAuthInfo;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        handleCookie(request, response);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    /**
     * 处理cookie
     */
    protected void handleCookie(HttpServletRequest request, HttpServletResponse response) {

        // 获取所有Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setHttpOnly(true);
                cookie.setSecure("produce".equals(clientAuthInfo.getProduce()));
                cookie.setAttribute("SameSite", "Strict");
                response.addCookie(cookie);
            }
        }
    }
}

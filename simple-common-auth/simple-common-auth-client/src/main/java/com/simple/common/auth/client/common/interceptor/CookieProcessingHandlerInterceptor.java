package com.simple.common.auth.client.common.interceptor;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA
 * Description: Cookie 处理拦截器
 * <p>
 * 对「仅随请求上行、未在本次响应中下发」的 Cookie 补齐 HttpOnly/Secure/SameSite 加固属性后重新下发；
 * 已由本次响应显式 Set-Cookie 的 Cookie（如登出删除、业务自定义下发）不重复下发，
 * 避免覆盖控制器语义与产生重复头。头值经由 {@link ResponseCookie} 校验转义，杜绝手工拼接导致的头注入。
 * </p>
 *
 * @author qty
 */
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

        // 已由本次响应显式下发 Set-Cookie 的 Cookie 名：不再重复加固下发（避免与登出/删除及控制器下发竞争）
        Set<String> responsedNames = new HashSet<>();
        Collection<String> setCookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        for (String sc : setCookies) {
            int eq = sc.indexOf('=');
            if (eq > 0) {
                responsedNames.add(sc.substring(0, eq).trim());
            }
        }

        // 获取所有Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (responsedNames.contains(cookie.getName())) {
                    continue;
                }
                // 通过 ResponseCookie 构建头值：名称/值非法字符（分隔符、CR/LF 等）由其校验并抛出，
                // 不再手工拼接（请求方可控字段直接拼进 Set-Cookie 存在头注入风险）
                ResponseCookie hardened = ResponseCookie.from(cookie.getName(), cookie.getValue())
                        .httpOnly(true)
                        // 请求 Cookie 无法还原原始 Domain/Max-Age：Path 缺省时收敛为当前应用根
                        .path(cookie.getPath() != null ? cookie.getPath() : "/")
                        .secure("produce".equals(clientAuthInfo.getProduce()) || request.isSecure())
                        .sameSite("Strict")
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, hardened.toString());
            }
        }
    }
}

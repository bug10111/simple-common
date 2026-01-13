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

                if (clientAuthInfo.getProduce().equals("produce")) {
                    // 使用https协议连接时cookie才可以被页面访问。可用于防止信息在传递的过程中被监听捕获后信息泄漏。
                    cookie.setSecure(true);
                }

                // 设置HttpOnly属性，通过程序(JS脚本、Applet等)将无法读取到Cookie信息,
                // 防止程序获取cookie后进行攻击止，例如通过document.cookie获取cookie值
                cookie.setHttpOnly(true);
                cookie.setPath("/");

                // 设置SameSite属性为Strict,表示完全禁止第三方cookie,也就是在跨站时，均不会携带cookie,可以有效防止CSRF攻击
                // 不分非正规或者古董浏览器，可能不兼容
                //一般和CSRF token一起使用，可以杜绝绝大多数场景
                response.addHeader("Set-Cookie", cookie + "; SameSite=Strict");
                response.addCookie(cookie);
            }
        }
    }
}

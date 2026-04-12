package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.interceptor.AuthHandlerInterceptor;
import com.simple.common.auth.client.common.interceptor.CookieProcessingHandlerInterceptor;
import com.simple.common.core.common.config.DefaultWebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * Created with IntelliJ IDEA
 * Description: 拦截器配置
 *
 * @author qty
 */
@Service
@Configuration
public class AuthClientWebMvcConfig extends DefaultWebMvcConfigurer {

    @Autowired
    private CookieProcessingHandlerInterceptor cookieProcessingHandlerInterceptor;

    @Autowired
    private AuthHandlerInterceptor authHandlerInterceptor;

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cookieProcessingHandlerInterceptor).addPathPatterns("/**").order(1);
        registry.addInterceptor(authHandlerInterceptor).addPathPatterns("/**").order(2);
    }
}

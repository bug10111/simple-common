package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.interceptor.AuthHandlerInterceptor;
import com.simple.common.auth.client.common.interceptor.CookieProcessingHandlerInterceptor;
import com.simple.common.core.common.config.DefaultWebMvcConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * Created with IntelliJ IDEA
 * Description: 拦截器配置
 *
 * @author qty
 */
@Configuration
public class AuthClientWebMvcConfig extends DefaultWebMvcConfigurer {

    @Autowired
    private CookieProcessingHandlerInterceptor cookieProcessingHandlerInterceptor;

    @Autowired
    private AuthHandlerInterceptor authHandlerInterceptor;

    /**
     * 拦截器配置。
     * <p>
     * 仅排除 /error：Spring Boot 错误分发路径必须可达，否则鉴权拦截器会把错误页渲染本身
     * 拦截为 401，掩盖原始异常。业务白名单（登录页、文档等）请通过 AbsClientAuthConfig 声明，
     * 不在此处默认放开（默认放开即 fail-open）。
     * </p>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cookieProcessingHandlerInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/error").order(1);
        registry.addInterceptor(authHandlerInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/error").order(2);
    }
}

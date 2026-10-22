package com.simple.common.logs.client.common.config;

import com.simple.common.core.common.config.DefaultWebMvcConfigurer;
import com.simple.common.logs.client.common.interceptor.LogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

/**
 * Created with IntelliJ IDEA
 * Description: 拦截器配置
 *
 * @author qty
 */
@Configuration
public class LogClientMvcConfig extends DefaultWebMvcConfigurer {

    private final LogInterceptor logInterceptor;

    public LogClientMvcConfig(LogInterceptor logInterceptor) {
        this.logInterceptor = logInterceptor;
    }

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                // 排除静态资源/文档/监控端点，避免非业务请求产生大量无意义日志
                .excludePathPatterns("/static/**", "/webjars/**", "/favicon.ico", "/error",
                        "/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**")
                // 须晚于认证拦截器（order 1/2），以便记录到当前登录用户信息
                .order(4);
    }
}
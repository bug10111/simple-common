package com.simple.common.logs.client.common.config;

import com.simple.common.core.common.config.DefaultWebMvcConfigurer;
import com.simple.common.logs.client.common.interceptor.LogInterceptor;
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
public class LogClientMvcConfig extends DefaultWebMvcConfigurer {

    @Autowired
    private LogInterceptor logInterceptor;

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor).addPathPatterns("/**").order(4);
    }
}
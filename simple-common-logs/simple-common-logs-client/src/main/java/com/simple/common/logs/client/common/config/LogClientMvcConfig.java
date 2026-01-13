package com.simple.common.logs.client.common.config;

import com.simple.common.core.common.config.DefaultWebMvcConfigurer;
import com.simple.common.logs.client.common.interceptor.LogInterceptor;
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
public class LogClientMvcConfig extends DefaultWebMvcConfigurer {

    @Autowired
    private LogInterceptor logInterceptor;

    //    @Bean
    //    @Qualifier(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
    //    public DispatcherServlet dispatcherServlet() {
    //        return new XinDispatcherServlet();
    //    }

    /**
     * 拦截器配置
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logInterceptor).addPathPatterns("/**").order(4);
    }
}

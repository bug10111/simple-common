package com.simple.common.logs.client.common.config;

import com.simple.common.logs.client.common.filter.LogFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 日志客户端配置类
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = "com.simple.common.logs.client")
public class LogClientConfig {

    /**
     * 显式注册日志过滤器，确保其在所有请求处理前执行
     * <p>
     * 该过滤器负责包装 HttpServletRequest 为 CachedBodyHttpServletRequest，
     * 使得请求体可以被多次读取，用于日志记录。
     * </p>
     *
     * @param logFilter 日志过滤器实例
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<LogFilter> logFilterRegistration(LogFilter logFilter) {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(logFilter);
        registration.addUrlPatterns("/*");
        // 设置最高优先级 + 10，确保在其他过滤器之前执行
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

}
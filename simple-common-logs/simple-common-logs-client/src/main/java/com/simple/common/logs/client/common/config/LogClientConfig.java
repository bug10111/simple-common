package com.simple.common.logs.client.common.config;

import com.simple.common.logs.client.common.filter.LogFilter;
import com.simple.common.logs.client.common.manager.LogUserManager;
import com.simple.common.logs.client.manager.DefaultLogUserManager;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 日志客户端配置类
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = "com.simple.common.logs.client")
public class LogClientConfig {

    /**
     * 显式注册日志过滤器，确保其在业务过滤器之前执行
     * <p>
     * 该过滤器负责将 HttpServletRequest 包装为 CachedBodyHttpServletRequest，
     * 使请求体可以被重复读取，用于日志记录。
     * </p>
     *
     * @param logFilter 日志过滤器实例
     * @param properties 日志客户端配置
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<LogFilter> logFilterRegistration(LogFilter logFilter, LogTcpClientProperties properties) {
        FilterRegistrationBean<LogFilter> registration = new FilterRegistrationBean<>(logFilter);
        registration.addUrlPatterns("/*");
        // 显式覆盖 REQUEST/ASYNC/ERROR/FORWARD 派发类型：
        // 容器默认仅 REQUEST，缺失 ERROR/ASYNC 派发日志（/error、异步控制器等）
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD);
        // 执行顺序可配置（默认最高优先级 + 10）：
        // 若更早的过滤器（签名/解密等）已消费请求体，可将 simple.logs.client.filter-order 调得更靠前
        registration.setOrder(properties.getFilterOrder());
        return registration;
    }

    /**
     * 默认日志用户管理器（占位实现）。
     * <p>
     * 使用方注册任意 {@link LogUserManager} 实现后本默认 Bean 自动退避，
     * 无需再以 @Primary/@Qualifier 消解注入冲突。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(LogUserManager.class)
    public DefaultLogUserManager defaultLogUserManager() {
        return new DefaultLogUserManager();
    }
}
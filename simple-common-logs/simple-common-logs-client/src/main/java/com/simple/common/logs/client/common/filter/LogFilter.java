package com.simple.common.logs.client.common.filter;

import com.simple.common.logs.client.common.httpservletrequest.CachedBodyHttpServletRequest;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA
 * Description: 日志过滤器，包装CachedBodyHttpServletRequest
 * <p>
 * 仅通过 {@code LogClientConfig#logFilterRegistration} 以 FilterRegistrationBean 注册
 * （不使用 @WebFilter 注解：/** 非法映射，且可能双重注册），执行顺序见配置项
 * {@code simple.logs.client.filter-order}。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class LogFilter implements Filter {

    @Autowired
    private LogTcpClientProperties logTcpClientProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 非 HTTP 请求不做包装，直接放行（修复无守卫强转导致 ClassCastException 中断请求的问题）
        if (!(request instanceof HttpServletRequest httpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest wrappedRequest;
        try {
            wrappedRequest = new CachedBodyHttpServletRequest(httpServletRequest, logTcpClientProperties);
        } catch (Exception e) {
            // 请求体缓存失败不得阻塞业务请求：降级为原始请求继续链路，仅记录诊断日志
            log.error("构建请求体缓存包装失败，降级为原始请求继续链路", e);
            wrappedRequest = httpServletRequest;
        }
        chain.doFilter(wrappedRequest, response);
    }
}
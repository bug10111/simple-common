package com.simple.common.logs.client.common.filter;

import com.simple.common.logs.client.common.httpservletrequest.CachedBodyHttpServletRequest;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA
 * Description: 日志过滤器，传递CachedBodyHttpServletRequest
 *
 * @author qty
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Component
@WebFilter(urlPatterns = "/**", filterName = "logFilter")
public class LogFilter implements Filter {

    @Autowired
    private LogTcpClientProperties logTcpClientProperties;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest((HttpServletRequest) request, logTcpClientProperties);
        chain.doFilter(cachedBodyHttpServletRequest, response);
    }
}
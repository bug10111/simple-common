package com.simple.common.logs.client.common.filter;

import com.simple.common.logs.client.common.httpservletrequest.CachedBodyHttpServletRequest;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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
@Order(999)
@Component
@WebFilter(urlPatterns = "/**", filterName = "logFilter")
public class LogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest((HttpServletRequest) request);
        chain.doFilter(cachedBodyHttpServletRequest, response);
    }
}

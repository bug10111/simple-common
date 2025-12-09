package com.come.on.alibaba;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Configuration
public class FeignConfig implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            String token = requestAttributes.getRequest().getHeader("Authorization");
            requestTemplate.header("Authorization", token);
        }
    }
}
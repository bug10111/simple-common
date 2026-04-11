package com.come.on.alibaba;

import com.simple.common.auth.client.common.constant.TokenConstant;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Configuration
public class FeignConfig implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {

            //传递token
            String token = requestAttributes.getRequest().getHeader(TokenConstant.Authorization);
            if (token != null) {
                requestTemplate.header(TokenConstant.Authorization, token);
            }

            //传递序序列化数据
            String encoded = requestAttributes.getRequest().getHeader(TokenConstant.userHead);
            if (encoded != null) {
                requestTemplate.header(TokenConstant.userHead, encoded);
            }

            //传递签名
            String sign = requestAttributes.getRequest().getHeader(TokenConstant.userSignHead);
            if (sign != null) {
                requestTemplate.header(TokenConstant.userSignHead, sign);
            }
        }

    }
}
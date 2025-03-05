package com.simple.common.logs.client.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Configuration
@ComponentScan(basePackages = "com.simple.common.logs.client")
public class LogClientConfig {

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

}

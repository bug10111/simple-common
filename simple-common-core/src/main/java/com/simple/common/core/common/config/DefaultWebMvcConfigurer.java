package com.simple.common.core.common.config;

import cn.hutool.extra.spring.EnableSpringUtil;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Created by IntelliJ IDEA
 * 跨域配置
 *
 * @author qty
 */
@Configuration
@EnableSpringUtil
@ComponentScan(basePackages = { "com.simple.common.core" })
public class DefaultWebMvcConfigurer implements WebMvcConfigurer {

    static final String[] ORIGINS = new String[] { "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS" };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") //指定路径
                .allowedOriginPatterns("*") //指定站点
                .allowedHeaders("*") //指定站点
                .exposedHeaders("*").allowCredentials(true) //允许携带凭证
                .allowedMethods(ORIGINS) //方法
                .maxAge(3600); //设置预检请求的缓存时间（秒），在这个时间段里，对于相同的跨域请求不会再预检了
    }

    //    //让cors高于拦截器的权限
    //    @Bean
    //    public CorsFilter corsFilter() {
    //        CorsConfiguration config = new CorsConfiguration();
    //        config.addAllowedOriginPattern("*");
    //        config.setAllowCredentials(true);
    //        config.addAllowedMethod("*");
    //        config.addAllowedHeader("*");
    //        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
    //        configSource.registerCorsConfiguration("/**", config);
    //        return new CorsFilter(configSource);
    //    }

    //    @Override
    //    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    //        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
    //        registry.addResourceHandler("/webjars/**"). addResourceLocations("classpath:/META-INF/resources/webjars/");
    //        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    //    }
}

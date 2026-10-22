package com.simple.common.core.common.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.EnableSpringUtil;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Created by IntelliJ IDEA
 * 跨域配置
 * <p>安全说明：跨域来源白名单经 {@code simple.cors.allowed-origins}（逗号分隔）显式配置后才允许携带凭证；
 * 未配置白名单时默认放开全部来源但禁止携带凭证，杜绝任意第三方站点以用户身份发起跨域请求。</p>
 *
 * @author qty
 */
@Configuration
@EnableSpringUtil
@ComponentScan(basePackages = { "com.simple.common.core" })
public class DefaultWebMvcConfigurer implements WebMvcConfigurer {

    /**
     * 允许的HTTP方法（原ORIGINS命名易与"来源"混淆，已按语义更名并收为私有）
     */
    private static final String[] ALLOWED_METHODS = new String[] { "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS" };

    /**
     * 跨域来源白名单，逗号分隔；为空时表示未配置白名单
     */
    @Setter
    @Value("${simple.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var registration = registry.addMapping("/**") //指定路径
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowedMethods(ALLOWED_METHODS) //方法
                .maxAge(3600); //设置预检请求的缓存时间（秒），在这个时间段里，对于相同的跨域请求不会再预检了
        if (StrUtil.isBlank(allowedOrigins)) {
            // 未配置白名单：默认全部来源可访问，但禁止携带凭证
            registration.allowedOriginPatterns("*").allowCredentials(false);
        } else {
            // 显式白名单：仅白名单来源可访问，才允许携带凭证
            List<String> origins = StrUtil.splitTrim(allowedOrigins, ',');
            registration.allowedOrigins(origins.toArray(new String[0])).allowCredentials(true);
        }
    }
}

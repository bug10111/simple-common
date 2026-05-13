package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.core.common.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 客户端权限配置抽象基类。
 * <p>
 * 用于配置客户端的权限信息，包括白名单路径、URL权限映射等。
 * 业务系统需要继承此类并实现 {@link #configure(ClientAuthInfo)} 方法来定义
 * 自定义的权限配置。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Configuration
 * public class MyAuthConfig extends AbsClientAuthConfig {
 *     @Override
 *     protected void configure(ClientAuthInfo clientAuthInfo) {
 *         // 配置白名单路径（无需鉴权）
 *         clientAuthInfo.addWhitePath("/api/public/**");
 *         clientAuthInfo.addWhitePath("/api/login");
 *
 *         // 配置URL权限映射
 *         clientAuthInfo.addUrlOperation("/api/admin/**", "admin");
 *         clientAuthInfo.addUrlOperation("/api/user/**", "user");
 *     }
 * }
 * }</pre>
 *
 * <h3>配置加载时机：</h3>
 * <p>
 * 配置在应用启动时通过 {@link com.simple.common.auth.client.init.AuthInit} 初始化类加载。
 * </p>
 *
 * @author qty
 */
public abstract class AbsClientAuthConfig {

    @Value("${spring.profiles.active}")
    private String produce;

    @Autowired
    private ApplicationProperties applicationProperties;


    @Bean
    public ClientAuthInfo configure() {
        ClientAuthInfo clientAuthInfo = new ClientAuthInfo();
        configure(clientAuthInfo);
        clientAuthInfo.setDocument(produce);
        clientAuthInfo.addScope("ALL",applicationProperties.getName());

        if (!produce.equals("produce")) {
            clientAuthInfo
                            //放开接口文档
                            .antMatchers("/webjars/**").permitAll()
                            .antMatchers("/v3/**").permitAll()
                            .antMatchers("/favicon.ico") .permitAll()
                            .antMatchers("/doc.html").permitAll()

                            //放开admin监控
                            .antMatchers("/actuator/**").permitAll();
        }
        return clientAuthInfo;
    }

    /**
     * 客户端权限配置
     *
     * @param clientAuthInfo 客户端权限信息
     */
    protected abstract void configure(ClientAuthInfo clientAuthInfo);

}
package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.core.common.properties.ApplicationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Created with IntelliJ IDEA
 * 客户端权限配置基类
 *
 * @author qty
 */
public abstract class AbsClientAuthConfig {

    @Value("${spring.profiles.active}")
    private String produce;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private AbsClientAuthConfig absClientAuthConfig;

    @Bean
    public ClientAuthInfo configure() {
        ClientAuthInfo clientAuthInfo = new ClientAuthInfo();
        absClientAuthConfig.configure(clientAuthInfo);
        clientAuthInfo.setDocument(produce);
        clientAuthInfo.addScope("all",applicationProperties.getName());

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

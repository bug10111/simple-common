package com.simple.oauth.common.config;

import com.simple.common.auth.client.common.config.AbsClientAuthConfig;
import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.oauth.common.properties.OauthProperties;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Component
public class OauthConfig extends AbsClientAuthConfig {

    @Autowired
    private OauthProperties oauthProperties;

    @Override
    @SneakyThrows
    protected void configure(ClientAuthInfo clientAuthInfo) {
        clientAuthInfo.openLogin()
                      .openAuthentication()
                      .openIPWhitelist()
                      .antMatchers("/").permitAll()
                      .antMatchers("/index.html").permitAll()
                      .antMatchers("/src/**").permitAll()
                      .antMatchers("/config/**").permitAll()
                      .antMatchers("/auth/key/**").permitAll()
                      .antMatchers("/favicon.ico").permitAll()

                      .antMatchers("/auth/login").permitAll()
                      .antMatchers("/auth/wx-login").permitAll()
                      .antMatchers("/auth/refresh").permitAll()
                      .antMatchers("/auth/getTime").permitAll()
                      .antMatchers("/api/user/name/*").permitAll()
                      .antMatchers("/api/client/list/*").permitAll()
//                      .antMatchers("/app/sys-annexs/by-id/*").permitAll()
                      .antMatchers("/app/sys-dict-types/*").permitAll()

                      .antMatchers("/app/sys-advertisements/**").permitAll()

                      //用户信息
                      .antMatchers("/api/**").roleByRestricted(getRoleByRestricted())

                      //开放接口
                      .antMatchers("/api/user/**").onlyIp("127.0.0.1", "192.168.101.66")
                      .antMatchers("/api/user/name/*").onlyIp("127.0.0.1", "192.168.101.66")
                      .antMatchers("/api/client/list/*").onlyIp("127.0.0.1", "192.168.101.66")
        ;
    }

    /**
     * 获取允许公共接口的角色
     */
    protected String[] getRoleByRestricted(){
        return new String[]{oauthProperties.getTourists(),"property", "merchant"};
    }
}

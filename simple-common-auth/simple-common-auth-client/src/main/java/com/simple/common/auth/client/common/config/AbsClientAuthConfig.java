package com.simple.common.auth.client.common.config;

import com.simple.common.auth.client.common.entity.auth.ClientAuthInfo;
import com.simple.common.core.common.properties.ApplicationProperties;
import com.simple.common.core.utils.AssertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

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

    /**
     * 生产环境 profile 标识；兼容旧配置读法，缺省不报错（实际判定走 {@link Environment}）。
     */
    @Value("${spring.profiles.active:}")
    private String produce;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private Environment environment;

    @Bean
    public ClientAuthInfo clientAuthInfo() {
        ClientAuthInfo clientAuthInfo = new ClientAuthInfo();
        configure(clientAuthInfo);
        clientAuthInfo.setProduce(produce);
        // spring.application.name 未配置时 fail-fast，避免 ALL scope 落入 null 值污染授权数据
        String applicationName = applicationProperties.getName();
        AssertUtils.notEmpty(applicationName, "spring.application.name 未配置，客户端权限 ALL scope 无法标识当前服务");
        clientAuthInfo.addScope("ALL", applicationName);

        if (!isProduce()) {
            clientAuthInfo
                            //放开接口文档
                            .antMatchers("/webjars/**").permitAll()
                            .antMatchers("/v3/**").permitAll()
                            .antMatchers("/favicon.ico").permitAll()
                            .antMatchers("/doc.html").permitAll()

                            //放开admin监控
                            .antMatchers("/actuator/**").permitAll();
        }

        //实例同步接收端点：内网端点不鉴权（设计裁定），接收中心投递引擎变更载荷；部署侧由 nginx 不代理清单隔离外网。
        //纵深防御：InternalSyncController 支持可选共享令牌校验（simple.auth.internal-sync-token，配置即强制校验）。
        clientAuthInfo.antMatchers("/internal/sync/**").permitAll();
        return clientAuthInfo;
    }

    /**
     * 判定当前是否生产环境。
     * <p>
     * 通过 {@link Environment#acceptsProfiles(Profiles)} 判定，兼容多 profile
     * （如 spring.profiles.active=prod,cloud）与未配置 profile 的场景，避免字符串等值比较漏判。
     * </p>
     *
     * @return true 表示当前激活 profile 含 produce
     */
    protected boolean isProduce() {
        return environment.acceptsProfiles(Profiles.of("produce"));
    }

    /**
     * 客户端权限配置
     *
     * @param clientAuthInfo 客户端权限信息
     */
    protected abstract void configure(ClientAuthInfo clientAuthInfo);

}
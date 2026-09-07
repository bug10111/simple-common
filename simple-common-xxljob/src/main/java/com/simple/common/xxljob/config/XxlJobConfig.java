package com.simple.common.xxljob.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by IntelliJ IDEA on 2023/11/7 15:01
 *
 * @author qty
 */
@Data
@Configuration
@ComponentScan(basePackages = { "com.simple.common.xxljob" })
@ConfigurationProperties(prefix = "xxl.job")
@ConditionalOnProperty(prefix = "xxl.job", name = "open", havingValue = "true")
public class XxlJobConfig {

    //是否开启xxl-job
    private String open;

    //地址
    private String adminAddresses;

    //token
    private String accessToken;

    //Admin登录账号，缺省空=不启用登录态鉴权；官方Admin的/jobinfo/*走登录cookie校验，配置账号密码后动态管理请求会先登录Admin取cookie
    private String adminUsername;

    //Admin登录密码，缺省空=不启用登录态鉴权；与adminUsername必须同时配置
    private String adminPassword;

    //执行器名称
    private String executorAppname;

    //执行器端口，未配置时默认9999
    private int executorPort = 9999;

    //日志保存地址
    private String executorLogPath;

    //日志保存天数，未配置时默认30天
    private int executorLogRetentionDays = 30;

    //Admin远程调用超时时间，单位秒，未配置时默认10秒
    private int requestTimeout = 10;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(executorAppname);
        xxlJobSpringExecutor.setPort(executorPort);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(executorLogPath);
        xxlJobSpringExecutor.setLogRetentionDays(executorLogRetentionDays);
        return xxlJobSpringExecutor;
    }

    /**
     * 针对多网卡、容器内部署等情况，可借助 "spring-cloud-commons" 提供的 "InetUtils" 组件灵活定制注册IP；
     *
     *      1、引入依赖：
     *          <dependency>
     *             <groupId>org.springframework.cloud</groupId>
     *             <artifactId>spring-cloud-commons</artifactId>
     *             <version>${version}</version>
     *         </dependency>
     *
     *      2、配置文件，或者容器启动变量
     *          spring.cloud.inetutils.preferred-networks: 'xxx.xxx.xxx.'
     *
     *      3、获取IP
     *          String ip_ = inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
     */

}

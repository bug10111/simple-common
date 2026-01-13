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

    //执行器名称
    private String executorAppname;

    //端口
    private int executorPort;

    //日志保存地址
    private String executorLogPath;

    //日志保存时间
    private int executorLogRetentionDays;

    //手动添加的请求超时时间
    private int requestTimeout;

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

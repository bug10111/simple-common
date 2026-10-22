package com.simple.common.xxljob.config;

import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import com.simple.common.xxljob.common.manager.XxlJobManager;
import com.simple.common.xxljob.common.service.XxlJobService;
import com.simple.common.xxljob.manager.DefaultXxlJobManager;
import com.simple.common.xxljob.service.DefaultXxlJobService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;

/**
 * Created by IntelliJ IDEA on 2023/11/7 15:01
 * <p>
 * xxl-job 配置与执行器装配。
 * 说明：本自动配置以显式 {@code @Bean} 注册库内组件（不再包级 {@code @ComponentScan}），
 * {@code xxl.job.open} 非 true 时整组组件（执行器/管理器/服务）均不装配，
 * 且宿主工程误扫本包也不会拉起半套组件导致配置缺失报错。
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@ToString(exclude = {"accessToken", "adminPassword"})
@Validated
@Configuration
@ConfigurationProperties(prefix = "xxl.job")
@ConditionalOnProperty(prefix = "xxl.job", name = "open", havingValue = "true")
public class XxlJobConfig {

    //Admin 地址；执行器注册与任务管理通道都强依赖，留空在启动期快速失败
    private String adminAddresses;

    //token
    private String accessToken;

    //Admin 登录账号，缺省空=不启用登录态鉴权；官方 Admin 的 /jobinfo/* 走登录 cookie 校验，配置账号密码后动态管理请求会先登录 Admin 取 cookie
    private String adminUsername;

    //Admin 登录密码，缺省空=不启用登录态鉴权；与 adminUsername 必须同时配置（见 {@link #isAdminCredentialValid}）
    private String adminPassword;

    //执行器名称
    private String executorAppname;

    //执行器端口，未配置时默认9999
    private int executorPort = 9999;

    //日志保存地址
    private String executorLogPath;

    //日志保存天数，未配置时默认30天
    private int executorLogRetentionDays = 30;

    //Admin 远程调用超时时间，单位秒，未配置时默认10秒
    private int requestTimeout = 10;

    /**
     * 校验 Admin 登录账号与密码必须同时配置
     * <p>
     * 只配其一会被判定为「未启用登录态」而静默关闭登录态鉴权层：
     * 管理通道对开启登录校验的 Admin 全部失败，或请求以非预期凭据发出；
     * 在绑定阶段快速失败给出可读提示。
     * </p>
     *
     * @return 配置合法时返回true
     */
    @AssertTrue(message = "xxl.job.admin-username 与 xxl.job.admin-password 必须同时配置")
    public boolean isAdminCredentialValid() {
        return StrUtil.isBlank(adminUsername) == StrUtil.isBlank(adminPassword);
    }

    /**
     * 任务管理器：Admin 通道的任务动态管理
     */
    @Bean
    @ConditionalOnMissingBean(XxlJobManager.class)
    public XxlJobManager xxlJobManager() {
        return new DefaultXxlJobManager(this);
    }

    /**
     * 任务管理服务：面向业务的门面
     */
    @Bean
    @ConditionalOnMissingBean(XxlJobService.class)
    public XxlJobService xxlJobService(XxlJobManager xxlJobManager) {
        return new DefaultXxlJobService(xxlJobManager);
    }

    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        // 执行器注册强依赖项快速失败：adminAddresses 为空白时执行器会空注册（调度静默不工作）、
        // executorAppname 为空白时注册不可用，都要在启动期暴露而不是运行期静默失效
        Assert.hasText(adminAddresses, "xxl.job.admin-addresses 不能为空");
        Assert.hasText(executorAppname, "xxl.job.executor-appname 不能为空");

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

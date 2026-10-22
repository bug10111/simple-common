package com.simple.common.annex.common.config;

import com.amazonaws.services.s3.AmazonS3;
import com.simple.common.annex.common.properties.AnnexProperties;
import com.simple.common.annex.common.service.AnnexService;
import com.simple.common.annex.common.manager.S3Manager;
import com.simple.common.annex.manager.DefaultS3Handler;
import com.simple.common.annex.service.S3AnnexService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Created with IntelliJ IDEA
 * Description: 附件模块装配配置。
 * <p>
 * 以自动配置方式装配(不再整包组件扫描)：
 * <ul>
 *   <li>未引入 AWS S3 SDK 时整个附件能力不装配,避免宿主应用因缺少 SDK 类而启动失败;</li>
 *   <li>业务方注册自定义 S3Manager / AnnexService / AnnexProperties 后,对应默认实现自动让位,
 *       避免同类型双候选导致 NoUniqueBeanDefinitionException;</li>
 *   <li>配置属性以 simple.annex 前缀绑定,必填项在装配阶段即校验,配置缺失启动即失败。</li>
 * </ul>
 *
 * @author qty
 */
@AutoConfiguration
@ConditionalOnClass(AmazonS3.class)
public class AnnexConfig {

    /**
     * 附件配置属性装配(容器已有 AnnexProperties 类型 Bean 时让位)
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "simple.annex")
    public AnnexProperties annexProperties() {
        return new AnnexProperties();
    }

    /**
     * S3 协议默认实现装配(容器已有 S3Manager 类型 Bean 时让位)
     */
    @Bean
    @ConditionalOnMissingBean(S3Manager.class)
    public S3Manager defaultS3Handler() {
        return new DefaultS3Handler();
    }

    /**
     * 附件服务默认实现装配(容器已有 AnnexService 类型 Bean 时让位)
     */
    @Bean
    @ConditionalOnMissingBean(AnnexService.class)
    public AnnexService s3AnnexService() {
        return new S3AnnexService();
    }
}

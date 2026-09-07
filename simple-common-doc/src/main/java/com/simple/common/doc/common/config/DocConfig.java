package com.simple.common.doc.common.config;

import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import com.simple.common.doc.manager.PoiTlTemplateReplaceManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created with IntelliJ IDEA
 * Description: 文档模块装配配置。
 * 默认模板替换引擎 PoiTlTemplateReplaceManager 以
 * {@code @ConditionalOnMissingBean(DocTemplateReplaceManager.class)} 方式装配：
 * 业务方注册自定义 DocTemplateReplaceManager 实现后，默认引擎自动让位，
 * 避免与业务实现形成同类型双候选导致启动失败。
 *
 * @author qty
 */
@Configuration
@ComponentScan(basePackages = { "com.simple.common.doc" })
public class DocConfig {

    /**
     * 默认文档模板替换引擎装配
     * <p>
     * 仅在容器中不存在其它 DocTemplateReplaceManager 实现时装配，
     * 保证 DefaultDocReplaceService 按类型注入时始终只有单一候选。
     * </p>
     *
     * @return 默认的 poi-tl 模板替换引擎实例
     */
    @Bean
    @ConditionalOnMissingBean(DocTemplateReplaceManager.class)
    public DocTemplateReplaceManager poiTlTemplateReplaceManager() {
        return new PoiTlTemplateReplaceManager();
    }
}

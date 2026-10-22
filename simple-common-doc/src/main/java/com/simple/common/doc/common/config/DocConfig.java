package com.simple.common.doc.common.config;

import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import com.simple.common.doc.common.service.DocReplaceService;
import com.simple.common.doc.manager.PoiTlTemplateReplaceManager;
import com.simple.common.doc.service.DefaultDocReplaceService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Created with IntelliJ IDEA
 * Description: 文档模块装配配置。
 * <p>
 * 以自动配置方式装配(不再整包组件扫描)：
 * 业务方注册自定义 DocTemplateReplaceManager 或 DocReplaceService 后,
 * 对应默认实现自动让位,避免与业务实现形成同类型双候选导致启动失败。
 * </p>
 *
 * @author qty
 */
@AutoConfiguration
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

    /**
     * 默认文档替换服务装配
     * <p>
     * 仅在容器中不存在其它 DocReplaceService 实现时装配,业务自定义实现自动让位。
     * </p>
     *
     * @param docTemplateReplaceManager 容器内唯一的模板替换引擎
     * @return 默认文档替换服务实例
     */
    @Bean
    @ConditionalOnMissingBean(DocReplaceService.class)
    public DocReplaceService defaultDocReplaceService(DocTemplateReplaceManager docTemplateReplaceManager) {
        return new DefaultDocReplaceService(docTemplateReplaceManager);
    }
}

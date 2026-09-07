package com.simple.common.doc.manager;

import com.deepoove.poi.XWPFTemplate;
import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: 基于 poi-tl 引擎的文档模板替换默认实现。
 * 由 {@link com.simple.common.doc.common.config.DocConfig} 以
 * {@code @ConditionalOnMissingBean(DocTemplateReplaceManager.class)} 方式装配：
 * 容器中注册了自定义 DocTemplateReplaceManager 实现时，本默认实现自动让位不再装配。
 * 参考：https://blog.csdn.net/weixin_44496396/article/details/140066940
 * 官网：https://deepoove.com/poi-tl/
 *
 * @author qty
 */
@Slf4j
public class PoiTlTemplateReplaceManager implements DocTemplateReplaceManager {

    @Override
    @SneakyThrows
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        XWPFTemplate template = XWPFTemplate.compile(inputStream).render(values);
        template.writeAndClose(outputStream);
    }
}

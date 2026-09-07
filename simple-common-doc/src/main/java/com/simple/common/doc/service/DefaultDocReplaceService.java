package com.simple.common.doc.service;

import com.simple.common.doc.common.function.DocFunction;
import com.simple.common.doc.common.manager.DocTemplateReplaceManager;
import com.simple.common.doc.common.service.DocReplaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 文档替换服务默认实现。
 * <p>
 * 委托容器中唯一的 {@link DocTemplateReplaceManager} 完成模板替换；
 * 引擎装配规则见 {@link com.simple.common.doc.common.config.DocConfig}。
 * </p>
 *
 * @author qty
 */
@Service
public class DefaultDocReplaceService implements DocReplaceService {

    /**
     * 文档模板替换引擎,容器内按类型唯一
     */
    @Autowired
    private DocTemplateReplaceManager docTemplateReplaceManager;

    @Override
    public void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values) {
        docTemplateReplaceManager.replace(inputStream, outputStream, values);
    }
}

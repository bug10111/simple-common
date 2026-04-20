package com.simple.common.doc.common.manager;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * 文档模板替换管理器接口。
 * <p>
 * 用于处理Word文档模板的变量替换，支持基于模板生成定制化文档。
 * 默认实现 {@link com.simple.common.doc.manager.PoiTlTemplateReplaceManager} 基于
 * poi-tl模板引擎实现文档模板替换功能。
 * </p>
 *
 * @author qty
 */
public interface DocTemplateReplaceManager {

    /**
     * 替换doc文档参数,会关闭文件流
     *
     * @param inputStream  模板文件流
     * @param outputStream 输出流
     * @param values       参数值
     */
    void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);


}
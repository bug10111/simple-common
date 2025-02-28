package com.simple.common.doc.common.manager;

import com.simple.common.doc.common.function.DocFunction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: doc文档参数替换
 *
 * @author 兄台丶请冷静
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

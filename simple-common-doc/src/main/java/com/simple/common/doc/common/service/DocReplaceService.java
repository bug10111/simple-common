package com.simple.common.doc.common.service;

import com.simple.common.core.utils.FileUtils;
import com.simple.common.core.utils.ResponseUtils;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Description: doc文档占位符替换
 *
 * @author qty
 */
public interface DocReplaceService {

    /**
     * 替换doc文档参数,会关闭文件流
     *
     * @param inputStream  模板文件流
     * @param outputStream 输出流
     * @param values       参数值
     */
    void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);

    /**
     * 替换doc文档参数，获取输入流
     *
     * @param name        文件名称
     * @param inputStream 模板文件流
     * @param values      参数值
     */
    @SneakyThrows
    default void replaceResponse(String name, InputStream inputStream, Map<String, Object> values) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        replace(inputStream, byteArrayOutputStream, values);
        ResponseUtils.writeResponse(name + ".docx", byteArrayOutputStream);
    }

    /**
     * 替换doc文档参数，获取输入流
     *
     * @param name         文件名称
     * @param templatePath 模板文件地址，这里是获取resources下的文件
     * @param values       参数值
     */
    @SneakyThrows
    default void replaceResponse(String name, String templatePath, Map<String, Object> values) {
        ByteArrayOutputStream byteArrayOutputStream = replaceAndGetOutputStream(templatePath, values);
        ResponseUtils.writeResponse(name + ".docx", byteArrayOutputStream);
    }

    /**
     * 替换doc文档参数，获取输入流
     *
     * @param templatePath 模板文件地址，这里是获取resources下的文件
     * @param values       参数值
     */
    @SneakyThrows
    default ByteArrayInputStream replaceAndGetInputStream(String templatePath, Map<String, Object> values) {
        ByteArrayOutputStream byteArrayOutputStream = replaceAndGetOutputStream(templatePath, values);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        byteArrayInputStream.close();
        return byteArrayInputStream;
    }

    /**
     * 替换doc文档参数，获取输出流
     *
     * @param templatePath 模板文件地址，这里是获取resources下的文件
     * @param values       参数值
     */
    default ByteArrayOutputStream replaceAndGetOutputStream(String templatePath, Map<String, Object> values) {
        InputStream inputStream = FileUtils.getResourcesFileInputStream(templatePath);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        replace(inputStream, byteArrayOutputStream, values);
        return byteArrayOutputStream;
    }

}

package com.simple.common.excel.common.service;

import com.alibaba.excel.read.listener.ReadListener;
import lombok.SneakyThrows;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 * Description: easyexcel 读取
 *
 * @author qty
 */
public interface EasyExcelReadService {

    /**
     * 根据文件URL读取excel
     *
     * @param filePath      文件路径
     * @param headRowNumber 表头行数
     * @param head          基础数据类，属性排序需要和excel一致,不创建对象的话传入Map<Integer, String>
     * @param readListener  监听类
     */
    <T> void read(String filePath, int headRowNumber, Class<T> head, ReadListener<T> readListener);

    /**
     * 根据输入流读取excel
     *
     * @param inputStream   输入流
     * @param headRowNumber 表头行数
     * @param head          基础数据类，属性排序需要和excel一致,不创建对象的话传入Map<Integer, String>
     * @param readListener  监听类
     */
    <T> void read(InputStream inputStream, int headRowNumber, Class<T> head, ReadListener<T> readListener);

    /**
     * 根据上传文件对象读取excel
     *
     * @param file          上传的文件对象
     * @param headRowNumber 表头行数，有两行的话 就意味着从第三行开始读取数据
     * @param head          基础数据类，属性排序需要和excel一致,不创建对象的话传入Map<Integer, String>
     * @param readListener  监听类
     */
    @SneakyThrows
    default <T> void read(MultipartFile file, int headRowNumber, Class<T> head, ReadListener<T> readListener) {
        read(file.getInputStream(), headRowNumber, head, readListener);
    }
}

package com.simple.common.excel.common.service;

import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: easyexcel 写入接口
 *
 * @author qty
 */
public interface EasyExcelWriteService {

    /**
     * 将数据写入输出流
     *
     * @param outputStream 目标输出流
     * @param clazz        数据对象class
     * @param data         数据集合
     * @param <T>          数据对象
     */
    <T> ByteArrayOutputStream writeOutputStream(ByteArrayOutputStream outputStream, Class<T> clazz, List<T> data);

    /**
     * 将数据写入response
     *
     * @param clazz     数据对象class
     * @param data      数据集合
     * @param writeName 导出文件名称（不带上格式）
     * @param <T>       数据对象
     */
    <T> void writeResponse(Class<T> clazz, List<T> data, String writeName);

    /**
     * 将数据写入输出流
     *
     * @param <T>   数据对象
     * @param clazz 数据对象class
     * @param data  数据集合
     */
    default <T> ByteArrayOutputStream writeOutputStream(Class<T> clazz, List<T> data) {
        return writeOutputStream(new ByteArrayOutputStream(), clazz, data);
    }

    /**
     * 将数据写入输入流
     *
     * @param clazz 数据对象class
     * @param data  数据集合
     * @param <T>   数据对象
     */
    @SneakyThrows
    default <T> ByteArrayInputStream writeInputStream(Class<T> clazz, List<T> data) {
        ByteArrayOutputStream write = writeOutputStream(clazz, data);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(write.toByteArray());
        byteArrayInputStream.close();
        return byteArrayInputStream;
    }
}

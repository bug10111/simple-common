package com.simple.common.excel.common.service;

import com.simple.common.excel.common.function.PoiExportFunction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description:Poi xlsx excel导出
 *
 * @author qty
 */
public interface PoiWriteService {

    /**
     * 将数据写入输出流
     *
     * @param function     数据填充
     * @param list         数据集合
     * @param head         导出列的名字
     * @param width        列宽
     * @param num          Excel每一页多少数据（不要超过1048576）
     * @param outputStream 需要写入的输出流 输入完毕后会自动关闭
     */
    <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num,
                                                ByteArrayOutputStream outputStream);

    /**
     * 导出Excel文档到response
     *
     * @param function  数据填充
     * @param list      数据集合
     * @param head      导出列的名字
     * @param width     列宽
     * @param num       Excel每一页多少数据（不要超过1048576）
     * @param excelName 导出Excel的名字（不带上格式）
     */
    <T> void exportResponse(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num, String excelName);

    /**
     * 将数据写入输入流
     *
     * @param function 数据填充
     * @param list     数据集合
     * @param head     导出列的名字
     * @param width    列宽
     * @param num      Excel每一页多少数据（不要超过1048576）
     */
    default <T> ByteArrayInputStream writeInputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num) {
        ByteArrayOutputStream export = writeOutputStream(function, list, head, width, num);
        return new ByteArrayInputStream(export.toByteArray());
    }

    /**
     * 将数据写入输出流
     *
     * @param function 数据填充
     * @param list     数据集合
     * @param head     导出列的名字
     * @param width    列宽
     * @param num      Excel每一页多少数据（不要超过1048576）
     */
    default <T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list, String[] head, Integer[] width, Integer num) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        return writeOutputStream(function, list, head, width, num, outputStream);
    }

}

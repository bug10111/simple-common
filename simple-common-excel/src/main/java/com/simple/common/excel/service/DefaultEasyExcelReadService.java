package com.simple.common.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.listener.ReadListener;
import com.simple.common.excel.common.service.EasyExcelReadService;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 * Description: EasyExcel 读取服务默认实现。
 * <p>
 * 流所有权说明：EasyExcel 默认 autoCloseStream=true，本实现读取完成后会关闭传入的文件路径/输入流，
 * 调用方不应在读取后复用同一流。如需自行管理流生命周期，请自行包装后实现本接口。
 * </p>
 *
 * @author qty
 */
@Service
public class DefaultEasyExcelReadService implements EasyExcelReadService {

    @Override
    public <T> void read(String filePath, int headRowNumber, Class<T> head, ReadListener<T> readListener) {
        // 文件由 EasyExcel 读取后自动关闭
        doRead(EasyExcel.read(filePath, head, readListener), headRowNumber);
    }

    @Override
    public <T> void read(InputStream inputStream, int headRowNumber, Class<T> head, ReadListener<T> readListener) {
        // 输入流由 EasyExcel 读取后自动关闭(autoCloseStream 默认 true)
        doRead(EasyExcel.read(inputStream, head, readListener), headRowNumber);
    }

    /**
     * 公共读取流程(两个 read 重载共用)，避免表头行数等公共逻辑双份维护
     *
     * @param readerBuilder EasyExcel 读取构建器
     * @param headRowNumber 表头行数(必须为正数；当前实现固定读取第一个 sheet)
     */
    private void doRead(ExcelReaderBuilder readerBuilder, int headRowNumber) {
        // 这里可以设置1，因为头就是一行。如果多行头，可以设置其他值。不传入也可以，因为默认会根据实体类解析，默认1行
        readerBuilder.sheet().headRowNumber(headRowNumber).doRead();
    }
}

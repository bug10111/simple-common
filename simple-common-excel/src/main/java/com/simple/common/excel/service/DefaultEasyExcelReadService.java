package com.simple.common.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.ReadListener;
import com.simple.common.excel.common.service.EasyExcelReadService;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultEasyExcelReadService implements EasyExcelReadService {

    @Override
    public <T> void read(String filePath, int headRowNumber, Class<T> head, ReadListener<T> readListener) {

        //文件流会自动关闭
        EasyExcel.read(filePath, head, readListener).sheet()
                 // 这里可以设置1，因为头就是一行。如果多行头，可以设置其他值。不传入也可以，因为默认会根据DemoData 来解析，他没有指定头，也就是默认1行
                 .headRowNumber(headRowNumber).doRead();
    }

    @Override
    public <T> void read(InputStream inputStream, int headRowNumber, Class<T> head, ReadListener<T> readListener) {
        //文件流会自动关闭
        EasyExcel.read(inputStream, head, readListener).sheet()
                 // 这里可以设置1，因为头就是一行。如果多行头，可以设置其他值。不传入也可以，因为默认会根据DemoData 来解析，他没有指定头，也就是默认1行
                 .headRowNumber(headRowNumber).doRead();
    }
}

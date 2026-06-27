package com.simple.common.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.excel.common.service.EasyExcelWriteService;
import com.simple.common.excel.common.service.WriteContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Service
public class DefaultEasyExcelWriteService implements EasyExcelWriteService {

    @Override
    @SneakyThrows
    public <T> ByteArrayOutputStream writeOutputStream(ByteArrayOutputStream outputStream, Class<T> clazz, List<T> data) {
        EasyExcel.write(outputStream, clazz).sheet().doWrite(data);
        outputStream.close();
        return outputStream;
    }

    @SneakyThrows
    @Override
    public <T> void writeResponse(Class<T> clazz, List<T> data, String writeName) {
        HttpServletResponse response = HttpServletUtils.getResponse();
        HttpServletRequest request = HttpServletUtils.getRequest();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 这里URLEncoder.encode可以防止中文乱码
        String fileName = URLEncoder.encode(writeName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=" + fileName + ".xlsx");

        String originalURL = request.getHeader("Origin");
        if (originalURL != null) {
            response.addHeader("Access-Control-Allow-Origin", originalURL);
        }

        writeOutputStream(clazz, data).writeTo(response.getOutputStream());
    }

    @Override
    public <T> WriteContext<T> createWriter(OutputStream outputStream, Class<T> clazz, String sheetName) {

        // 创建 EasyExcel 写入器
        ExcelWriter excelWriter = EasyExcel.write(outputStream, clazz).build();

        // 创建 Sheet 配置
        WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();

        return new WriteContext<>(excelWriter, writeSheet);
    }
}
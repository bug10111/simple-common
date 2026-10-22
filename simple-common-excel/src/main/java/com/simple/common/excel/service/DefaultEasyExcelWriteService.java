package com.simple.common.excel.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.simple.common.core.utils.HttpServletUtils;
import com.simple.common.excel.common.service.EasyExcelWriteService;
import com.simple.common.excel.common.service.WriteContext;
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
        // 参数校验：目标输出流必须提供；本实现写完后会关闭该流(ByteArrayOutputStream 的 close 为空操作,不影响后续 toByteArray)
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream(目标输出流)不能为空");
        }
        EasyExcel.write(outputStream, clazz).sheet().doWrite(data);
        outputStream.close();
        return outputStream;
    }

    @SneakyThrows
    @Override
    public <T> void writeResponse(Class<T> clazz, List<T> data, String writeName) {
        HttpServletResponse response = HttpServletUtils.getResponse();
        if (response == null) {
            throw new IllegalStateException("当前线程不存在 Servlet 请求上下文，无法写入下载响应");
        }
        if (writeName == null || writeName.isBlank()) {
            throw new IllegalArgumentException("writeName(下载文件名)不能为空");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 文件名按 UTF-8 URL 编码并将空格替换为 %20,下载头带 UTF-8'' 字符集前缀严格符合 RFC 5987,防止中文文件名乱码
        String fileName = URLEncoder.encode(writeName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=UTF-8''" + fileName + ".xlsx");

        // 直接写入响应流,避免先在内存组装完整工作簿再二次拷贝导致大导出内存峰值翻倍;
        // 跨域由网关或应用统一的 CORS 配置管理,不再反射请求 Origin 头(防止任意 Origin 反射绕过同源策略)
        EasyExcel.write(response.getOutputStream(), clazz).sheet().doWrite(data);
        response.flushBuffer();
    }

    @Override
    public <T> WriteContext<T> createWriter(OutputStream outputStream, Class<T> clazz, String sheetName) {

        // 创建参数校验
        if (outputStream == null) {
            throw new IllegalArgumentException("outputStream(目标输出流)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz(数据类型)不能为空");
        }

        // 创建 EasyExcel 写入器
        ExcelWriter excelWriter = EasyExcel.write(outputStream, clazz).build();

        // 创建 Sheet 配置
        WriteSheet writeSheet = EasyExcel.writerSheet(sheetName).build();

        return new WriteContext<>(excelWriter, writeSheet);
    }
}

package com.simple.common.core.utils;

import cn.hutool.core.util.ZipUtil;
import com.simple.common.core.function.ZipWriteFunction;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA
 * Description: 压缩文件操作
 *
 * @author qty
 */
public class ZipUtils extends ZipUtil {

    /**
     * 将文zip放入response
     *
     * @param fileName zip名称
     */
    @SneakyThrows
    public static void downloadZip(String fileName, ZipWriteFunction function)  {
        HttpServletResponse response = HttpServletUtils.getResponse();

        // 使用 RFC 5987 编码文件名
        String encodedFilename = URLEncoder.encode(fileName + ".zip", StandardCharsets.UTF_8).replaceAll("\\+", "%20"); // 关键替换

        // 设置响应头（同时兼容新旧浏览器）
        response.setHeader("Content-Disposition", "attachment; filename=" + encodedFilename);

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            function.addExcelToZip(zipOut, fileName);
            zipOut.finish();
        }
    }

    /**
     * 写入文件，多个文件需要在ZipWriteFunction中循环调用
     *
     * @param zipOut   zip输出流
     * @param fileName 需要加入的文件名称
     * @param date     需要加入的文件字节
     */
    @SneakyThrows
    public static void write(ZipOutputStream zipOut, String fileName, byte[] date) {
        ZipEntry entry = new ZipEntry(fileName);
        zipOut.putNextEntry(entry);
        zipOut.write(date);
    }
}

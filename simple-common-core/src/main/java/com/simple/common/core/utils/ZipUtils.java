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

        // 文件名按 UTF-8 URL 编码并将空格替换为 %20,下载头带 UTF-8'' 字符集前缀严格符合 RFC 5987,防止中文文件名乱码
        String encodedFilename = URLEncoder.encode(fileName + ".zip", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFilename);

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

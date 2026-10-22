package com.simple.common.core.utils;

import cn.hutool.core.util.ZipUtil;
import com.simple.common.core.function.ZipWriteFunction;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
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

    private ZipUtils() {
    }

    /**
     * 将文zip放入response
     * <p>实现说明：先在内存缓冲区完成打包，成功后才写响应——避免回调中途异常时
     * 客户端收到已提交的200+截断zip；响应头补充Content-Type与Content-Length。</p>
     *
     * @param fileName zip名称
     */
    @SneakyThrows
    public static void downloadZip(String fileName, ZipWriteFunction function) {
        HttpServletResponse response = HttpServletUtils.getResponse();

        // 文件名按 UTF-8 URL 编码并将空格替换为 %20,下载头带 UTF-8'' 字符集前缀严格符合 RFC 5987,防止中文文件名乱码
        String encodedFilename = URLEncoder.encode(fileName + ".zip", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFilename);
        response.setContentType("application/zip");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOut = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            function.addToZip(zipOut, fileName);
            zipOut.finish();
        } catch (IOException | RuntimeException e) {
            // 响应未提交时改写为错误响应，避免客户端拿到截断的zip文件
            if (!response.isCommitted()) {
                response.reset();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "压缩文件生成失败");
            }
            throw e;
        }

        response.setContentLengthLong(buffer.size());
        response.getOutputStream().write(buffer.toByteArray());
        response.getOutputStream().flush();
    }

    /**
     * 写入文件，多个文件需要在ZipWriteFunction中循环调用
     *
     * @param zipOut   zip输出流
     * @param fileName 需要加入的文件名称
     * @param data     需要加入的文件字节（原参数名date为笔误，已更正为data）
     */
    @SneakyThrows
    public static void write(ZipOutputStream zipOut, String fileName, byte[] data) {
        ZipEntry entry = new ZipEntry(fileName);
        zipOut.putNextEntry(entry);
        zipOut.write(data);
        zipOut.closeEntry();
    }
}

package com.simple.common.core.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created with IntelliJ IDEA
 * Description: 响应工具类
 *
 * @author qty
 */
@Slf4j
public class ResponseUtils {

    private ResponseUtils() {
    }

    /**
     * 发送文本。使用UTF-8编码。
     *
     * @param response response
     * @param text     发送的字符串
     */
    public static void renderText(HttpServletResponse response, String text) {
        render(response, "text/plain;charset=UTF-8", text);
    }

    /**
     * 发送json。使用UTF-8编码。
     *
     * @param response response
     * @param text     发送的字符串
     */
    public static void renderJson(HttpServletResponse response, String text) {
        render(response, "application/json;charset=UTF-8", text);
    }

    /**
     * 发送json。使用UTF-8编码。
     *
     * @param response response
     * @param object   发送的字符串
     */
    public static void renderJson(HttpServletResponse response, Object object) {
        String text = JsonUtils.toJsonStr(object);
        render(response, "application/json;charset=UTF-8", text);
    }

    /**
     * 发送内容。使用UTF-8编码。禁止缓存（鉴权/业务数据响应不允许被代理或浏览器缓存）。
     *
     * @param response    response
     * @param contentType 内容类型
     * @param text        发送的字符串（null按空串写出）
     */
    public static void render(HttpServletResponse response, String contentType, String text) {
        response.setContentType(contentType);
        // 敏感响应禁止缓存
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        try {
            PrintWriter writer = response.getWriter();
            // 复用同一writer；null按空串写出，避免PrintWriter.write(null) NPE
            writer.write(text == null ? "" : text);
            writer.flush();
        } catch (IOException e) {
            // IO失败带上下文抛出，不静默吞掉（响应可能已部分提交）
            log.error("响应写出失败, contentType={}", contentType, e);
            throw new IllegalStateException("响应写出失败", e);
        } catch (RuntimeException e) {
            // 已提交/已获取流的容器态异常带上下文抛出，不静默
            log.error("响应写出失败, contentType={}", contentType, e);
            throw e;
        }
    }

    /**
     * 返回流（写完即刷出，避免异步/手工分发场景下缓冲未刷出导致下载截断）
     *
     * @param name                  名称
     * @param byteArrayOutputStream 输出流
     */
    public static void writeResponse(String name, ByteArrayOutputStream byteArrayOutputStream) {
        if (byteArrayOutputStream == null) {
            throw new IllegalArgumentException("byteArrayOutputStream 不能为null");
        }
        HttpServletResponse response = HttpServletUtils.getResponse();
        response.setHeader("Content-Disposition", "attachment;filename=" + encodeFileName(name)
                + ";filename*=UTF-8''" + encodeFileName(name));

        try {
            OutputStream out = response.getOutputStream();
            byteArrayOutputStream.writeTo(out);
            out.flush();
        } catch (IOException e) {
            // 响应头已提交时失败只能记录并抛出，交由上层统一处理
            log.error("响应流写出失败, fileName={}", name, e);
            throw new IllegalStateException("响应流写出失败", e);
        }
    }

    /**
     * 返回流（由调用方自行写出并flush）
     *
     * @param name 名称
     * @return 响应输出流
     */
    public static OutputStream getResponseOutputStream(String name) throws IOException {
        HttpServletResponse response = HttpServletUtils.getResponse();
        response.setHeader("Content-Disposition", "attachment;filename=" + encodeFileName(name)
                + ";filename*=UTF-8''" + encodeFileName(name));
        return response.getOutputStream();
    }

    /**
     * 下载头文件名编码：UTF-8 百分号编码 + {@code *}→%2A，严格符合 RFC 5987/8187 attr-char
     * （URLEncoder 属 form 编码，不转义 {@code *}，会造成 filename* 不合规）
     */
    private static String encodeFileName(String name) {
        String base = name == null ? "" : name;
        return URLEncoder.encode(base, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A");
    }
}

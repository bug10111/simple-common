package com.simple.common.core.utils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
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
    @SneakyThrows
    public static void renderJson(HttpServletResponse response, Object object) {
        String text = JsonUtils.toJsonStr(object);
        render(response, "application/json;charset=UTF-8", text);
    }

    /**
     * 发送内容。使用UTF-8编码。
     *
     * @param response    response
     * @param contentType 内容类型
     * @param text        发送的字符串
     */
    @SneakyThrows
    public static void render(HttpServletResponse response, String contentType, String text) {

        response.setContentType(contentType);
        //        response.setStatus(HttpServletResponse.SC_OK);

        //        //禁止缓存
        //        response.setHeader("Pragma", "No-cache");
        //        response.setHeader("Cache-Control", "no-cache");
        //
        //        //立即过期
        //        response.setDateHeader("Expires", 0);
        //        response.setStatus(500);
        //
        //设置返回跨域
        //        HttpServletRequest request = HttpServletUtils.getRequest();
        //        String originalURL = request.getHeader("Origin");
        //        if (originalURL != null) {
        //            response.addHeader("Access-Control-Allow-Origin", originalURL);
        //        }
        response.getWriter().write(text);
        response.getWriter().flush();

    }

    /**
     * 返回流
     *
     * @param name                  名称
     * @param byteArrayOutputStream 输出流
     */
    @SneakyThrows
    public static void writeResponse(String name, ByteArrayOutputStream byteArrayOutputStream) {
        HttpServletResponse response = HttpServletUtils.getResponse();

        // 这里URLEncoder.encode可以防止中文乱码
        String fileName = URLEncoder.encode(name, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=" + fileName);

        byteArrayOutputStream.writeTo(response.getOutputStream());
    }

    /**
     * 返回流
     *
     * @param name 名称
     */
    @SneakyThrows
    public static OutputStream getResponseOutputStream(String name) {
        HttpServletResponse response = HttpServletUtils.getResponse();

        // 这里URLEncoder.encode可以防止中文乱码
        String fileName = URLEncoder.encode(name, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=" + fileName);

        return response.getOutputStream();
    }
}

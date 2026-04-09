package com.simple.common.logs.client.common.httpservletrequest;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 支持多种Content-Type的请求体缓存包装器
 * <p>
 * 支持的类型包括：
 * - application/json
 * - application/xml
 * - text/xml
 * - text/plain
 * - application/x-www-form-urlencoded
 * - multipart/form-data (仅缓存元数据，不缓存文件内容)
 * </p>
 *
 * @author qty
 */
@Slf4j
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    /**
     * 需要缓存请求体的Content-Type前缀列表
     */
    private static final String[] CACHEABLE_CONTENT_TYPES = {
            ContentType.JSON.getValue(),
            ContentType.XML.getValue(),
            "text/xml",
            "text/plain",
            ContentType.FORM_URLENCODED.getValue()
    };

    /**
     * 缓存的请求体内容
     */
    private final byte[] body;

    /**
     * 原始Content-Type
     */
    private final String originalContentType;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.originalContentType = request.getContentType();
        this.body = shouldCacheBody(originalContentType) ? StreamUtils.copyToByteArray(request.getInputStream()) : new byte[0];
    }

    /**
     * 判断是否需要缓存请求体
     *
     * @param contentType Content-Type
     * @return true-需要缓存，false-不需要缓存
     */
    private boolean shouldCacheBody(String contentType) {
        if (ObjUtil.isEmpty(contentType)) {
            return false;
        }

        String lowerContentType = contentType.toLowerCase();
        for (String cacheableType : CACHEABLE_CONTENT_TYPES) {
            if (lowerContentType.contains(cacheableType.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取缓存的请求体内容（字符串形式）
     *
     * @return 请求体字符串，如果没有缓存则返回空字符串
     */
    public String getCachedBody() {
        if (body == null || body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * 获取缓存的请求体内容（字节数组形式）
     *
     * @return 请求体字节数组
     */
    public byte[] getCachedBodyBytes() {
        return body != null ? body.clone() : new byte[0];
    }

    /**
     * 判断请求体是否已被缓存
     *
     * @return true-已缓存，false-未缓存
     */
    public boolean isBodyCached() {
        return body != null && body.length > 0;
    }

    /**
     * 获取原始Content-Type
     *
     * @return Content-Type字符串
     */
    public String getOriginalContentType() {
        return originalContentType;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(body);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final InputStream bodyStream;

        public CachedBodyServletInputStream(byte[] body) {
            this.bodyStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() throws IOException {
            return bodyStream.read();
        }

        @Override
        @SneakyThrows
        public boolean isFinished() {
            return bodyStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 如果需要，可以在此实现读取监听器逻辑
        }
    }
}

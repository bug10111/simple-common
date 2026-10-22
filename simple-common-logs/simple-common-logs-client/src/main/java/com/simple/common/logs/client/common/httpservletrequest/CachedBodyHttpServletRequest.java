package com.simple.common.logs.client.common.httpservletrequest;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
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
 * - multipart/form-data (仅缓存元数据，不缓存文件内容)
 * </p>
 * <p>
 * 注意：application/x-www-form-urlencoded 不在缓存之列——该类型由容器延迟解析，
 * 提前消费请求体会导致 getParameter 系列方法取不到参数；
 * 表单参数日志改由参数表（ParameterMap）途径采集，语义不受影响。
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
            "text/plain"
    };

    /**
     * 缓存的请求体内容
     */
    private final byte[] body;

    /**
     * 是否已缓存请求体
     */
    private final boolean cached;

    /**
     * 原始Content-Type
     */
    private final String originalContentType;

    public CachedBodyHttpServletRequest(HttpServletRequest request, LogTcpClientProperties properties) throws IOException {
        super(request);
        this.originalContentType = request.getContentType();
        boolean shouldCache = shouldCacheBody(originalContentType);
        // 检查是否应该缓存，并且请求体大小未超过配置限制
        if (shouldCache) {
            long contentLength = request.getContentLengthLong();
            if (contentLength > properties.getMaxBodyCacheSize()) {
                log.debug("请求体大小 {} 字节超过缓存限制 {} 字节，跳过缓存", contentLength, properties.getMaxBodyCacheSize());
                shouldCache = false;
            } else if (contentLength < 0) {
                // 无 Content-Length 的 chunked/流式请求体不缓存：
                // 体积未知，全量读入内存存在 OOM 风险；半途放弃已消费的流还会破坏请求
                log.debug("请求体无 Content-Length（chunked 传输），跳过缓存");
                shouldCache = false;
            }
        }
        this.body = shouldCache ? StreamUtils.copyToByteArray(request.getInputStream()) : null;
        this.cached = shouldCache;
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
        // 按请求自身字符编码解码，缺省回退 UTF-8，避免非 UTF-8 载荷被破坏
        String encoding = getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return new String(body, StandardCharsets.UTF_8);
        }
        try {
            return new String(body, encoding);
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(body, StandardCharsets.UTF_8);
        }
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
        return cached && body != null && body.length > 0;
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
    public ServletInputStream getInputStream() throws IOException {
        if (cached && body != null) {
            return new CachedBodyServletInputStream(body);
        }
        return super.getInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        // 按请求自身字符编码读取，缺省回退 UTF-8，避免非 UTF-8 载荷被默认编码破坏
        String encoding = getCharacterEncoding();
        return new BufferedReader(new InputStreamReader(getInputStream(),
                encoding != null && !encoding.isBlank() ? encoding : StandardCharsets.UTF_8.name()));
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final InputStream bodyStream;
        private boolean finished = false;

        public CachedBodyServletInputStream(byte[] body) {
            this.bodyStream = new ByteArrayInputStream(body);
        }

        @Override
        public int read() throws IOException {
            int data = bodyStream.read();
            if (data == -1) {
                finished = true;
            }
            return data;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            // 重写批量读取：父类默认逐字节循环拷贝严重放大开销；
            // 以读取结果判定完成，修复"恰好读满缓冲区时 isFinished 仍为 false"的问题
            int read = bodyStream.read(b, off, len);
            if (read == -1) {
                finished = true;
            }
            return read;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 按 Servlet 规范：非异步请求调用 setReadListener 必须抛 IllegalStateException
            // （UnsupportedOperationException 不在容器/框架的预期异常面内，会破坏异步错误处理）
            throw new IllegalStateException("CachedBodyHttpServletRequest does not support ReadListener (non-async request)");
        }
    }
}
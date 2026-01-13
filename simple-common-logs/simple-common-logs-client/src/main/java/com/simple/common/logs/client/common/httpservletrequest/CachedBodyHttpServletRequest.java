package com.simple.common.logs.client.common.httpservletrequest;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.SneakyThrows;
import org.springframework.util.StreamUtils;

import java.io.*;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] body;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        String contentType = request.getContentType();
        body = ObjUtil.isNotEmpty(contentType) && contentType.contains(ContentType.JSON.getValue()) ? StreamUtils.copyToByteArray(request.getInputStream()) : new byte[0];
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

package com.simple.common.core.utils;

import com.simple.common.core.common.entity.HttpRecord;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * http请求帮助类单测：postJson 请求体与 Content-Type、postForm 表单体、post 历史重载向后兼容。
 *
 * @author qty
 */
class HttpRequestUtilsTest {

    private HttpServer server;

    private String url;

    private final AtomicReference<String> requestMethod = new AtomicReference<>();

    private final AtomicReference<String> requestBody = new AtomicReference<>();

    private final AtomicReference<String> requestContentType = new AtomicReference<>();

    /**
     * 启动本地探针服务：记录方法/请求体/Content-Type 并返回固定 JSON。
     *
     * @throws IOException 端口绑定失败
     */
    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/probe", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/probe";
    }

    /**
     * 停止本地探针服务。
     */
    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    /**
     * postJson：请求体为 JSON 文本，未声明 Content-Type 时补充 application/json。
     */
    @Test
    void postJsonShouldSerializeJsonBodyAndSetContentType() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "a");
        body.put("count", 2);

        HttpRecord record = HttpRequestUtils.postJson(url, new HashMap<>(), body, 5000);

        assertEquals(200, record.getExecute().getStatus());
        assertEquals("POST", requestMethod.get());
        assertTrue(requestBody.get().contains("\"name\":\"a\""));
        assertTrue(requestBody.get().contains("\"count\":2"));
        assertEquals("application/json", requestContentType.get());
    }

    /**
     * postJson：调用方已声明 Content-Type（大小写不敏感）时保留原值，不覆盖。
     */
    @Test
    void postJsonShouldKeepExplicitContentType() {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json; charset=utf-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "a");

        HttpRequestUtils.postJson(url, headers, body, 5000);

        assertEquals("application/json; charset=utf-8", requestContentType.get());
    }

    /**
     * postForm：请求体为表单编码，不走 JSON 序列化。
     */
    @Test
    void postFormShouldSendFormEncodedBody() {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("userName", "admin");

        HttpRequestUtils.postForm(url, new HashMap<>(), form, 5000);

        assertTrue(requestBody.get().contains("userName=admin"));
        assertTrue(requestBody.get().startsWith("userName=admin"));
    }

    /**
     * post（Object 历史重载）：委托 postJson，请求体为 JSON 且携带 JSON Content-Type。
     *
     * <p>注意：实参静态类型必须为 Object 才命中该重载；静态类型为 Map 时按重载解析命中 form 重载。</p>
     */
    @Test
    void postObjectShouldDelegateToJsonBody() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", "deepseek-chat");
        Object body = payload;

        HttpRequestUtils.post(url, new HashMap<>(), body, 5000);

        assertTrue(requestBody.get().contains("\"model\":\"deepseek-chat\""));
        assertEquals("application/json", requestContentType.get());
    }
}

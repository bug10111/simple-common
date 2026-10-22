package com.simple.common.auth.server.exchange.sync.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.auth.server.common.properties.ExchangeSyncProperties;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;
import java.nio.charset.StandardCharsets;

/**
 * 实例回调客户端（投递引擎 → 业务实例接收端点）。
 * <p>
 * 将同步载荷 POST 到实例注册表登记的回调地址，HTTP 200 视为送达（200-ACK）；
 * 无响应/非 200/超时一律视为投递失败交由 worker 重投，其中确定性客户端错误
 * （4xx 永久拒绝）抛 {@link SyncNonRetryableException} 直接收口不耗重试预算。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class SyncCallbackClient {

    /**
     * 送达判定状态码（HTTP 200 = 接收端落地成功）
     */
    private static final int ACK_STATUS = 200;

    /**
     * 非重试状态码集合（确定性客户端错误：重试同请求注定失败；429 限流为可重试故不在列）
     */
    private static final Set<Integer> NON_RETRYABLE_STATUSES = Set.of(400, 401, 403, 404, 405, 410, 413, 414, 415);

    /**
     * 失败日志携带的响应体上限（字符）
     */
    private static final int ERROR_BODY_SNIPPET_LENGTH = 200;

    @Autowired
    private ExchangeSyncProperties exchangeSyncProperties;

    /**
     * 回调单实例接收端点
     *
     * @param callbackUrl 实例回调地址
     * @param payload     同步载荷
     * @throws SyncNonRetryableException 确定性客户端错误（4xx 永久拒绝，交由 worker 直接收口记失败）
     * @throws IllegalStateException     非 200 响应（含状态码信息，交由 worker 记失败并重投）
     * @throws RuntimeException          网络异常/超时（透传调用方异常，交由 worker 记失败并重投）
     */
    public void post(String callbackUrl, SyncPayload payload) {
        // 超时换算：秒→毫秒（秒值已由 ExchangeSyncProperties 装配期校验为正数，int 乘法无溢出）
        int timeoutMillis = exchangeSyncProperties.getCallbackTimeoutSeconds() * 1000;
        try (HttpResponse response = HttpRequest.post(callbackUrl)
                                                    .header("Content-Type", "application/json")
                                                    .body(JsonUtils.toJsonStr(payload))
                                                    .timeout(timeoutMillis)
                                                    .execute()) {

            // 非 200 视为投递失败：确定性客户端错误直接收口，其余抛出带状态码异常交由 worker 重投
            int status = response.getStatus();
            if (status != ACK_STATUS) {
                if (NON_RETRYABLE_STATUSES.contains(status)) {
                    throw new SyncNonRetryableException("实例回调确定性拒绝: status=" + status
                            + " url=" + maskUrl(callbackUrl) + " body=" + snippet(response.body()));
                }
                throw new IllegalStateException("实例回调非200响应: status=" + status
                        + " url=" + maskUrl(callbackUrl) + " body=" + snippet(response.body()));
            }
            log.debug("实例回调送达: url={} payloadType={}", maskUrl(callbackUrl), payload.getPayloadType());
        }
    }

    /**
     * 脱敏回调地址：仅保留 scheme/host/path，查询串（可能携带凭证）不落日志
     *
     * @param url 原始回调地址
     * @return 脱敏后的地址（解析失败返回 "unknown"）
     */
    private static String maskUrl(String url) {
        try {
            URI uri = URI.create(url);
            String hostPart = uri.getHost() == null ? "unknown" : uri.getHost();
            return uri.getScheme() + "://" + hostPart
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                    + (uri.getPath() == null ? "" : uri.getPath());
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 截取响应体片段（诊断用，限长且按 UTF-8 安全截断）
     *
     * @param body 响应体
     * @return 限长片段
     */
    private static String snippet(String body) {
        if (body == null) {
            return "";
        }
        String text = new String(body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        return text.length() <= ERROR_BODY_SNIPPET_LENGTH ? text : text.substring(0, ERROR_BODY_SNIPPET_LENGTH) + "...";
    }
}

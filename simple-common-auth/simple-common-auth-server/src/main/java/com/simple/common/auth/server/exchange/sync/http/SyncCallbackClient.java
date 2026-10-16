package com.simple.common.auth.server.exchange.sync.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.auth.server.common.properties.ExchangeSyncProperties;
import com.simple.common.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 实例回调客户端（投递引擎 → 业务实例接收端点）
 * <p>
 * 将同步载荷 POST 到实例注册表登记的回调地址，HTTP 200 视为送达（200-ACK）；
 * 非响应/非 200/超时一律视为投递失败交由 worker 重投。
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

    @Autowired
    private ExchangeSyncProperties exchangeSyncProperties;

    /**
     * 回调单实例接收端点
     *
     * @param callbackUrl 实例回调地址
     * @param payload     同步载荷
     * @throws IllegalStateException 非 200 响应（含状态码信息，交由 worker 记失败）
     * @throws RuntimeException      网络异常/超时（透传调用方异常，交由 worker 记失败）
     */
    public void post(String callbackUrl, SyncPayload payload) {
        HttpResponse response = HttpRequest.post(callbackUrl)
                                            .header("Content-Type", "application/json")
                                            .body(JsonUtils.toJsonStr(payload))
                                            .timeout(exchangeSyncProperties.getCallbackTimeoutSeconds() * 1000)
                                            .execute();

        // 非 200 视为投递失败，抛出带状态码的异常交由 worker 记录
        if (response.getStatus() != ACK_STATUS) {
            throw new IllegalStateException("实例回调非200响应: status=" + response.getStatus() + " url=" + callbackUrl);
        }
        log.debug("实例回调送达: url={} payloadType={}", callbackUrl, payload.getPayloadType());
    }

}

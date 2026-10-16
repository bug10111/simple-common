package com.simple.common.auth.server.exchange.sync.producer;

import cn.hutool.core.util.IdUtil;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.auth.server.common.properties.ExchangeSyncProperties;
import com.simple.common.auth.server.exchange.sync.SyncInstanceRegistry;
import com.simple.common.auth.server.exchange.sync.task.SyncDeliveryTask;
import com.simple.common.eventbus.common.service.EventBusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 投递任务生产者（变更源 → 工作队列的唯一入口）
 * <p>
 * 变更源（密钥轮换/权限变更/登出通知）构造载荷后经本生产者发入本服务
 * 工作队列（durable，队列名=应用名，MQ 凭证每应用独立是队列隔离前提）；
 * 定向投递按客户端编码入队，全员通知类载荷按实例注册表扇出为逐客户端任务。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class SyncDeliveryProducer {

    @Autowired
    private EventBusService eventBusService;

    @Autowired
    private SyncInstanceRegistry syncInstanceRegistry;

    @Autowired
    private ExchangeSyncProperties exchangeSyncProperties;

    /**
     * 按载荷归属项目定向投递
     *
     * @param payload 同步载荷（projectCode 即目标客户端编码）
     */
    public void dispatch(SyncPayload payload) {
        dispatchToClient(payload, payload.getProjectCode());
    }

    /**
     * 定向投递到指定客户端
     *
     * @param payload  同步载荷
     * @param clientId 目标客户端编码
     */
    public void dispatchToClient(SyncPayload payload, String clientId) {
        SyncDeliveryTask task = new SyncDeliveryTask();
        task.setTaskId(IdUtil.fastSimpleUUID())
            .setClientId(clientId)
            .setPayload(payload)
            .setRetryCount(0);
        eventBusService.push(task);
        log.info("投递任务已入队: taskId={} clientId={} payloadType={}", task.getTaskId(), clientId, payload.getPayloadType());
    }

    /**
     * 全员通知投递（按实例注册表扇出为逐客户端任务）
     *
     * @param payload 同步载荷
     */
    public void dispatchToAllClients(SyncPayload payload) {
        List<String> clientIds = syncInstanceRegistry.findAllClientIds();

        // 逐客户端入队（每客户端独立任务独立重投，互不阻塞）
        for (String clientId : clientIds) {
            dispatchToClient(payload, clientId);
        }
        log.info("全员通知投递完成入队: payloadType={} 客户端数={}", payload.getPayloadType(), clientIds.size());
    }

    /**
     * 失败延迟重投（worker 专用：任务整体重发，重试次数已由 worker 递增）
     *
     * @param task 待重投任务（retryCount 已递增）
     */
    public void redispatch(SyncDeliveryTask task) {
        eventBusService.push(task, exchangeSyncProperties.getRetryIntervalSeconds(), TimeUnit.SECONDS);
        log.info("投递任务延迟重投入队: taskId={} clientId={} retryCount={}", task.getTaskId(), task.getClientId(), task.getRetryCount());
    }

}

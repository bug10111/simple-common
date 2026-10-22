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

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 投递任务生产者（变更源 → 工作队列的唯一入口）。
 * <p>
 * 变更源（密钥轮换/权限变更/登出通知）构造载荷后经本生产者发入本服务
 * 工作队列（durable，队列名=应用名，MQ 凭证每应用独立是队列隔离的前提）；
 * 定向投递按客户端编码入队，全员通知类载荷按实例注册表扇出为逐客户端任务——
 * 每客户端独立载荷副本、独立任务、独立重投，互不阻塞、互不串写。
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
     * 按载荷归属项目定向投递（公共入口：供框架外模块/插件按载荷自带目标投递）
     *
     * @param payload 同步载荷（projectCode 即目标客户端编码）
     */
    public void dispatch(SyncPayload payload) {
        dispatchToClient(payload, payload.getProjectCode());
    }

    /**
     * 定向投递到指定客户端
     * <p>
     * 载荷的 projectCode 由本方法按目标客户端盖章（权威目标以 clientId 入参为准），
     * 接收端按 projectCode 与自身项目比对做归属过滤；入参载荷先做防御性拷贝再盖章，
     * 不回写调用方的载荷对象，同批多客户端投递互不串写。
     * </p>
     *
     * @param payload  同步载荷（非空）
     * @param clientId 目标客户端编码（非空白）
     * @throws IllegalArgumentException payload 为空或 clientId 空白时抛出——入队前快速失败，
     *                                  不把注定失败的任务投入队列再在消费端静默丢失
     */
    public void dispatchToClient(SyncPayload payload, String clientId) {
        if (payload == null) {
            throw new IllegalArgumentException("同步载荷不能为空");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("目标客户端编码不能为空");
        }

        // 防御性拷贝并盖章：同一载荷对象不得跨客户端复用（projectCode 相互覆盖会路由错目标）
        SyncPayload stamped = copyPayload(payload);
        stamped.setProjectCode(clientId);

        SyncDeliveryTask task = new SyncDeliveryTask();
        task.setTaskId(IdUtil.fastSimpleUUID())
                .setClientId(clientId)
                .setPayload(stamped)
                .setRetryCount(0);

        // 入队失败记告警并原样抛出：调用方（如登出链路）需要感知通知未投出；
        // 全员扇出路径由 dispatchToAllClients 逐客户端隔离，单客户端入队失败不中断其余目标
        try {
            eventBusService.push(task);
        } catch (Exception e) {
            log.error("投递任务入队失败: taskId={} clientId={} payloadType={}", task.getTaskId(), clientId, payload.getPayloadType(), e);
            throw e;
        }
        log.info("投递任务已入队: taskId={} clientId={} payloadType={}", task.getTaskId(), clientId, payload.getPayloadType());
    }

    /**
     * 全员通知投递（按实例注册表扇出为逐客户端任务）
     * <p>
     * 逐客户端独立载荷、独立任务、独立重投，互不阻塞；注册表返回空集合时告警——
     * 全员通知（如登出）静默丢失是需要暴露的注册表异常。
     * </p>
     *
     * @param payload 同步载荷（projectCode 由扇出过程按目标盖章）
     */
    public void dispatchToAllClients(SyncPayload payload) {
        List<String> clientIds = syncInstanceRegistry.findAllClientIds();

        // 全员通知无目标：注册表空注册属需暴露的异常（通知被静默丢弃），warn 留痕后返回
        if (clientIds == null || clientIds.isEmpty()) {
            log.warn("全员通知无目标客户端，载荷未投递: payloadType={}", payload == null ? null : payload.getPayloadType());
            return;
        }

        // 逐客户端入队（每客户端独立任务独立重投，单客户端入队失败不中断其余目标）
        for (String clientId : clientIds) {
            try {
                dispatchToClient(payload, clientId);
            } catch (Exception e) {
                log.error("全员通知投递入队失败（不中断其余目标）: clientId={}", clientId, e);
            }
        }
        log.info("全员通知投递完成入队: payloadType={} 客户端数={}", payload.getPayloadType(), clientIds.size());
    }

    /**
     * 失败延迟重投（worker 专用：任务整体重发，重试次数已由 worker 递增）
     *
     * @param task 待重投任务（独立副本，retryCount 已递增）
     */
    public void redispatch(SyncDeliveryTask task) {
        eventBusService.push(task, exchangeSyncProperties.getRetryIntervalSeconds(), TimeUnit.SECONDS);
        log.info("投递任务延迟重投入队: taskId={} clientId={} retryCount={}", task.getTaskId(), task.getClientId(), task.getRetryCount());
    }

    /**
     * 载荷防御性拷贝（逐字段复制，浅拷贝：secrets/permissions Map 同步复制，
     * 防止同批多客户端任务共享同一可变 Map）
     *
     * @param payload 源载荷
     * @return 新载荷副本
     */
    private static SyncPayload copyPayload(SyncPayload payload) {
        SyncPayload copy = new SyncPayload();
        copy.setPayloadType(payload.getPayloadType())
                .setProjectCode(payload.getProjectCode())
                .setSecrets(payload.getSecrets() == null ? null : new HashMap<>(payload.getSecrets()))
                .setRoleKey(payload.getRoleKey())
                .setTenantId(payload.getTenantId())
                .setPermissions(payload.getPermissions() == null ? null : new HashMap<>(payload.getPermissions()))
                .setChangeType(payload.getChangeType())
                .setTimestamp(payload.getTimestamp())
                .setUserId(payload.getUserId());
        return copy;
    }

}

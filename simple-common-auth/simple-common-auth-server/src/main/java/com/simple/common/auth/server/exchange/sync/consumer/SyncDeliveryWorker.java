package com.simple.common.auth.server.exchange.sync.consumer;

import com.simple.common.auth.server.common.properties.ExchangeSyncProperties;
import com.simple.common.auth.server.exchange.sync.SyncInstance;
import com.simple.common.auth.server.exchange.sync.SyncInstanceRegistry;
import com.simple.common.auth.server.exchange.sync.SyncRecord;
import com.simple.common.auth.server.exchange.sync.SyncRecordStore;
import com.simple.common.auth.server.exchange.sync.http.SyncCallbackClient;
import com.simple.common.auth.server.exchange.sync.producer.SyncDeliveryProducer;
import com.simple.common.auth.server.exchange.sync.task.SyncDeliveryTask;
import com.simple.common.eventbus.common.annotation.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 投递 worker（工作队列消费端）
 * <p>
 * 消费投递任务后按实例注册表逐实例回调接收端点：200-ACK 即刻落成功记录；
 * 失败实例随任务延迟重投（重投后按实例注册表现值重新枚举，期间新注册实例一并补达），
 * 超限后按实例落失败记录收口。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class SyncDeliveryWorker {

    @Autowired
    private SyncInstanceRegistry syncInstanceRegistry;

    @Autowired
    private SyncCallbackClient syncCallbackClient;

    @Autowired
    private SyncRecordStore syncRecordStore;

    @Autowired
    private SyncDeliveryProducer syncDeliveryProducer;

    @Autowired
    private ExchangeSyncProperties exchangeSyncProperties;

    /**
     * 消费投递任务
     *
     * @param task 投递任务
     */
    @EventHandler
    public void onDeliveryTask(SyncDeliveryTask task) {
        deliver(task);
    }

    /**
     * 执行一次投递
     *
     * @param task 投递任务
     */
    private void deliver(SyncDeliveryTask task) {
        // 按实例注册表取目标实例（存活判定由注册表实现承载）
        List<SyncInstance> instances = syncInstanceRegistry.findAliveInstances(task.getClientId());

        // 无存活实例：无目标可投（密钥靠注册自举兜底，缓存类载荷实例注册后自然重建）
        if (instances.isEmpty()) {
            log.info("投递任务无存活实例跳过: taskId={} clientId={} payloadType={}", task.getTaskId(), task.getClientId(), task.getPayload().getPayloadType());
            return;
        }

        // 逐实例回调并收集失败
        Map<SyncInstance, String> failures = deliverToInstances(task, instances);

        // 落定：全成即收口，有失败走重投或超限收口
        settleDelivery(task, failures);
    }

    /**
     * 逐实例回调（成功即刻落记录，失败收集后统一落定）
     *
     * @param task      投递任务
     * @param instances 目标实例清单
     * @return 失败实例 → 错误信息
     */
    private Map<SyncInstance, String> deliverToInstances(SyncDeliveryTask task, List<SyncInstance> instances) {
        Map<SyncInstance, String> failures = new LinkedHashMap<>();

        // 逐实例回调（单实例失败不阻断其余实例投递）
        for (SyncInstance instance : instances) {
            try {
                syncCallbackClient.post(instance.getCallbackUrl(), task.getPayload());
                saveRecord(buildRecord(task, instance, true, task.getRetryCount(), null));
            } catch (Exception e) {
                log.warn("实例回调失败: taskId={} clientId={} url={} 原因={}", task.getTaskId(), task.getClientId(), instance.getCallbackUrl(), e.getMessage());
                failures.put(instance, e.getMessage());
            }
        }
        return failures;
    }

    /**
     * 投递落定：全部送达即收口；有失败按重试预算延迟重投，超限（或重投入队失败）落失败记录收口
     *
     * @param task     投递任务
     * @param failures 失败实例 → 错误信息
     */
    private void settleDelivery(SyncDeliveryTask task, Map<SyncInstance, String> failures) {
        // 全部送达：直接收口
        if (failures.isEmpty()) {
            return;
        }

        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();

        // 重试超限：落失败记录收口
        if (retryCount >= exchangeSyncProperties.getMaxRetries()) {
            failures.forEach((instance, error) -> saveRecord(buildRecord(task, instance, false, retryCount, error)));
            log.error("投递任务重试超限收口: taskId={} clientId={} 失败实例数={}", task.getTaskId(), task.getClientId(), failures.size());
            return;
        }

        // 延迟重投（任务整体重发，retryCount 递增；重投入队失败时落失败记录，防任务静默消失）
        try {
            syncDeliveryProducer.redispatch(task.setRetryCount(retryCount + 1));
        } catch (Exception e) {
            log.error("投递任务重投入队失败: taskId={} clientId={}", task.getTaskId(), task.getClientId(), e);
            failures.forEach((instance, error) -> saveRecord(buildRecord(task, instance, false, retryCount, error + "；重投入队失败: " + e.getMessage())));
        }
    }

    /**
     * 构建同步记录
     *
     * @param task       投递任务
     * @param instance   目标实例
     * @param success    是否送达
     * @param retryCount 落定时的重试次数
     * @param error      失败错误信息
     * @return 同步记录
     */
    private SyncRecord buildRecord(SyncDeliveryTask task, SyncInstance instance, boolean success, Integer retryCount, String error) {
        SyncRecord record = new SyncRecord();
        record.setPayloadType(task.getPayload().getPayloadType() == null ? null : task.getPayload().getPayloadType().name())
              .setClientId(task.getClientId())
              .setInstanceIp(instance.getInstanceIp())
              .setCallbackUrl(instance.getCallbackUrl())
              .setSuccess(success)
              .setRetryCount(retryCount)
              .setErrorMessage(error)
              .setSyncTime(new Date());
        return record;
    }

    /**
     * 落同步记录（记录写入失败不阻断投递主链路，记错误日志留痕）
     *
     * @param record 同步记录
     */
    private void saveRecord(SyncRecord record) {
        try {
            syncRecordStore.save(record);
        } catch (Exception e) {
            log.error("同步记录写入失败: clientId={} url={} success={}", record.getClientId(), record.getCallbackUrl(), record.getSuccess(), e);
        }
    }

}

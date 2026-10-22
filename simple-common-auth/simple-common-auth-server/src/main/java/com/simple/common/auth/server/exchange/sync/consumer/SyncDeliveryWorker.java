package com.simple.common.auth.server.exchange.sync.consumer;

import com.simple.common.auth.server.common.properties.ExchangeSyncProperties;
import com.simple.common.auth.server.exchange.sync.SyncInstance;
import com.simple.common.auth.server.exchange.sync.SyncInstanceRegistry;
import com.simple.common.auth.server.exchange.sync.SyncRecord;
import com.simple.common.auth.server.exchange.sync.SyncRecordStore;
import com.simple.common.auth.server.exchange.sync.http.SyncCallbackClient;
import com.simple.common.auth.server.exchange.sync.http.SyncNonRetryableException;
import com.simple.common.auth.server.exchange.sync.producer.SyncDeliveryProducer;
import com.simple.common.auth.server.exchange.sync.task.SyncDeliveryTask;
import com.simple.common.eventbus.common.annotation.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 投递 worker（工作队列消费端）。
 * <p>
 * 消费投递任务后按实例注册表逐实例回调接收端点：200-ACK 即刻落成功记录并把实例 IP
 * 记入已送达集合；失败实例随任务延迟重投（重投携带独立任务副本与已送达集合，
 * 已 ACK 实例不再重复回调），不可重试的确定性拒绝即刻落失败记录收口，
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
        // 载荷守卫：载荷为可空反序列化字段，缺失任务无法投递，显式丢弃并留痕而非在深层 NPE
        if (task.getPayload() == null) {
            log.error("投递任务载荷为空，丢弃: taskId={} clientId={} retryCount={}", task.getTaskId(), task.getClientId(), task.getRetryCount());
            return;
        }

        // 按实例注册表取目标实例（存活判定由注册表实现承载；SPI 契约为永不返回 null，此处仍做归一防御）
        List<SyncInstance> instances = syncInstanceRegistry.findAliveInstances(task.getClientId());
        if (instances == null) {
            instances = Collections.emptyList();
        }

        // 无存活实例：无目标可投（密钥靠注册自举兜底，缓存类载荷实例注册后自然重建）；
        // warn 留痕携带 retryCount——重投期间实例全体失联意味着待补投递将随收口消失，需要可观测
        if (instances.isEmpty()) {
            log.warn("投递任务无存活实例跳过（不再投递）: taskId={} clientId={} payloadType={} retryCount={}",
                    task.getTaskId(), task.getClientId(), task.getPayload().getPayloadType(), task.getRetryCount());
            return;
        }

        // 逐实例回调并收集失败
        Map<SyncInstance, String> failures = deliverToInstances(task, instances);

        // 落定：全成即收口，有失败走重投或超限收口
        settleDelivery(task, failures);
    }

    /**
     * 逐实例回调（成功即刻落记录，失败收集后统一落定）
     * <p>
     * 已送达实例（deliveredIps）跳过：重投按注册表现值重新枚举实例，不对已 ACK 实例
     * 重复回调、不重复落成功记录，保证记录表可用于审计。
     * </p>
     *
     * @param task      投递任务
     * @param instances 目标实例清单
     * @return 失败实例 → 错误信息
     */
    private Map<SyncInstance, String> deliverToInstances(SyncDeliveryTask task, List<SyncInstance> instances) {
        Map<SyncInstance, String> failures = new LinkedHashMap<>();

        // 逐实例回调（单实例失败不阻断其余实例投递）
        for (SyncInstance instance : instances) {
            if (task.getDeliveredIps() != null && task.getDeliveredIps().contains(instance.getInstanceIp())) {
                log.debug("实例已送达，重投跳过: taskId={} clientId={} instanceIp={}", task.getTaskId(), task.getClientId(), instance.getInstanceIp());
                continue;
            }
            try {
                syncCallbackClient.post(instance.getCallbackUrl(), task.getPayload());
                // ACK 即记入已送达集合：重投副本按此剔除，不重复回调、不重复落成功记录
                task.getDeliveredIps().add(instance.getInstanceIp());
                saveRecord(buildRecord(task, instance, true, task.getRetryCount(), null));
            } catch (SyncNonRetryableException e) {
                // 确定性拒绝（4xx 永久错误）：重试注定失败，直接落失败记录收口，不消耗重试预算
                log.warn("实例回调确定性拒绝（不重试）: taskId={} clientId={} instanceIp={} 原因={}",
                        task.getTaskId(), task.getClientId(), instance.getInstanceIp(), e.getMessage());
                saveRecord(buildRecord(task, instance, false, task.getRetryCount(), "不可重试: " + e.getMessage()));
            } catch (Exception e) {
                log.warn("实例回调失败: taskId={} clientId={} instanceIp={} 原因={}",
                        task.getTaskId(), task.getClientId(), instance.getInstanceIp(), e.getMessage());
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

        int retryCount = task.getRetryCount();

        // 重试超限：落失败记录收口
        if (retryCount >= exchangeSyncProperties.getMaxRetries()) {
            failures.forEach((instance, error) -> saveRecord(buildRecord(task, instance, false, retryCount, error)));
            log.error("投递任务重试超限收口: taskId={} clientId={} 失败实例数={}", task.getTaskId(), task.getClientId(), failures.size());
            return;
        }

        // 延迟重投：重投独立任务副本（已送达集合随行、retryCount 递增），不污染消费中的任务实例；
        // 重投入队失败时按实例落失败记录，防任务静默消失
        try {
            syncDeliveryProducer.redispatch(task.copyForRedispatch());
        } catch (Exception e) {
            log.error("投递任务重投入队失败: taskId={} clientId={}", task.getTaskId(), task.getClientId(), e);
            failures.forEach((instance, error) ->
                    saveRecord(buildRecord(task, instance, false, retryCount, error + "；重投入队失败: " + e.getMessage())));
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
    private SyncRecord buildRecord(SyncDeliveryTask task, SyncInstance instance, boolean success, int retryCount, String error) {
        SyncRecord record = new SyncRecord();
        record.setPayloadType(task.getPayload() == null || task.getPayload().getPayloadType() == null
                        ? null : task.getPayload().getPayloadType().name())
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
     * 落同步记录（记录写入失败不阻断投递主链路——200-ACK 不因审计落库失败回滚为失败；
     * 记错误日志留痕，落库可靠性由 {@link SyncRecordStore} 实现方保障）
     *
     * @param record 同步记录
     */
    private void saveRecord(SyncRecord record) {
        try {
            syncRecordStore.save(record);
        } catch (Exception e) {
            log.error("同步记录写入失败（投递主链路不受影响，请检查记录存储实现）: clientId={} instanceIp={} payloadType={} success={}",
                    record.getClientId(), record.getInstanceIp(), record.getPayloadType(), record.isSuccess(), e);
        }
    }

}

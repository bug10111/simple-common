package com.simple.common.auth.server.exchange.sync.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Set;

/**
 * 实例投递任务（投递引擎的工作队列消息体）。
 * <p>
 * 变更源构造载荷后经投递生产者发入本服务工作队列（durable），
 * 投递 worker 消费后按实例注册表逐实例回调；单实例失败经延迟重投
 * （任务整体重发，retryCount 递增，已送达实例经 deliveredIps 剔除不再重复回调），
 * 超限后按实例落失败记录收口。
 * </p>
 * <p>
 * 幂等契约：实例回调本身幂等（缓存落地/密钥覆盖），但重投会重复回调已送达实例之外的
 * 存活实例并重复落成功记录，接收端与记录存储方须容忍重复消息与重复记录。
 * </p>
 *
 * @author qty
 */
@Data
@Event(targets = EventConstant.THIS_MACHINE)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class SyncDeliveryTask {

    /**
     * 任务 ID（一次变更投递的唯一标识，跨重投不变）
     */
    private String taskId;

    /**
     * 目标客户端编码（实例注册表按此取实例清单）
     */
    private String clientId;

    /**
     * 同步载荷
     */
    private SyncPayload payload;

    /**
     * 已重试次数（0=首次投递；int 缺省 0，杜绝旧版本消息缺失字段时的 null 统计）
     */
    private int retryCount;

    /**
     * 已送达实例 IP 集合（重投时按此剔除已 ACK 实例，避免重复回调与重复成功记录；
     * 兼容旧版本消息缺省为空集合）
     */
    private Set<String> deliveredIps = new HashSet<>();

    /**
     * 防御性拷贝（重投时携带独立副本，不共享消费中的任务实例）
     *
     * @return 新的投递任务（字段逐项复制；payload 为同一载荷引用——载荷已按客户端独立构造）
     */
    public SyncDeliveryTask copyForRedispatch() {
        SyncDeliveryTask copy = new SyncDeliveryTask();
        copy.setTaskId(this.taskId)
            .setClientId(this.clientId)
            .setPayload(this.payload)
            .setRetryCount(this.retryCount)
            .setDeliveredIps(new HashSet<>(this.deliveredIps));
        return copy;
    }
}

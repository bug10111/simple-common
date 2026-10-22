package com.simple.common.auth.server.exchange.sync.task;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;
import com.simple.common.eventbus.common.annotation.Event;
import com.simple.common.eventbus.common.constants.EventConstant;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 实例投递任务（投递引擎的工作队列消息本体）
 * <p>
 * 变更源构造载荷后经投递生产者发入本服务工作队列（durable），
 * 投递 worker 消费后按实例注册表逐实例回调；单实例失败经延迟重投
 * （任务整体重发，retryCount 递增），超限后按实例落失败记录。
 * 实例回调语义天然幂等（缓存落地/密钥覆盖），任务重发不产生副作用累积。
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
     * 已重试次数（0=首次投递）
     */
    private Integer retryCount;

}

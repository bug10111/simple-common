package com.simple.common.auth.server.exchange.sync;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 实例同步记录（一次投递对单个实例的最终落定）
 * <p>
 * 成功与失败均落记录：成功记录送达时的重试次数，失败记录超限时的重试次数与错误信息，
 * 控制台按客户端聚合展示。
 * </p>
 * <p>
 * 语义契约：本记录不含去重键——投递引擎的失败重投会按存活实例重放任务，存储实现方
 * （{@link SyncRecordStore}）需容忍同一 (payloadType, clientId, instanceIp) 的多次落定记录，
 * 以 syncTime 最近的记录为准。
 * </p>
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
public class SyncRecord {

    /**
     * 载荷类型（SECRET / PERMISSION / LOGOUT）
     */
    private String payloadType;

    /**
     * 目标客户端编码
     */
    private String clientId;

    /**
     * 目标实例本机 IP
     */
    private String instanceIp;

    /**
     * 目标实例回调地址
     */
    private String callbackUrl;

    /**
     * 是否送达（true=收到 200-ACK；false=重试超限仍未送达）
     */
    private boolean success;

    /**
     * 落定时的重试次数（0=首次投递即送达；类型 int 保证缺省 0，杜绝 null 混入统计）
     */
    private int retryCount;

    /**
     * 失败错误信息（成功为空）
     */
    private String errorMessage;

    /**
     * 落定时间
     */
    private Date syncTime;

    /**
     * 获取落定时间（防御性拷贝：Date 可变，防止落定时间在聚合展示间被篡改）
     *
     * @return 落定时间（克隆副本）
     */
    public Date getSyncTime() {
        return syncTime == null ? null : (Date) syncTime.clone();
    }

    /**
     * 设置落定时间（防御性拷贝）
     *
     * @param syncTime 落定时间
     * @return 当前记录
     */
    public SyncRecord setSyncTime(Date syncTime) {
        this.syncTime = syncTime == null ? null : (Date) syncTime.clone();
        return this;
    }
}

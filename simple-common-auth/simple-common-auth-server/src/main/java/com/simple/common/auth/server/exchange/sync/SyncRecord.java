package com.simple.common.auth.server.exchange.sync;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 实例同步记录（一次投递对单个实例的最终落定）
 * <p>
 * 成功与失败均落记录：成功记送达时的重试次数，失败记超限时的重试次数与错误信息，
 * 控制台按客户端聚合呈现。
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
    private Boolean success;

    /**
     * 落定时的重试次数（0=首次投递即送达）
     */
    private Integer retryCount;

    /**
     * 失败错误信息（成功为空）
     */
    private String errorMessage;

    /**
     * 落定时间
     */
    private Date syncTime;

}

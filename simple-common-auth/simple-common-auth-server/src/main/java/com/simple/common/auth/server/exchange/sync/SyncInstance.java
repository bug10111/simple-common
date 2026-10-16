package com.simple.common.auth.server.exchange.sync;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 实例同步快照（实例注册表中参与投递的存活实例）
 *
 * @author qty
 */
@Data
@Accessors(chain = true)
public class SyncInstance {

    /**
     * 所属客户端编码
     */
    private String clientId;

    /**
     * 实例本机 IP
     */
    private String instanceIp;

    /**
     * 实例接收回调地址（投递引擎按此回调）
     */
    private String callbackUrl;

    /**
     * 最近一次注册/刷新时间
     */
    private Date registerTime;

}

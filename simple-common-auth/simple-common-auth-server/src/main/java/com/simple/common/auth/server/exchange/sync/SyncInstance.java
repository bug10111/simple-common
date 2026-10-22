package com.simple.common.auth.server.exchange.sync;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 实例同步快照（实例注册表中参与投递的存活实例）
 * <p>
 * 相等性契约：仅以 {@code instanceIp + callbackUrl} 判定同一投递目标——
 * 本类型作为投递失败记录的 Map 键使用，全字段值相等（如 registerTime 相同的
 * 两个不同注册项）不等价于同一投递目标；以全字段做值相等会静默合并失败记录。
 * </p>
 *
 * @author qty
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Accessors(chain = true)
public class SyncInstance {

    /**
     * 所属客户端编码
     */
    private String clientId;

    /**
     * 实例本机 IP（投递目标的身份键之一；非空是投递正确性的前提）
     */
    @EqualsAndHashCode.Include
    private String instanceIp;

    /**
     * 实例接收回调地址（投递引擎按此回调并落记录；须为合法绝对 URL，非空是投递正确性的前提）
     */
    @EqualsAndHashCode.Include
    private String callbackUrl;

    /**
     * 最近一次注册/刷新时间
     */
    private Date registerTime;

    /**
     * 获取最近一次注册/刷新时间（防御性拷贝：Date 可变，防止快照注册时间被外部篡改）
     *
     * @return 注册时间（克隆副本）
     */
    public Date getRegisterTime() {
        return registerTime == null ? null : (Date) registerTime.clone();
    }

    /**
     * 设置最近一次注册/刷新时间（防御性拷贝）
     *
     * @param registerTime 注册时间
     * @return 当前快照
     */
    public SyncInstance setRegisterTime(Date registerTime) {
        this.registerTime = registerTime == null ? null : (Date) registerTime.clone();
        return this;
    }
}

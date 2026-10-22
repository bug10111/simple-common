package com.simple.common.auth.client.exchange.sync.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 同步载荷类型
 * <p>
 * 授权中心向业务实例定向投递的变更载荷分类，接收端按类型落地各自缓存。
 * </p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum SyncPayloadType {

    /**
     * 密钥同步（载荷字段：projectCode + secrets；接收端落地 TokenManager/SignManager 密钥缓存）
     */
    SECRET("密钥同步"),

    /**
     * 权限同步（载荷字段：projectCode + roleKey + permissions + changeType + timestamp；接收端落地本地权限缓存）
     */
    PERMISSION("权限同步"),

    /**
     * 登出通知（载荷字段：projectCode + userId；接收端清除该用户本地令牌缓存）
     */
    LOGOUT("登出通知"),

    /**
     * 未知类型兜底：滚动升级/回滚期间，旧实例收到新版本引入的类型时，
     * 接收端 ObjectMapper 需开启 READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE 指向本常量，
     * 配合 DefaultSyncPayloadApplier 的 default 分支走「记日志+照常ACK」，避免整条投递失败重投风暴
     */
    UNKNOWN("未知类型");

    /**
     * 中文说明
     */
    private final String label;

}

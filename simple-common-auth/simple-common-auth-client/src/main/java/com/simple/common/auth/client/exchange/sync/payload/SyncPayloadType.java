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
    LOGOUT("登出通知");

    /**
     * 中文说明
     */
    private final String label;

}

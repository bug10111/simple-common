package com.simple.common.auth.client.exchange.sync.receiver;

import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;

/**
 * 同步载荷落地器（接收端核心）
 * <p>
 * 按载荷类型把中心投递的变更落地到本实例缓存：
 * SECRET → 密钥缓存；PERMISSION → 角色权限缓存；LOGOUT → 用户令牌缓存。
 * </p>
 *
 * @author qty
 */
public interface SyncPayloadApplier {

    /**
     * 落地同步载荷
     *
     * @param payload 同步载荷
     */
    void apply(SyncPayload payload);

}

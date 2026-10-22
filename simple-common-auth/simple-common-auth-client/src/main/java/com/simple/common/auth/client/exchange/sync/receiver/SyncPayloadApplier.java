package com.simple.common.auth.client.exchange.sync.receiver;

import com.simple.common.auth.client.exchange.sync.payload.SyncPayload;

/**
 * 同步载荷落地器（接收端核心）
 * <p>
 * 按载荷类型把中心投递的变更落地到本实例缓存：
 * SECRET → 密钥缓存；PERMISSION → 角色权限缓存；LOGOUT → 用户令牌缓存。
 * </p>
 * <p>
 * 实现契约：
 * <ul>
 *   <li><b>并发安全</b>：同一实例可能被投递引擎并发调用 apply（多在途投递/重试并发），
 *       且实现会读写共享缓存（密钥/权限/令牌）——必须保证并发安全，
 *       避免 check-then-act 与非原子读改写；</li>
 *   <li><b>幂等</b>：同一载荷可能被重复投递（重试）或乱序投递，落地必须幂等；</li>
 *   <li><b>失败语义</b>：落地失败抛出运行时异常（接收端返回非 200 触发中心重投）；
 *       payload 为 null 属编程错误（实现应抛出 IllegalArgumentException）；
 *       类型未识别（UNKNOWN/default 分支）应记日志后正常返回并照常 ACK（实例配置漂移不制造重投风暴）。</li>
 * </ul>
 * </p>
 *
 * @author qty
 */
public interface SyncPayloadApplier {

    /**
     * 落地同步载荷（幂等；失败抛运行时异常触发中心重投）
     *
     * @param payload 同步载荷，不允许为 null
     */
    void apply(SyncPayload payload);

}

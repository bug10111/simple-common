package com.simple.common.rabbitmq.common.manager;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;

import java.util.concurrent.TimeUnit;

/**
 * 消息消费防重锁管理器接口（v2，owner token 语义）。
 *
 * <h3>锁值格式（跨版本契约）：</h3>
 * <ul>
 *   <li>处理中：{@code <lockStatusProcessing>:<最近心跳毫秒>:<ownerToken>}，心跳由 {@link #renewLock} 刷新；</li>
 *   <li>已完成：{@code <lockStatusDonePrefix><businessId>}（与 v1 兼容，完成锁不再携带 token）。</li>
 * </ul>
 * <h3>v1 兼容与迁移语义：</h3>
 * <p>
 * 升级部署后，存量 v1 锁值（纯 PROCESSING 常量、{@code DONE:xxx}）按如下方式处理：
 * {@code DONE:xxx} 正常参与幂等判定；纯 PROCESSING（无心跳段）视为"存活处理中"，
 * 只能等其 TTL 自然过期或被持锁消费者结算，不会被误删。防重锁 TTL 短（businessTime 级），
 * 存量键分钟级自愈，无需人工迁移。
 * </p>
 * <h3>所有者语义：</h3>
 * <p>
 * {@link #tryAcquire} 成功返回的 ownerToken 是该锁的持有凭证；续期、释放、标记完成、恢复接管
 * 均以 token 做 Lua CAS 原子校验，杜绝"误删他人锁/覆盖他人状态"的竞态。
 * </p>
 *
 * @author qty
 */
public interface ConsumptionLockManager {

    /**
     * 尝试获取消费锁（Redis SET NX PX 原子操作）
     *
     * @param queue         队列名称
     * @param correlationId 消息唯一标识
     * @param businessTime  业务预估执行时长，换算后必须大于 0
     * @param timeUnit      时间单位
     * @return 获取成功返回本次持有的 ownerToken（非 null），失败返回 null
     * @throws IllegalArgumentException businessTime 换算后不大于 0，或 queue/correlationId 为空白
     */
    String tryAcquire(String queue, String correlationId, long businessTime, TimeUnit timeUnit);

    /**
     * 续期锁：仅当锁值仍为本 ownerToken 的处理中状态时成功（Lua 原子），同时刷新心跳时间戳。
     * <p>
     * Redis 基础设施异常会以异常抛出（调用方应区分"锁已丢失"返回 false 与"Redis 不可用"抛异常两种结局）。
     * </p>
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param ownerToken    {@link #tryAcquire} 返回的持有凭证
     * @param businessTime  业务预估时长，换算后必须大于 0
     * @param timeUnit      时间单位
     * @return true-续期成功；false-锁不存在或已被他人接管（持有权丢失）
     */
    boolean renewLock(String queue, String correlationId, String ownerToken, long businessTime, TimeUnit timeUnit);

    /**
     * 检查已存在的锁状态（一次 Lua 往返内原子读取状态与 TTL，避免 GET 与 TTL 两步之间的竞态）。
     * <p>
     * 副作用：当锁为处理中且心跳静默超过恢复阈值时，会缩短该锁 TTL 以尽快触发接管（返回
     * {@link LockStatus#PROCESSING_WITH_RECOVERED_TTL}）。校验策略异常会向上传播，由调用方决定重投。
     * </p>
     *
     * @param queue          队列名
     * @param correlationId  消息ID
     * @param defaultMessage 消息体
     * @param message        原始Message对象（供校验策略使用）
     * @return 锁状态视图（含 businessId 与当前锁值快照，供条件操作做期望值）
     */
    LockView checkLockStatus(String queue, String correlationId, DefaultMessage defaultMessage, Message message);

    /**
     * 原子接管恢复态锁：仅当锁为处理中、心跳静默超过恢复阈值时，将持有权转移给新 ownerToken
     * 并以 businessTime 重建 TTL（Lua 内二次校验心跳，原持有者续期会让接管失败）。
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param businessTime  新锁的业务预估时长，换算后必须大于 0
     * @param timeUnit      时间单位
     * @return 接管成功返回新 ownerToken，失败返回 null（锁已消失/已被接管/为完成锁/非本实现格式）
     */
    String takeOverRecoveredLock(String queue, String correlationId, long businessTime, TimeUnit timeUnit);

    /**
     * 条件释放处理中锁：仅当锁值仍为本 ownerToken 时删除（Lua compare-and-delete），
     * 不会误删已被他人接管的锁。
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param ownerToken    持有凭证
     * @return true-确由本调用方删除；false-锁不存在或不属于该凭证（无操作）
     */
    boolean release(String queue, String correlationId, String ownerToken);

    /**
     * 按期望值条件删除（用于清理完成态锁等场景）：仅当当前锁值与 expectedValue 完全一致时删除。
     *
     * @param queue          队列名
     * @param correlationId  消息ID
     * @param expectedValue  期望的当前锁值（来自 {@link LockView#value()}）
     * @return true-已删除；false-锁不存在或值已变化（无操作）
     */
    boolean releaseIfValue(String queue, String correlationId, String expectedValue);

    /**
     * 标记完成：仅当锁值仍为本 ownerToken 的处理中状态时，原子改写为
     * {@code <lockStatusDonePrefix><businessId>} 并按 effectiveTime 计时（Lua CAS），
     * 不会覆盖其他消费者新获取的处理中锁。
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param ownerToken    持有凭证
     * @param businessId    业务返回的唯一标识
     * @param effectiveTime 完成状态有效时长，换算后必须大于 0
     * @param timeUnit      时间单位
     * @return true-标记成功；false-锁已丢失（调用方不得 ACK，应走丢失恢复路径）
     */
    boolean markAsDone(String queue, String correlationId, String ownerToken, String businessId, long effectiveTime, TimeUnit timeUnit);

    /**
     * 获取锁的 Redis Key（格式 {@code {namespace}:{queue}:{correlationId}}，跨版本稳定；
     * queue/correlationId 为空白时抛出 IllegalArgumentException）。
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @return Redis Key
     */
    String buildLockKey(String queue, String correlationId);

    /**
     * 锁状态枚举
     */
    enum LockStatus {
        /** 锁不存在 */
        NOT_EXISTS,
        /** 处理中（心跳正常，确有消费者在处理） */
        PROCESSING,
        /** 已完成且校验通过 */
        COMPLETED_AND_VERIFIED,
        /** 已完成但校验未通过 */
        COMPLETED_BUT_VERIFICATION_FAILED,
        /** 处理中但心跳静默超阈值（疑似持锁者已死亡），已缩短 TTL 等待接管 */
        PROCESSING_WITH_RECOVERED_TTL
    }

    /**
     * 锁状态视图：状态 + 已完成消息的业务ID + 当前锁值快照（供条件操作做期望值）
     */
    record LockView(LockStatus status, String businessId, String value) {
    }
}

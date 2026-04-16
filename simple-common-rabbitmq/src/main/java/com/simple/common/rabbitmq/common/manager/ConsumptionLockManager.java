package com.simple.common.rabbitmq.common.manager;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;

import java.util.concurrent.TimeUnit;

/**
 * 消息消费防重锁管理器接口
 *
 * @author qty
 */
public interface ConsumptionLockManager {

    /**
     * 尝试获取消费锁（原子操作）
     *
     * @param queue         队列名称
     * @param correlationId 消息唯一标识
     * @param businessTime  业务预估执行时长
     * @param timeUnit      时间单位
     * @return true-获取成功，false-锁已存在
     */
    boolean tryAcquire(String queue, String correlationId, int businessTime, TimeUnit timeUnit);

    /**
     * 续期锁的过期时间，仅当锁的值仍为 PROCESSING 时成功
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param businessTime  业务预估时长
     * @param timeUnit      时间单位
     * @return true-续期成功，false-锁不存在或已变更
     */
    boolean renewLock(String queue, String correlationId, int businessTime, TimeUnit timeUnit);

    /**
     * 检查已存在的锁状态，返回处理建议
     *
     * @param queue          队列名
     * @param correlationId  消息ID
     * @param defaultMessage 消息体
     * @param message        原始Message对象（供校验策略使用）
     * @return 锁状态及建议操作
     */
    LockStatus checkLockStatus(String queue, String correlationId, DefaultMessage defaultMessage, Message message);

    /**
     * 删除锁
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     */
    void release(String queue, String correlationId);

    /**
     * 将锁标记为已完成
     *
     * @param queue           队列名
     * @param correlationId   消息ID
     * @param businessId      业务返回的唯一标识
     * @param effectiveSeconds 完成状态有效时长
     * @param timeUnit        时间单位
     */
    void markAsDone(String queue, String correlationId, String businessId, long effectiveSeconds, TimeUnit timeUnit);

    /**
     * 重置锁为处理中状态（用于校验失败时重新进入处理状态）
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @param businessTime  业务预估时长
     * @param timeUnit      时间单位
     */
    void resetToProcessing(String queue, String correlationId, int businessTime, TimeUnit timeUnit);

    /**
     * 获取锁的 Redis Key
     *
     * @param queue         队列名
     * @param correlationId 消息ID
     * @return Redis Key 字符串
     */
    String buildLockKey(String queue, String correlationId);

    /**
     * 锁状态枚举
     */
    enum LockStatus {
        /** 锁不存在 */
        NOT_EXISTS,
        /** 处理中 */
        PROCESSING,
        /** 已完成且校验通过 */
        COMPLETED_AND_VERIFIED,
        /** 已完成但校验未通过 */
        COMPLETED_BUT_VERIFICATION_FAILED,
        /** 处理中但TTL异常长，已自动缩短 */
        PROCESSING_WITH_RECOVERED_TTL
    }
}
package com.simple.common.rabbitmq.common.manager;

/**
 * 重试次数管理器接口。
 * <p>
 * 用于管理消息消费失败后的重试次数，防止无限重试导致系统资源耗尽。
 * 默认实现 {@link com.simple.common.rabbitmq.manager.DefaultRetryCountManager} 使用Redis
 * 存储重试次数计数器。
 * </p>
 *
 * @author qty
 */
public interface RetryCountManager {

    /**
     * 原子递增计数。
     * <p>
     * 实现必须是原子操作且线程安全。
     * TTL 由实现配置决定，且每次递增都会刷新过期时间（滑动过期窗口）；
     * 若 Key 已存在但没有 TTL，实现应为其补设 TTL，避免计数器永久泄漏。
     * </p>
     *
     * @param retryKey 计数 Key，不允许为 null 或空串
     * @return 递增后的计数值（首次递增返回 1）
     */
    long incrementAndGet(String retryKey);

    /**
     * 删除计数；Key 不存在时为无操作（不抛异常）。
     * <p>
     * 底层存储异常的处理方式由实现决定（默认实现记录警告日志，不向调用方抛出，
     * 避免阻断消费流程；残留计数会在 TTL 到期后自动清理）。
     * </p>
     *
     * @param retryKey 计数 Key
     */
    void delete(String retryKey);

    /**
     * 构建业务失败重试 Key。
     * <p>
     * 格式为 {@code {namespace}:{queue}:{correlationId}}，需在实现间保持一致且跨版本稳定。
     * 实现必须对 queue、correlationId 做非空校验（空白时抛出 IllegalArgumentException）；
     * 入参中不建议包含 ":" 分隔符（含 ":" 的组合在理论上可能与其他消息 Key 碰撞）。
     * Key 均带 TTL 自动过期，升级更换格式后旧计数自然失效，无需迁移。
     * </p>
     *
     * @param queue         队列名，不允许为空白
     * @param correlationId 消息关联 ID，不允许为空白
     * @return 业务重试计数 Key
     */
    String buildRetryKey(String queue, String correlationId);

    /**
     * 构建校验失败重试 Key（与业务重试 Key 独立计数）。
     * <p>
     * 实现必须保证校验 Key 与 {@link #buildRetryKey} 产生的 Key 空间互不重叠
     * （默认实现使用独立的固定前缀段 {@code {namespace}:verify:{queue}:{correlationId}}，
     * 而非自由格式后缀，避免 correlationId 以 ":verify" 结尾时的拼接碰撞）。
     * queue、correlationId 非空校验要求与 {@link #buildRetryKey} 相同。
     * </p>
     *
     * @param queue         队列名，不允许为空白
     * @param correlationId 消息关联 ID，不允许为空白
     * @return 校验重试计数 Key
     */
    String buildVerifyRetryKey(String queue, String correlationId);
}

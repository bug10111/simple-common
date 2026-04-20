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
     * 原子递增计数，并设置过期时间（若首次创建）
     *
     * @param retryKey 计数 Key
     * @return 递增后的计数值
     */
    long incrementAndGet(String retryKey);

    /**
     * 删除计数
     */
    void delete(String retryKey);

    /**
     * 构建业务失败重试 Key
     */
    String buildRetryKey(String queue, String correlationId);

    /**
     * 构建校验失败重试 Key（独立计数）
     */
    String buildVerifyRetryKey(String queue, String correlationId);
}
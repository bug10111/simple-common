package com.simple.common.rabbitmq.common.manager;

/**
 * 重试计数管理器接口，提供原子递增与删除功能
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
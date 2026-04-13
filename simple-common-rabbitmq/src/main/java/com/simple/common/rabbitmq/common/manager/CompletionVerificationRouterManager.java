package com.simple.common.rabbitmq.common.manager;

/**
 * 业务完成校验策略路由器，根据队列名获取对应的校验策略。
 *
 * @author qty
 */
public interface CompletionVerificationRouterManager {

    /**
     * 根据队列名获取校验策略，若未找到则返回 null
     *
     * @param queueName 队列名称
     * @return 对应的策略，或 null 表示无需校验（默认信任 Redis）
     */
    CompletionVerificationStrategyManager getStrategy(String queueName);
}
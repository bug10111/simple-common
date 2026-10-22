package com.simple.common.rabbitmq.common.manager;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * 业务完成校验策略路由器，根据队列名获取对应的校验策略。
 * <p>
 * 队列名匹配语义：<b>精确匹配、区分大小写</b>（实现基于注册表精确 Key 查找）。
 * 不支持前缀/通配匹配——传入与注册名不一致的队列名将得到 null（调用方按"无需校验"处理），
 * 请确保策略注册名（{@code getQueueName()}）与消费队列名完全一致。
 * </p>
 *
 * @author qty
 */
public interface CompletionVerificationRouterManager {

    /**
     * 根据队列名获取校验策略，若未注册则返回 null。
     * <p>
     * 返回 null 语义 = 该队列无需校验（默认信任 Redis 完成标记，直接放行）；
     * 调用方应区分"null-无需校验"与"策略返回 false-校验未通过"两种情形。
     * </p>
     *
     * @param queueName 队列名称，不允许为 null 或空白（空白输入将被拒绝并记录告警，返回 null）
     * @return 对应策略，或 null 表示无需校验
     */
    @Nullable
    CompletionVerificationStrategyManager getStrategy(@NonNull String queueName);
}

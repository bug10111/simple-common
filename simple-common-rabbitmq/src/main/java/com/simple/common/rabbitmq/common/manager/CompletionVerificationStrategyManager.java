package com.simple.common.rabbitmq.common.manager;

import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.amqp.core.Message;

/**
 * 业务完成校验策略，每个队列可对应不同的校验实现。
 *
 * @author qty
 */
public interface CompletionVerificationStrategyManager {

    /**
     * 判断指定业务ID对应的业务是否真正完成。
     * <p>
     * 契约：
     * </p>
     * <ul>
     *   <li>返回 true = 业务已完成，重复消息可安全丢弃；</li>
     *   <li>返回 false = 业务未完成，重复消息应重新消费；</li>
     *   <li>校验过程中发生异常（DB/RPC 失败、超时等）必须<b>抛出异常</b>交由调用方处理，
     *       <b>禁止</b>返回 true（会导致未完成业务被误丢弃），也慎用 false（会驱动无限重消费）；</li>
     *   <li>对永久无效的消息（业务上永远无法完成）应返回 true 并在业务侧落失败标记，
     *       或由实现方自行告警处理，避免死循环重试。</li>
     * </ul>
     *
     * @param businessId     业务方法返回的唯一标识（不允许为 null）
     * @param defaultMessage 消息体对象（可能为 null）
     * @param message        原始消息对象（可访问消息头等元数据，可能为 null）
     * @return true-已完成，可安全丢弃消息；false-未完成，应重新消费
     * @throws RuntimeException 校验过程发生异常时抛出，由调用方决策（重投/告警）
     */
    boolean isCompleted(@NonNull String businessId, @Nullable DefaultMessage defaultMessage, @Nullable Message message);

    /**
     * 返回该策略支持的队列名称
     * <p>
     * 契约：返回值必须非 null、非空白、跨实例稳定（不得随状态变化），
     * 且在整个应用内对该队列唯一——同一队列注册多个策略时容器启动直接失败，
     * 不会静默覆盖（防止消息被路由到错误策略）。
     * 返回值将与消费队列名做<b>精确匹配</b>（区分大小写），请与监听队列名完全一致。
     * </p>
     *
     * @return 队列名称，不可为 null/空白
     */
    @NonNull
    String getQueueName();
}

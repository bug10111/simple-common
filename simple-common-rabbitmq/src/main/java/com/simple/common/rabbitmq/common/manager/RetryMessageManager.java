package com.simple.common.rabbitmq.common.manager;

import com.rabbitmq.client.Channel;
import com.simple.common.rabbitmq.common.entity.DefaultMessage;
import org.springframework.amqp.core.Message;

/**
 * 消息重试管理器接口。
 * <p>
 * 用于管理RabbitMQ消息消费失败后的重试逻辑,支持业务失败重试和校验失败重试两种场景。
 * 默认实现 {@link com.simple.common.rabbitmq.manager.DefaultRetryMessageManager} 基于Redis计数器
 * 控制重试次数,超过最大重试次数后将消息持久化到失败队列或数据库。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>业务失败重试：数据库连接超时、第三方API调用失败等临时性错误</li>
 *   <li>校验失败重试：数据一致性校验未通过,等待上游系统完成后再重试</li>
 *   <li>幂等性保证：通过唯一标识防止重复消费</li>
 * </ul>
 *
 * @author qty
 */
public interface RetryMessageManager {

    /**
     * 处理业务失败重试
     * <p>
     * 当消息消费过程中抛出业务异常时调用此方法。会根据重试次数决定是重新入队还是持久化失败消息。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * try {
     *     // 执行业务逻辑
     *     processMessage(message);
     * } catch (Exception e) {
     *     String retryKey = retryCountManager.buildRetryKey(queueName, correlationId);
     *     retryMessageManager.handleBusinessFailureRetry(retryKey, message, channel, maxRetryCount, e, defaultMessage);
     * }
     * }</pre>
     *
     * @param retryKey       重试计数Key,格式为 "queue:correlationId"
     * @param message        原始消息对象,包含消息体和相关元数据
     * @param channel        RabbitMQ通道,用于执行ack/nack操作
     * @param maxRetryCount  最大重试次数,超过此次数后将不再重试
     * @param cause          导致失败的异常对象
     * @param defaultMessage 解析后的消息体对象
     */
    void handleBusinessFailureRetry(String retryKey, Message message, Channel channel, int maxRetryCount, Exception cause, DefaultMessage defaultMessage);

    /**
     * 处理校验失败重试
     * <p>
     * 当消息业务执行成功但后续校验(如数据一致性校验)失败时调用此方法。
     * 与业务失败重试独立计数,避免相互影响。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 业务执行成功
     * String businessId = processMessage(message);
     * 
     * // 校验是否真正完成
     * if (!verificationStrategy.isCompleted(businessId, defaultMessage, message)) {
     *     String verifyRetryKey = retryCountManager.buildVerifyRetryKey(queueName, correlationId);
     *     retryMessageManager.handleVerificationFailureRetry(verifyRetryKey, message, channel, maxRetryCount, null, defaultMessage);
     * }
     * }</pre>
     *
     * @param verifyRetryKey 校验重试计数Key,与业务重试Key隔离
     * @param message        原始消息对象
     * @param channel        RabbitMQ通道
     * @param maxRetryCount  最大重试次数
     * @param cause          导致失败的异常对象(可能为null)
     * @param defaultMessage 解析后的消息体对象
     */
    void handleVerificationFailureRetry(String verifyRetryKey, Message message, Channel channel, int maxRetryCount, Exception cause, DefaultMessage defaultMessage);
}
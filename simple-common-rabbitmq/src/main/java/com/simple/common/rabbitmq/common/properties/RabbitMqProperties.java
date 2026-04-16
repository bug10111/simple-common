package com.simple.common.rabbitmq.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 消息队列配置属性类
 * <p>
 * 所有属性均可通过 Spring Boot 配置文件进行覆盖，前缀为 {@code simple.mq.rabbitmq}。
 * 例如：{@code simple.mq.rabbitmq.mq-package=myapp:}
 * </p>
 *
 * @author qty
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.mq.rabbitmq")
public class RabbitMqProperties {

    /**
     * Redis 中存储 RabbitMQ 相关信息的根命名空间（包名前缀）
     * <p>
     * 默认值为 "rabbitmq:"，用于隔离不同业务或环境的 Key，避免冲突。
     * 例如：锁 Key 为 {@code mqPackage + "whether_to_consume:" + queue + ":" + correlationId}
     * </p>
     */
    private String mqPackage = "rabbitmq:";

    /**
     * 防重消费锁的 Redis Key 前缀（动态拼接自 mqPackage）
     * <p>
     * 实际值通过 {@link #getWhetherToConsume()} 动态获取，无需外部直接配置。
     * 最终锁 Key 格式：{@code mqPackage + "whether_to_consume:" + queue + ":" + correlationId}
     * </p>
     */
    private String whetherToConsume;

    /**
     * 重试计数的 Redis Key 前缀（动态拼接自 mqPackage）
     * <p>
     * 实际值通过 {@link #getCalculation()} 动态获取，无需外部直接配置。
     * 最终计数 Key 格式：{@code mqPackage + "calculation:" + queue + ":" + correlationId}
     * </p>
     */
    private String calculation;

    /**
     * 延迟重试交换机名称
     * <p>
     * 使用 RabbitMQ 延迟消息插件（rabbitmq_delayed_message_exchange），
     * 消费失败后会将消息发送至此交换机，经过指数退避延迟后重新投递到原队列。
     * </p>
     * <p>默认值：{@code simple.delayed.retry.exchange}</p>
     */
    private String delayedExchange = "simple.delayed.retry.exchange";

    /**
     * Redis 中重试计数的过期时间（单位：秒）
     * <p>
     * 每次重试会刷新该过期时间，确保计数 Key 在整个重试窗口内有效。
     * 若业务重试延迟可能超过该值，请适当调大。
     * </p>
     * <p>默认值：{@code 3600}（1小时）</p>
     */
    private long retryCountTtlSeconds = 3600L;

    /**
     * 防重锁状态值：处理中
     * <p>
     * 当消息开始消费时，锁的值会被设置为该常量。
     * </p>
     * <p>默认值：{@code "PROCESSING"}</p>
     */
    private String lockStatusProcessing = "PROCESSING";

    /**
     * 防重锁状态值前缀：已完成
     * <p>
     * 业务成功执行后，锁的值会被设置为 {@code lockStatusDonePrefix + businessId}，
     * 用于后续重复消息的快速幂等判断。
     * </p>
     * <p>默认值：{@code "DONE:"}</p>
     */
    private String lockStatusDonePrefix = "DONE:";

    /**
     * 锁 TTL 恢复阈值（单位：秒）
     * <p>
     * 当消费者因宕机等原因重启后，若检测到旧锁的剩余 TTL 大于该阈值，
     * 会将其缩短至该值，以便新消费者能尽快接管。
     * </p>
     * <p>默认值：{@code 5}</p>
     */
    private long lockTtlRecoveryThresholdSeconds = 5L;

    /**
     * 重试延迟基数（单位：毫秒）
     * <p>
     * 第一次重试的延迟时间，后续重试采用指数退避策略（乘以 2 的幂次），
     * 但不会超过 {@link #retryDelayMaxMs}。
     * </p>
     * <p>默认值：{@code 5000}（5秒）</p>
     */
    private long retryDelayBaseMs = 5000L;

    /**
     * 最大重试延迟（单位：毫秒）
     * <p>
     * 指数退避计算出的延迟若超过该值，则取该值作为上限。
     * </p>
     * <p>默认值：{@code 60000}（60秒）</p>
     */
    private long retryDelayMaxMs = 60000L;

    /**
     * 动态获取防重锁的 Redis Key 前缀
     *
     * @return 完整前缀，如 {@code "rabbitmq:whether_to_consume"}
     */
    public String getWhetherToConsume() {
        return mqPackage + "whether_to_consume";
    }

    /**
     * 动态获取重试计数的 Redis Key 前缀
     *
     * @return 完整前缀，如 {@code "rabbitmq:calculation"}
     */
    public String getCalculation() {
        return mqPackage + "calculation";
    }

    /**
     * 配置属性合理性校验
     * <p>
     * 在 Bean 初始化后执行，对非法或边界值进行自动修正，并打印警告日志。
     * </p>
     */
    @PostConstruct
    public void validate() {
        if (lockTtlRecoveryThresholdSeconds <= 0) {
            log.warn("lockTtlRecoveryThresholdSeconds 配置为 {}，已自动调整为 5", lockTtlRecoveryThresholdSeconds);
            lockTtlRecoveryThresholdSeconds = 5L;
        }
        if (retryCountTtlSeconds <= 0) {
            log.warn("retryCountTtlSeconds 配置为 {}，已自动调整为 3600", retryCountTtlSeconds);
            retryCountTtlSeconds = 3600L;
        }
        if (retryDelayBaseMs <= 0) {
            log.warn("retryDelayBaseMs 配置为 {}，已自动调整为 5000", retryDelayBaseMs);
            retryDelayBaseMs = 5000L;
        }
        if (retryDelayMaxMs < retryDelayBaseMs) {
            log.warn("retryDelayMaxMs ({}) 小于 retryDelayBaseMs ({})，已自动调整为 {}", 
                    retryDelayMaxMs, retryDelayBaseMs, retryDelayBaseMs * 10);
            retryDelayMaxMs = retryDelayBaseMs * 10;
        }
    }
}
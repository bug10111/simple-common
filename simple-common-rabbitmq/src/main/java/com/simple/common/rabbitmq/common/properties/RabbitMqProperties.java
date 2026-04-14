package com.simple.common.rabbitmq.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 配置属性
 *
 * @author qty
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.mq.rabbitmq")
public class RabbitMqProperties {

    // ==================== 基础配置 ====================

    /**
     * RabbitMQ相关储存信息的总包名
     */
    private String mqPackage = "rabbitmq:";

    /**
     * 是否消费（防重锁前缀）
     */
    private String whetherToConsume = mqPackage + "whether_to_consume";

    /**
     * 消费计数（重试计数前缀）
     */
    private String calculation = mqPackage + "calculation";

    /**
     * 延迟交换机名称，用于消费失败后的延迟重试
     */
    private String delayedExchange = "simple.mq.delayed.exchange";

    /**
     * Redis 重试计数过期时间（秒），默认 1 小时
     */
    private long retryCountTtlSeconds = 3600L;

    /**
     * 防重锁状态：处理中
     */
    private String lockStatusProcessing = "PROCESSING";

    /**
     * 防重锁状态：已完成（前缀）
     */
    private String lockStatusDonePrefix = "DONE:";

    /**
     * 宕机恢复时，锁剩余 TTL 大于此值则缩短至该值（秒），默认 5 秒
     */
    private long lockTtlRecoveryThresholdSeconds = 5L;

    /**
     * 重试延迟基数（毫秒）：第一次重试延迟，默认 5 秒
     */
    private long retryDelayBaseMs = 5000L;

    /**
     * 最大重试延迟（毫秒），默认 60 秒
     */
    private long retryDelayMaxMs = 60000L;

    /**
     * ✅ 配置合理性校验，在 Bean 初始化后执行
     */
    @PostConstruct
    public void validate() {
        if (lockTtlRecoveryThresholdSeconds <= 0) {
            log.warn("lockTtlRecoveryThresholdSeconds 配置为 {}，建议大于 0，已自动调整为 5", lockTtlRecoveryThresholdSeconds);
            lockTtlRecoveryThresholdSeconds = 5L;
        }
        if (retryCountTtlSeconds <= 0) {
            log.warn("retryCountTtlSeconds 配置为 {}，建议大于 0，已自动调整为 3600", retryCountTtlSeconds);
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
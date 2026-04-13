package com.simple.common.rabbitmq.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 配置属性
 *
 * @author qty
 */
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
     * 发送消息备份
     */
    private String sendBackup = mqPackage + "send_backup";

    /**
     * 持久化失败
     */
    private String sendError = mqPackage + "send_error";

    /**
     * 延迟交换机名称，用于消费失败后的延迟重试
     */
    private String delayedExchange = "simple.mq.delayed.exchange";

    /**
     * Redis 重试计数过期时间（秒）
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
     * 宕机恢复时，锁剩余 TTL 大于此值则缩短至该值（秒）
     */
    private long lockTtlRecoveryThresholdSeconds = 5L;

    /**
     * 重试延迟基数（毫秒）：第一次重试延迟
     */
    private long retryDelayBaseMs = 5000L;

    /**
     * 最大重试延迟（毫秒）
     */
    private long retryDelayMaxMs = 60000L;


}
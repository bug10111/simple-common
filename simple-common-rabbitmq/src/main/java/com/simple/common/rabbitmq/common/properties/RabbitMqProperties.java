package com.simple.common.rabbitmq.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;

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
     * 防重消费锁的 Redis Key 前缀
     * <p>
     * 默认未配置时动态拼接自 mqPackage（{@code mqPackage + "whether_to_consume"}）；
     * 显式配置后以配置值为准。
     * </p>
     */
    private String whetherToConsume;

    /**
     * 重试计数的 Redis Key 前缀
     * <p>
     * 默认未配置时动态拼接自 mqPackage（{@code mqPackage + "calculation"}）；
     * 显式配置后以配置值为准。
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
     * 每次重试都会刷新该过期时间，确保计数 Key 在整个重试窗口内有效。
     * 若业务重试延迟可能超过该值，请适当调大。
     * </p>
     * <p>默认值：{@code 3600}（1小时）</p>
     */
    private long retryCountTtlSeconds = 3600L;

    /**
     * 防重锁状态值：处理中
     * <p>
     * 处理中锁值为 {@code <该值>:<心跳毫秒>:<ownerToken>}。
     * 该值不允许包含 ":" 且不能与 {@link #lockStatusDonePrefix} 前缀重叠（启动时校验）。
     * </p>
     * <p>默认值：{@code "PROCESSING"}</p>
     */
    private String lockStatusProcessing = "PROCESSING";

    /**
     * 防重锁状态值前缀：已完成
     * <p>
     * 业务成功执行后，锁的值会被设置为 {@code lockStatusDonePrefix + businessId}，
     * 用于后续重复消息的快速幂等判断。该前缀不允许为空，且不能以 {@link #lockStatusProcessing} 开头。
     * </p>
     * <p>默认值：{@code "DONE:"}</p>
     */
    private String lockStatusDonePrefix = "DONE:";

    /**
     * 锁恢复阈值（单位：秒）
     * <p>
     * 处理中锁值携带最近心跳时间（续期时刷新）；当心跳静默超过该阈值时，
     * 判定持锁消费者疑似宕机，锁进入恢复态（缩短 TTL），允许新消费者原子接管。
     * 该值应大于锁续期间隔（businessTime/3）的 2 倍以上，避免误伤慢业务。
     * </p>
     * <p>默认值：{@code 5}</p>
     */
    private long lockTtlRecoveryThresholdSeconds = 5L;

    /**
     * 重试延迟基数（单位：毫秒）
     * <p>
     * 第一次重试的延迟时间，后续重试采用指数退避策略（乘以 2 的幂次，指数封顶 30），
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
     * 消息本地兜底持久化目录（死信消息与发送失败消息默认兜底实现的 JSON 行文件根目录）
     * <p>
     * 默认值为系统临时目录下 simple-rabbitmq-fallback 子目录，兜底文件按天滚动；
     * 生产环境建议配置到持久化磁盘目录，并实现 DeadLetterService / SendFailurePersistenceManager
     * 接口将失败消息落库，替换默认本地文件兜底实现。
     * 注意：兜底文件包含完整消息体，可能含敏感信息，目录权限默认收紧为仅属主可读写。
     * </p>
     */
    private String fallbackDir = System.getProperty("java.io.tmpdir") + File.separator + "simple-rabbitmq-fallback";

    /**
     * 本地兜底文件保留天数（单位：天）
     * <p>
     * 写入兜底文件时会顺带清理早于该天数的滚动文件，防止文件无限累积写满磁盘；
     * 0 表示不自动清理（需自行安排外部清理任务）。
     * </p>
     * <p>默认值：{@code 30}</p>
     */
    private int fallbackRetentionDays = 30;

    /**
     * 获取防重锁的 Redis Key 前缀
     * <p>
     * 显式配置了 {@code simple.mq.rabbitmq.whether-to-consume} 时返回配置值，否则动态拼接自 mqPackage。
     * </p>
     *
     * @return 完整前缀，如 {@code "rabbitmq:whether_to_consume"}
     */
    public String getWhetherToConsume() {
        return whetherToConsume != null ? whetherToConsume : mqPackage + "whether_to_consume";
    }

    /**
     * 获取重试计数的 Redis Key 前缀
     * <p>
     * 显式配置了 {@code simple.mq.rabbitmq.calculation} 时返回配置值，否则动态拼接自 mqPackage。
     * </p>
     *
     * @return 完整前缀，如 {@code "rabbitmq:calculation"}
     */
    public String getCalculation() {
        return calculation != null ? calculation : mqPackage + "calculation";
    }

    /**
     * 配置属性合理性校验
     * <p>
     * 在 Bean 初始化后执行，对非法或边界值进行自动修正，并打印警告日志。
     * 字符串配置（命名空间、锁状态值、交换机、兜底目录）为空时回退默认值并告警，
     * 避免空白配置产生无命名空间 Redis Key、锁状态误判（startsWith("") 恒真）或非法交换机名等隐蔽故障。
     * </p>
     */
    @PostConstruct
    public void validate() {
        if (mqPackage == null || mqPackage.isBlank()) {
            log.warn("mqPackage 配置为空，已自动调整为 rabbitmq:");
            mqPackage = "rabbitmq:";
        }
        if (lockStatusProcessing == null || lockStatusProcessing.isBlank()) {
            log.warn("lockStatusProcessing 配置为空，已自动调整为 PROCESSING");
            lockStatusProcessing = "PROCESSING";
        }
        if (lockStatusProcessing.contains(":")) {
            log.warn("lockStatusProcessing[{}] 不允许包含冒号，已自动调整为 PROCESSING", lockStatusProcessing);
            lockStatusProcessing = "PROCESSING";
        }
        if (lockStatusDonePrefix == null || lockStatusDonePrefix.isBlank()) {
            log.warn("lockStatusDonePrefix 配置为空，已自动调整为 DONE:");
            lockStatusDonePrefix = "DONE:";
        }
        if (lockStatusDonePrefix.startsWith(lockStatusProcessing)) {
            log.warn("lockStatusDonePrefix[{}] 不能以 lockStatusProcessing[{}] 开头，已自动调整为 DONE:",
                    lockStatusDonePrefix, lockStatusProcessing);
            lockStatusDonePrefix = "DONE:";
        }
        if (delayedExchange == null || delayedExchange.isBlank()) {
            log.warn("delayedExchange 配置为空，已自动调整为 simple.delayed.retry.exchange");
            delayedExchange = "simple.delayed.retry.exchange";
        }
        if (fallbackDir == null || fallbackDir.isBlank()) {
            log.warn("fallbackDir 配置为空，已自动调整为系统临时目录");
            fallbackDir = System.getProperty("java.io.tmpdir") + File.separator + "simple-rabbitmq-fallback";
        }
        if (fallbackRetentionDays < 0) {
            log.warn("fallbackRetentionDays 配置为 {}，已自动调整为 30", fallbackRetentionDays);
            fallbackRetentionDays = 30;
        }
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
        // 钳制乘法溢出：retryDelayBaseMs 接近 Long.MAX/10 时直接封顶，避免 max 变负数破坏退避上限
        if (retryDelayBaseMs > Long.MAX_VALUE / 10) {
            log.warn("retryDelayBaseMs ({}) 过大，已钳制为 {}", retryDelayBaseMs, Long.MAX_VALUE / 10);
            retryDelayBaseMs = Long.MAX_VALUE / 10;
        }
        if (retryDelayMaxMs < retryDelayBaseMs) {
            log.warn("retryDelayMaxMs ({}) 小于 retryDelayBaseMs ({})，已自动调整为 {}",
                    retryDelayMaxMs, retryDelayBaseMs, retryDelayBaseMs * 10);
            retryDelayMaxMs = retryDelayBaseMs * 10;
        }
    }
}

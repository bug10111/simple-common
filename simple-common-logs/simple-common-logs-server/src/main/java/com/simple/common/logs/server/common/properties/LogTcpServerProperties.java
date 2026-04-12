package com.simple.common.logs.server.common.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * TCP日志服务端配置属性
 * <p>
 * 线程数的默认值根据服务器 CPU 核心数动态计算，适用于 I/O 密集型场景。
 * 公式：
 * - bossThreads = 1（始终固定，仅处理连接接入）
 * - workerThreads = CPU 核心数 × 2
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Configuration
@Validated
@ConfigurationProperties(prefix = "simple.logs.server")
public class LogTcpServerProperties {

    /**
     * 服务端监听端口
     */
    @Min(value = 1024, message = "端口号必须在 1024-65535 之间")
    @Max(value = 65535, message = "端口号必须在 1024-65535 之间")
    private int port = 9999;

    /**
     * Boss线程数（处理连接接入）
     * <p>
     * 默认值为 1，因为 accept 操作极快，单个线程足以应对大量连接。
     * </p>
     */
    @Min(value = 1, message = "Boss线程数最小为 1")
    @Max(value = 8, message = "Boss线程数最大为 8")
    private int bossThreads = 1;

    /**
     * Worker线程数（处理IO读写和业务逻辑）
     * <p>
     * 默认值根据 CPU 核心数动态计算：availableProcessors() × 2。
     * 适用于 I/O 密集型场景，兼顾上下文切换开销与吞吐量。
     * </p>
     */
    @Min(value = 2, message = "Worker线程数最小为 2")
    @Max(value = 128, message = "Worker线程数最大为 128")
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 连接超时时间（毫秒）
     */
    @Min(value = 1000, message = "连接超时时间最小为 1000ms")
    @Max(value = 120000, message = "连接超时时间最大为 120000ms")
    private int connectTimeout = 30000;

    /**
     * 最大连接数
     */
    @Min(value = 10, message = "最大连接数最小为 10")
    @Max(value = 50000, message = "最大连接数最大为 50000")
    private int maxConnections = 1000;

    /**
     * 日志批量处理间隔（毫秒）
     */
    @Min(value = 50, message = "批量处理间隔最小为 50ms")
    @Max(value = 10000, message = "批量处理间隔最大为 10000ms")
    private int batchInterval = 1000;

    /**
     * 日志批量处理大小
     */
    @Min(value = 10, message = "批量处理大小最小为 10")
    @Max(value = 10000, message = "批量处理大小最大为 10000")
    private int batchSize = 1000;

    /**
     * 读空闲超时时间（秒）
     */
    @Min(value = 5, message = "读空闲超时时间最小为 5 秒")
    @Max(value = 300, message = "读空闲超时时间最大为 300 秒")
    private int readerIdleTime = 10;

    // ==================== WAL 配置 ====================

    /**
     * 是否启用 WAL 预写日志
     */
    private boolean walEnabled = true;

    /**
     * WAL 存储目录
     */
    @NotNull(message = "WAL 存储目录不能为空")
    private String walDir = "./logs_wal";

    /**
     * WAL 文件最大大小(字节)，超过则轮转
     */
    @Min(value = 1048576, message = "WAL 文件最大大小最小为 1MB (1048576 字节)")
    @Max(value = 1073741824, message = "WAL 文件最大大小最大为 1GB (1073741824 字节)")
    private long walFileMaxSize = 50 * 1024 * 1024;

    /**
     * 是否启用 WAL 自动恢复（定时扫描已轮转的 WAL 文件并重试持久化）
     */
    private boolean walRecoveryEnabled = true;

    /**
     * WAL 恢复扫描间隔（毫秒）
     */
    @Min(value = 5000, message = "WAL 恢复扫描间隔最小为 5000ms")
    @Max(value = 300000, message = "WAL 恢复扫描间隔最大为 300000ms")
    private int walRecoveryInterval = 60000;

    // ==================== 死信队列配置 ====================

    /**
     * 是否启用死信队列（解析失败或多次恢复失败的日志存放处）
     */
    private boolean deadLetterEnabled = true;

    /**
     * 死信队列存储目录
     */
    @NotNull(message = "死信队列存储目录不能为空")
    private String deadLetterDir = "./logs_deadletter";
}
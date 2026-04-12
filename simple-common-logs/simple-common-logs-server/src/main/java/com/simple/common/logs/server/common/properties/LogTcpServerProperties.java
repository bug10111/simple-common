package com.simple.common.logs.server.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
@ConfigurationProperties(prefix = "simple.logs.server")
public class LogTcpServerProperties {

    /**
     * 服务端监听端口
     */
    private int port = 9999;

    /**
     * Boss线程数（处理连接接入）
     * <p>
     * 默认值为 1，因为 accept 操作极快，单个线程足以应对大量连接。
     * </p>
     */
    private int bossThreads = 1;

    /**
     * Worker线程数（处理IO读写和业务逻辑）
     * <p>
     * 默认值根据 CPU 核心数动态计算：availableProcessors() × 2。
     * 适用于 I/O 密集型场景，兼顾上下文切换开销与吞吐量。
     * </p>
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 30000;

    /**
     * 最大连接数
     */
    private int maxConnections = 1000;

    /**
     * 日志批量处理间隔（毫秒）
     */
    private int batchInterval = 1000;

    /**
     * 日志批量处理大小
     */
    private int batchSize = 1000;

    /**
     * 读空闲超时时间（秒）
     */
    private int readerIdleTime = 10;

    // ==================== WAL 配置 ====================

    /**
     * 是否启用 WAL 预写日志
     */
    private boolean walEnabled = true;

    /**
     * WAL 存储目录
     */
    private String walDir = "./logs_wal";

    /**
     * WAL 文件最大大小(字节)，超过则轮转
     */
    private long walFileMaxSize = 50 * 1024 * 1024;

    /**
     * 是否启用 WAL 自动恢复（定时扫描已轮转的 WAL 文件并重试持久化）
     */
    private boolean walRecoveryEnabled = true;

    /**
     * WAL 恢复扫描间隔（毫秒）
     */
    private int walRecoveryInterval = 60000;

    // ==================== 死信队列配置 ====================

    /**
     * 是否启用死信队列（解析失败或多次恢复失败的日志存放处）
     */
    private boolean deadLetterEnabled = true;

    /**
     * 死信队列存储目录
     */
    private String deadLetterDir = "./logs_deadletter";
}
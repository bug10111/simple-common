package com.simple.common.logs.client.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TCP日志客户端配置属性
 * <p>
 * 线程数的默认值根据服务器 CPU 核心数动态计算，适用于 I/O 密集型场景。
 * 公式：workerThreads = max(2, availableProcessors())
 * 保证最小 2 个线程，避免单核环境下性能过低。
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.logs.client")
public class LogTcpClientProperties {

    /**
     * 服务端主机地址
     */
    private String host = "127.0.0.1";

    /**
     * 服务端端口
     */
    private int port = 9999;

    /**
     * 连接超时时间(毫秒)
     */
    private int connectTimeout = 5000;

    /**
     * 重连间隔时间(毫秒)
     */
    private int reconnectInterval = 5000;

    /**
     * 最大重连次数，-1表示无限重连
     */
    private int maxReconnectAttempts = -1;

    /**
     * 工作线程数（Netty EventLoopGroup 线程数）
     * <p>
     * 默认值根据 CPU 核心数动态计算：max(2, availableProcessors())。
     * 客户端主要处理网络 I/O 发送，属于 I/O 密集型操作，适当线程数可提升并发发送能力。
     * 最小保证 2 个线程，避免单核环境下性能不足。
     * </p>
     */
    private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * 缓冲队列容量，默认10000
     */
    private int bufferCapacity = 10000;

    /**
     * 批量发送大小，默认100条
     */
    private int batchSize = 100;

    /**
     * 批量发送最大等待时间(毫秒)，即使未达到batchSize也会发送
     */
    private int batchFlushInterval = 500;

    /**
     * 发送失败后是否启用本地文件降级存储
     */
    private boolean fallbackEnabled = true;

    /**
     * 降级存储目录
     */
    private String fallbackDir = "./logs_fallback";

    /**
     * 降级文件最大大小(字节)，超过则轮转
     */
    private long fallbackFileMaxSize = 10 * 1024 * 1024;

    /**
     * 恢复后是否自动重发降级日志
     */
    private boolean fallbackResendEnabled = true;
}
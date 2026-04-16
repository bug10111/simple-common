package com.simple.common.logs.client.common.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

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
@Validated
@ConfigurationProperties(prefix = "simple.logs.client")
public class LogTcpClientProperties {

    /**
     * 服务端主机地址
     */
    @NotNull(message = "服务端主机地址不能为空")
    private String host = "127.0.0.1";

    /**
     * 服务端端口
     */
    @Min(value = 1024, message = "端口号必须在 1024-65535 之间")
    @Max(value = 65535, message = "端口号必须在 1024-65535 之间")
    private int port = 9999;

    /**
     * 连接超时时间(毫秒)
     */
    @Min(value = 1000, message = "连接超时时间最小为 1000ms")
    @Max(value = 60000, message = "连接超时时间最大为 60000ms")
    private int connectTimeout = 5000;

    /**
     * 重连间隔时间(毫秒)
     */
    @Min(value = 1000, message = "重连间隔时间最小为 1000ms")
    @Max(value = 60000, message = "重连间隔时间最大为 60000ms")
    private int reconnectInterval = 5000;

    /**
     * 最大重连次数，-1表示无限重连
     */
    @Min(value = -1, message = "最大重连次数最小为 -1")
    @Max(value = 1000, message = "最大重连次数最大为 1000")
    private int maxReconnectAttempts = -1;

    /**
     * 工作线程数（Netty EventLoopGroup 线程数）
     * <p>
     * 默认值根据 CPU 核心数动态计算：max(2, availableProcessors())。
     * 客户端主要处理网络 I/O 发送，属于 I/O 密集型操作，适当线程数可提升并发发送能力。
     * 最小保证 2 个线程，避免单核环境下性能不足。
     * </p>
     */
    @Min(value = 1, message = "工作线程数最小为 1")
    @Max(value = 64, message = "工作线程数最大为 64")
    private int workerThreads = Math.max(2, Runtime.getRuntime().availableProcessors());

    /**
     * 缓冲队列容量，默认10000
     */
    @Min(value = 100, message = "缓冲队列容量最小为 100")
    @Max(value = 100000, message = "缓冲队列容量最大为 100000")
    private int bufferCapacity = 10000;

    /**
     * 批量发送大小，默认100条
     */
    @Min(value = 10, message = "批量发送大小最小为 10")
    @Max(value = 10000, message = "批量发送大小最大为 10000")
    private int batchSize = 100;

    /**
     * 批量发送最大等待时间(毫秒)，即使未达到batchSize也会发送
     */
    @Min(value = 50, message = "批量发送最大等待时间最小为 50ms")
    @Max(value = 5000, message = "批量发送最大等待时间最大为 5000ms")
    private int batchFlushInterval = 500;

    /**
     * 发送失败后是否启用本地文件降级存储
     */
    private boolean fallbackEnabled = true;

    /**
     * 降级存储目录
     */
    @NotNull(message = "降级存储目录不能为空")
    private String fallbackDir = "./logs_fallback";

    /**
     * 降级文件最大大小(字节)，超过则轮转
     */
    @Min(value = 1048576, message = "降级文件最大大小最小为 1MB (1048576 字节)")
    @Max(value = 1073741824, message = "降级文件最大大小最大为 1GB (1073741824 字节)")
    private long fallbackFileMaxSize = 10 * 1024 * 1024;

    /**
     * 恢复后是否自动重发降级日志
     */
    private boolean fallbackResendEnabled = true;

    /**
     * 降级恢复时，每次扫描最多处理的文件数量，避免瞬间重发大量文件造成压力。
     * 设置为 -1 表示不限制。
     */
    @Min(value = -1, message = "降级恢复单次处理文件数最小为 -1")
    private int fallbackResendMaxFilesPerRound = 5;

    /**
     * 请求体缓存最大大小（字节），超过此大小的请求体将不会被缓存，避免内存溢出
     * 默认 1MB
     */
    @Min(value = 1024, message = "请求体缓存最大大小最小为 1024 字节")
    @Max(value = 104857600, message = "请求体缓存最大大小最大为 100MB")
    private long maxBodyCacheSize = 1024 * 1024; // 1MB

    /**
     * 降级文件保留天数，超过该天数的降级文件（未成功发送）将被直接删除
     * 默认 7 天，设为 -1 表示永不过期
     */
    @Min(value = -1, message = "降级文件保留天数最小为 -1")
    private int fallbackFileRetentionDays = 7;
}
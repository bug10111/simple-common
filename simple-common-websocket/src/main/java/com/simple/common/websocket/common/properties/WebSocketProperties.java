package com.simple.common.websocket.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "simple.websocket")
public class WebSocketProperties {

    /**
     * WebSocket服务端口
     */
    private int port = 5558;

    /**
     * WebSocket握手路径
     */
    private String path = "/simple";

    /**
     * 接收连接的线程数
     */
    private int bossThreads = 1;

    /**
     * 工作线程数
     */
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 等待连接队列长度
     */
    private int soBacklog = 1024;

    /**
     * 写缓冲区高水位
     */
    private int writeBufferHigh = 64 * 1024;

    /**
     * 写缓冲区低水位
     */
    private int writeBufferLow = 32 * 1024;

    /**
     * 读空闲超时时间（秒）
     */
    private int readerIdleTime = 120;

    /**
     * 写空闲超时时间（秒）
     */
    private int writerIdleTime = 0;

    /**
     * 全空闲超时时间（秒）
     */
    private int allIdleTime = 0;

    /**
     * HTTP消息最大长度（字节）
     */
    private int maxHttpContentLength = 1048576;

    /**
     * WebSocket消息帧最大长度（字节），默认1MB，可通过 simple.websocket.max-web-socket-frame-size 配置
     */
    private int maxWebSocketFrameSize = 1048576;

    /**
     * 文本消息最大长度（字节），默认1MB，超过此长度将拒绝处理，可通过 simple.websocket.max-text-message-length 配置
     */
    private int maxTextMessageLength = 1048576;

    /**
     * 通道清理配置
     */
    private CleanConfig clean = new CleanConfig();

    /**
     * 同步请求配置
     */
    private SyncConfig sync = new SyncConfig();

    /**
     * 通道清理配置类
     */
    @Getter
    @Setter
    public static class CleanConfig {
        /**
         * 是否启用无效通道清理任务
         */
        private boolean enabled = true;

        /**
         * 清理间隔时间（秒）
         */
        private long interval = 60;

        /**
         * 初始延迟时间（秒）
         */
        private long initialDelay = 30;
    }

    /**
     * 同步请求配置类
     */
    @Getter
    @Setter
    public static class SyncConfig {
        /**
         * 同步请求清理间隔时间（秒）
         */
        private long cleanInterval = 30;

        /**
         * 同步请求清理初始延迟时间（秒）
         */
        private long initialDelay = 30;

        /**
         * 同步请求默认超时时间（秒）
         */
        private long timeout = 30;
    }
}
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
    private int maxHttpContentLength = 65536;

    /**
     * WebSocket消息最大长度（字节）
     */
    private int maxWebSocketFrameSize = 655360;

    /**
     * 文本消息最大长度（字节），超过此长度将拒绝处理
     */
    private int maxTextMessageLength = 1024 * 1024;

    /**
     * 通道清理配置
     */
    private CleanConfig clean = new CleanConfig();

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
}
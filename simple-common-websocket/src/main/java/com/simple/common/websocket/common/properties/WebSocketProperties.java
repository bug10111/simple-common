package com.simple.common.websocket.common.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * WebSocket 配置属性。
 * <p>
 * 所有数值项均带下界校验（{@code @Validated} + JSR-380），
 * 配置错误（负线程数、越界端口、非法水位等）在属性绑定阶段快速失败，
 * 而不是在 {@code ApplicationReadyEvent} 阶段以晦涩的运行时异常出现。
 * </p>
 *
 * @author qty
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "simple.websocket")
public class WebSocketProperties {

    /**
     * WebSocket服务端口（1-65535）
     */
    @Min(value = 1, message = "simple.websocket.port 必须 >= 1")
    @Max(value = 65535, message = "simple.websocket.port 必须 <= 65535")
    private int port = 5558;

    /**
     * WebSocket握手路径（不可空白）
     */
    @NotBlank(message = "simple.websocket.path 不可为空白")
    private String path = "/simple";

    /**
     * 接收连接的线程数（>= 1）
     */
    @Min(value = 1, message = "simple.websocket.boss-threads 必须 >= 1")
    private int bossThreads = 1;

    /**
     * 工作线程数（>= 1）
     */
    @Min(value = 1, message = "simple.websocket.worker-threads 必须 >= 1")
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 等待连接队列长度（>= 1）
     */
    @Min(value = 1, message = "simple.websocket.so-backlog 必须 >= 1")
    private int soBacklog = 1024;

    /**
     * 写缓冲区高水位（字节，>= 1 且 >= 低水位）
     */
    @Min(value = 1, message = "simple.websocket.write-buffer-high 必须 >= 1")
    private int writeBufferHigh = 64 * 1024;

    /**
     * 写缓冲区低水位（字节，>= 0 且 <= 高水位）
     */
    @Min(value = 0, message = "simple.websocket.write-buffer-low 必须 >= 0")
    private int writeBufferLow = 32 * 1024;

    /**
     * 写缓冲区水位约束校验：Netty 要求 low >= 0 且 high >= low，
     * 违反时 setOption 抛 IllegalArgumentException 导致启动失败，故在绑定阶段校验
     */
    @AssertTrue(message = "simple.websocket.write-buffer-low 不可大于 write-buffer-high")
    public boolean isWriteBufferWaterMarkValid() {
        return writeBufferLow >= 0 && writeBufferHigh >= writeBufferLow;
    }

    /**
     * 读空闲超时时间（秒，>= 0，0 表示不检测）
     */
    @Min(value = 0, message = "simple.websocket.reader-idle-time 必须 >= 0")
    private int readerIdleTime = 120;

    /**
     * 写空闲超时时间（秒，>= 0，0 表示不检测）
     */
    @Min(value = 0, message = "simple.websocket.writer-idle-time 必须 >= 0")
    private int writerIdleTime = 0;

    /**
     * 全空闲超时时间（秒，>= 0，0 表示不检测）
     */
    @Min(value = 0, message = "simple.websocket.all-idle-time 必须 >= 0")
    private int allIdleTime = 0;

    /**
     * HTTP消息最大长度（字节，>= 1，为 0 时 HTTP 升级请求将全部被拒绝）
     */
    @Min(value = 1, message = "simple.websocket.max-http-content-length 必须 >= 1")
    private int maxHttpContentLength = 1048576;

    /**
     * WebSocket消息帧最大长度（字节，>= 1），默认1MB，可通过 simple.websocket.max-web-socket-frame-size 配置；
     * Netty 帧解码层以此为上限兜底
     */
    @Min(value = 1, message = "simple.websocket.max-web-socket-frame-size 必须 >= 1")
    private int maxWebSocketFrameSize = 1048576;

    /**
     * 文本消息最大长度（字节口径，按 UTF-8 编码字节数比较，>= 1），默认1MB，超过此长度将拒绝处理，
     * 可通过 simple.websocket.max-text-message-length 配置；
     * Netty 帧解码层另有 max-web-socket-frame-size 字节上限兜底
     */
    @Min(value = 1, message = "simple.websocket.max-text-message-length 必须 >= 1")
    private int maxTextMessageLength = 1048576;

    /**
     * 鉴权配置
     */
    @Valid
    private AuthConfig auth = new AuthConfig();

    /**
     * 通道清理配置
     */
    @Valid
    private CleanConfig clean = new CleanConfig();

    /**
     * 同步请求配置
     */
    @Valid
    private SyncConfig sync = new SyncConfig();

    /**
     * 鉴权配置类
     */
    @Getter
    @Setter
    public static class AuthConfig {

        /**
         * 宽松鉴权开关（simple.websocket.auth.permissive，默认 false）。
         * <p>
         * 默认实现 {@link com.simple.common.websocket.manager.DefaultCheckWebSocketManager}
         * 未接入真实鉴权时采用 fail-closed（拒绝所有连接）。
         * 仅当部署方显式声明放弃鉴权（设为 true）时，默认实现才放行携带非空 token 的连接
         * （空 token 仍然拒绝）。生产环境应提供真实 CheckWebSocketManager 实现而非开启本开关。
         * </p>
         */
        private boolean permissive = false;
    }

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
         * 清理间隔时间（秒，>= 1）
         */
        @Min(value = 1, message = "simple.websocket.clean.interval 必须 >= 1")
        private long interval = 60;

        /**
         * 初始延迟时间（秒，>= 0）
         */
        @Min(value = 0, message = "simple.websocket.clean.initial-delay 必须 >= 0")
        private long initialDelay = 30;
    }

    /**
     * 同步请求配置类
     */
    @Getter
    @Setter
    public static class SyncConfig {

        /**
         * 同步请求清理间隔时间（秒，>= 1）
         */
        @Min(value = 1, message = "simple.websocket.sync.clean-interval 必须 >= 1")
        private long cleanInterval = 30;

        /**
         * 同步请求清理初始延迟时间（秒，>= 0）
         */
        @Min(value = 0, message = "simple.websocket.sync.initial-delay 必须 >= 0")
        private long initialDelay = 30;

        /**
         * 同步请求默认超时时间（秒，>= 1；为 0 或负数会导致所有同步请求立即失败）
         */
        @Min(value = 1, message = "simple.websocket.sync.timeout 必须 >= 1")
        private long timeout = 30;
    }
}

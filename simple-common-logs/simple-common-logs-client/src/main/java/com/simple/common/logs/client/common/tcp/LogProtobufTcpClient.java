package com.simple.common.logs.client.common.tcp;

import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import com.simple.common.logs.proto.LogBatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Protobuf TCP 日志客户端（Fire-and-Forget 模式）
 * 基于 Netty 实现，使用 Protobuf 进行序列化（仅支持批量发送）
 * <p>
 * 为追求极致性能，本客户端不处理服务端响应，仅单向发送日志数据。
 * </p>
 * <p>
 * 修改说明：增加发送失败回调机制，解决原 sendBatch 返回值无法准确反映发送结果的问题。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class LogProtobufTcpClient {

    /**
     * 发送失败监听器接口
     */
    @FunctionalInterface
    public interface SendFailureListener {
        /**
         * 当批量日志发送失败时调用
         *
         * @param batch 发送失败的批量日志
         */
        void onSendFailure(LogBatch batch);
    }

    @Autowired
    private LogTcpClientProperties logTcpClientProperties;

    private EventLoopGroup group;

    private volatile Channel channel;

    private Bootstrap bootstrap;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    /**
     * 启动客户端
     * <p>
     * 该方法在 Bean 初始化完成后自动调用，异步启动 TCP 连接，
     * 避免阻塞主线程影响应用启动速度。
     * </p>
     */
    @PostConstruct
    public void start() {
        connect();
    }

    /**
     * 初始化并建立 TCP 连接
     * <p>
     * 配置 Netty Bootstrap，包括线程组、通道类型、TCP 参数和处理器管道。
     * 处理器管道包含心跳检测、Protobuf 编码器和简化的业务处理器（无响应处理）。
     * </p>
     */
    private void connect() {
        group = new NioEventLoopGroup(logTcpClientProperties.getWorkerThreads());
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, logTcpClientProperties.getConnectTimeout())
                // 写缓冲区高低水位，防止 OOM
                .option(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 64 * 1024)
                .option(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, 32 * 1024)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 心跳检测
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 30000, 0, TimeUnit.MILLISECONDS));

                        // Protobuf 编码器（仅编码 LogBatch）
                        pipeline.addLast("encoder", new LogProtobufEncoder());

                        // 注意：Fire-and-Forget 模式下无需解码器，所有入站数据将被忽略
                        // 业务处理器（仅处理异常和心跳）
                        pipeline.addLast("handler", new LogProtobufClientHandler());
                    }
                });

        doConnect();
    }

    /**
     * 执行实际的连接操作
     * <p>
     * 异步连接服务器，成功后更新连接状态并注册断开重连监听器，
     * 失败则触发重连机制。
     * </p>
     */
    private void doConnect() {
        if (!running.get()) {
            return;
        }
        bootstrap.connect(logTcpClientProperties.getHost(), logTcpClientProperties.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                connected.set(true);
                reconnectAttempts.set(0);
                log.info("Protobuf TCP 日志客户端启动成功, 连接到 {}:{}", logTcpClientProperties.getHost(), logTcpClientProperties.getPort());
                // 添加断开重连监听
                channel.closeFuture().addListener(closeFuture -> {
                    connected.set(false);
                    log.warn("日志连接断开，尝试重连...");
                    scheduleReconnect();
                });
            } else {
                log.error("连接失败: {}", future.cause().getMessage());
                scheduleReconnect();
            }
        });
    }

    /**
     * 调度重连任务
     * <p>
     * 根据配置的最大重连次数和重连间隔，延迟执行重连操作。
     * 若已达到最大重连次数则停止重连。
     * 使用 reconnecting 标志防止重复调度。
     * </p>
     */
    private void scheduleReconnect() {
        if (!running.get()) {
            return;
        }
        // 防御性检查：防止 group 未初始化导致的 NPE
        if (group == null) {
            log.warn("EventLoopGroup 尚未初始化，放弃本次重连调度");
            return;
        }
        // 防止重复调度
        if (!reconnecting.compareAndSet(false, true)) {
            log.debug("已有重连任务正在调度，跳过本次请求");
            return;
        }

        if (logTcpClientProperties.getMaxReconnectAttempts() != 0) {
            int max = logTcpClientProperties.getMaxReconnectAttempts();
            int attempts = reconnectAttempts.get();
            if (max > 0 && attempts >= max) {
                log.error("已达最大重连次数 {}，不再重连", max);
                reconnecting.set(false);
                return;
            }
            reconnectAttempts.incrementAndGet();
        }
        group.schedule(() -> {
            reconnecting.set(false);
            doConnect();
        }, logTcpClientProperties.getReconnectInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 发送批量日志（LogBatch 协议），不关心失败回调
     * <p>
     * 将多条日志合并为一个 LogBatch 消息发送，减少网络包和编解码开销。
     * 采用 Fire-and-Forget 模式，不等待也不处理服务端响应。
     * 若需处理发送失败，请使用带监听器的方法。
     * </p>
     *
     * @param logBatch 批量日志容器
     * @return true-写操作已提交，false-通道未激活（不代表最终发送成功）
     * @deprecated 建议使用 {@link #sendBatch(LogBatch, SendFailureListener)} 以便准确处理失败
     */
    @Deprecated
    public boolean sendBatch(LogBatch logBatch) {
        return sendBatch(logBatch, null);
    }

    /**
     * 发送批量日志（LogBatch 协议），支持失败回调
     * <p>
     * 将多条日志合并为一个 LogBatch 消息发送，减少网络包和编解码开销。
     * 若提供监听器，当写操作失败时（如连接断开）会回调监听器执行降级操作。
     * 注意：回调可能在 Netty I/O 线程中执行，请确保回调逻辑非阻塞且快速返回。
     * </p>
     *
     * @param logBatch 批量日志容器
     * @param listener 发送失败监听器，可为 null
     * @return true-写操作已提交，false-通道未激活
     */
    public boolean sendBatch(LogBatch logBatch, SendFailureListener listener) {
        Channel ch = this.channel; // 读取 volatile 变量到本地变量，减少 volatile 访问次数
        if (ch != null && ch.isActive()) {
            ChannelFuture future = ch.writeAndFlush(logBatch);
            if (listener != null) {
                future.addListener((ChannelFutureListener) f -> {
                    if (!f.isSuccess()) {
                        log.error("批量日志发送失败: {}", f.cause().getMessage());
                        // 在 I/O 线程中直接回调，调用方需保证非阻塞
                        listener.onSendFailure(logBatch);
                    } else {
                        log.debug("批量日志发送成功, 数量: {}", logBatch.getLogsCount());
                    }
                });
            }
            return true;
        } else {
            log.warn("通道未激活，无法发送批量日志");
            // 通道不可用时立即回调（在调用线程中执行）
            if (listener != null) {
                listener.onSendFailure(logBatch);
            }
            return false;
        }
    }

    /**
     * 判断连接是否活跃
     * <p>
     * 通过连接状态标记和通道状态双重判断，确保连接真实可用。
     * </p>
     *
     * @return true 表示连接活跃，false 表示连接不可用
     */
    public boolean isActive() {
        Channel ch = this.channel;
        return connected.get() && ch != null && ch.isActive();
    }

    /**
     * 关闭客户端
     * <p>
     * 该方法在 Bean 销毁前自动调用，依次关闭通道和释放线程组资源。
     * </p>
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        Channel ch = this.channel;
        if (ch != null && ch.isActive()) {
            ch.close();
        }
        if (group != null) {
            try {
                // 优雅关闭，等待最多 5 秒
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("等待 Netty 线程组关闭时被中断");
            }
        }
        log.info("Protobuf TCP 日志客户端已关闭");
    }
}
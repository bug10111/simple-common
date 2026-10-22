package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Protobuf TCP 日志服务端（Fire-and-Forget 模式）
 * 基于 Netty 实现，使用 Protobuf 进行序列化
 * 具备异常恢复能力：绑定失败或通道关闭后自动重试
 * <p>
 * 为追求极致性能，本服务端不发送任何响应给客户端。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class LogProtobufTcpServer {

    /**
     * 端口绑定失败最大重试次数（有界重试 + 指数退避，超限后放弃并明确暴露错误）
     */
    private static final int MAX_BIND_ATTEMPTS = 5;

    private final LogTcpServerProperties properties;

    private final LogsSaveManager logsSaveManager;

    public LogProtobufTcpServer(LogTcpServerProperties properties, LogsSaveManager logsSaveManager) {
        this.properties = properties;
        this.logsSaveManager = logsSaveManager;
    }

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    /**
     * volatile：由 Netty IO 线程（绑定回调）写入，Spring 停机线程在 shutdown() 中读取，
     * 需要保证可见性与引用完整性
     */
    private volatile Channel serverChannel;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 日志保存业务线程池：IO 线程只做解码，持久化（序列化/WAL 磁盘 IO）全部转移到此执行
     */
    private ExecutorService saveExecutor;

    /**
     * 端口绑定失败已尝试次数（用于有界重试）
     */
    private final AtomicInteger bindAttempts = new AtomicInteger(0);

    /**
     * 当前活跃连接计数
     * <p>
     * 跨连接共享，配合最大连接数配置实现服务端连接数限流。
     * </p>
     */
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    /**
     * 启动服务端
     * <p>
     * 该方法在 Bean 初始化完成后自动调用，异步启动 TCP 服务，
     * 并支持启动失败后的自动重试。
     * </p>
     */
    @PostConstruct
    public void start() {
        if (logsSaveManager == null) {
            throw new IllegalStateException("未找到 LogsSaveManager 实现，请提供一个 Bean");
        }
        // 持久化业务线程池：有界队列 + 饱和丢弃并告警（日志为尽力而为语义），
        // 防止 IO 线程被序列化/磁盘 IO 拖垮，同时保证内存有界
        int coreThreads = Math.max(2, properties.getWorkerThreads() / 2);
        saveExecutor = new ThreadPoolExecutor(coreThreads, coreThreads, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                r -> {
                    Thread t = new Thread(r, "log-server-save");
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> log.error("日志保存线程池已饱和，拒绝 1 个批次保存任务"));
        ((ThreadPoolExecutor) saveExecutor).allowCoreThreadTimeOut(true);
        running.set(true);
        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());
        doBind();
    }

    /**
     * 执行端口绑定操作
     * <p>
     * 构建 ServerBootstrap 并尝试绑定端口。
     * 绑定成功则设置 closeFuture 监听器以便在通道关闭时自动重新绑定；
     * 绑定失败则延迟后重试。
     * </p>
     */
    private void doBind() {
        if (!running.get()) {
            return;
        }

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                 .channel(NioServerSocketChannel.class)
                 .option(ChannelOption.SO_BACKLOG, 128)
                 .option(ChannelOption.SO_REUSEADDR, true)
                 .childOption(ChannelOption.SO_KEEPALIVE, true)
                 .childOption(ChannelOption.TCP_NODELAY, true)
                 .childHandler(new ChannelInitializer<SocketChannel>() {
                     @Override
                     protected void initChannel(SocketChannel ch) throws Exception {
                         ChannelPipeline pipeline = ch.pipeline();

                         // 连接数限流：活跃连接超过最大连接数时立即拒绝新连接
                         pipeline.addLast("connectionLimitHandler", new ConnectionLimitHandler());

                         // 心跳检测
                         pipeline.addLast("idleStateHandler", new IdleStateHandler(properties.getReaderIdleTime(), 0, 0, TimeUnit.SECONDS));

                         // 空闲事件处理：读空闲判定为半开/失活连接，直接关闭释放资源
                         // （修复 IdleStateHandler 事件无人消费导致空闲连接永不回收、连接数被占满的问题）
                         pipeline.addLast("idleCloseHandler", new IdleCloseHandler());

                         // Protobuf 解码器
                         pipeline.addLast("decoder", new LogProtobufDecoder());

                         // 注意：Fire-and-Forget 模式下无需编码器，不发送任何响应

                         // 业务处理器（内部转交业务线程池做持久化，不阻塞 IO 线程）
                         pipeline.addLast("handler", new LogProtobufServerHandler(logsSaveManager, saveExecutor));
                     }
                 });

        // 异步绑定端口
        ChannelFuture bindFuture = bootstrap.bind(properties.getPort());
        bindFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                bindAttempts.set(0);
                if (!running.get()) {
                    // 绑定期间已收到关闭指令：立即释放新绑定的通道，避免端口泄漏
                    log.info("日志服务端启动期间已停机，释放新绑定端口 {}", properties.getPort());
                    future.channel().close();
                    return;
                }
                serverChannel = future.channel();
                log.info("Protobuf TCP 日志服务端启动成功, 监听端口: {} (Fire-and-Forget 模式)", properties.getPort());

                // 监听通道关闭事件，触发重新绑定
                serverChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.warn("服务端通道已关闭，尝试重新绑定...");
                    if (running.get()) {
                        // 延迟一段时间后重新绑定
                        scheduleRebind(5);
                    }
                });
            } else {
                int attempts = bindAttempts.incrementAndGet();
                log.error("服务端绑定端口 {} 失败（第 {} 次）: {}", properties.getPort(), attempts, future.cause().getMessage());
                if (running.get()) {
                    if (attempts >= MAX_BIND_ATTEMPTS) {
                        // 有界重试：超过上限后放弃，明确暴露错误状态，避免无限静默重试
                        log.error("日志服务端连续 {} 次绑定端口 {} 失败，放弃重试；请检查端口占用/配置后重启应用",
                                attempts, properties.getPort());
                        return;
                    }
                    // 绑定失败，指数退避重试（10s, 20s, 40s... 上限 300s）
                    long delaySeconds = Math.min(10L << (attempts - 1), 300);
                    scheduleRebind(delaySeconds);
                }
            }
        });
    }

    /**
     * 延迟重新绑定。
     * <p>
     * 使用 try/catch 包裹：shutdownGracefully 进行中的 workerGroup 拒绝调度任务时
     * 不允许 RejectedExecutionException 以未捕获监听器异常形式逃逸。
     * </p>
     */
    private void scheduleRebind(long delaySeconds) {
        try {
            workerGroup.schedule(this::doBind, delaySeconds, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            log.warn("线程组正在关闭，重新绑定调度被拒绝，忽略: {}", e.getMessage());
        }
    }

    /**
     * 关闭服务端
     * <p>
     * 该方法在 Bean 销毁前自动调用，依次关闭通道和释放线程组资源。
     * </p>
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (serverChannel != null) {
            // 有界等待关闭完成，避免停机流程被长时间阻塞
            serverChannel.close().awaitUninterruptibly(3000);
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
        if (saveExecutor != null) {
            saveExecutor.shutdown();
            try {
                if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    saveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                saveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Protobuf TCP 日志服务端已关闭");
    }

    /**
     * 连接数限流处理器
     * <p>
     * 连接建立时递增活跃连接计数，超过最大连接数则立即关闭新连接并记录 warn 日志；
     * 连接断开时递减计数，保证计数与实际活跃连接数一致。
     * </p>
     */
    private class ConnectionLimitHandler extends ChannelInboundHandlerAdapter {

        /**
         * 是否已向下游转发 channelActive：
         * 被拒绝的连接不转发 active 事件，其 inactive 事件同样不向下游传播，
         * 保证下游 handler 与连接计数的状态对称
         */
        private boolean activeForwarded = false;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            int current = activeConnections.incrementAndGet();
            if (current > properties.getMaxConnections()) {
                // 超过最大连接数，拒绝新连接并保留可观测日志
                log.warn("活跃连接数 {} 已超过最大连接数 {}，拒绝新连接: {}", current, properties.getMaxConnections(), ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            activeForwarded = true;
            ctx.fireChannelActive();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            activeConnections.decrementAndGet();
            if (activeForwarded) {
                ctx.fireChannelInactive();
            }
        }
    }

    /**
     * 读空闲断连处理器。
     * <p>
     * Fire-and-Forget 模式下服务端不发送任何响应，读空闲即代表客户端已失联，
     * 直接关闭连接释放资源（修复 IdleStateHandler 事件无下游消费、空闲连接
     * 永不回收直到占满 maxConnections 的问题）。
     * </p>
     */
    @Slf4j
    private static final class IdleCloseHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                log.debug("读空闲超时，关闭失活连接: {}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            super.userEventTriggered(ctx, evt);
        }
    }
}
package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
@Configuration
public class LogProtobufTcpServer {

    @Autowired
    private LogTcpServerProperties properties;

    @Autowired
    private LogsSaveManager logsSaveManager;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Channel serverChannel;

    private final AtomicBoolean running = new AtomicBoolean(false);

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

                         // 心跳检测
                         pipeline.addLast("idleStateHandler", new IdleStateHandler(properties.getReaderIdleTime(), 0, 0, TimeUnit.SECONDS));

                         // Protobuf 解码器
                         pipeline.addLast("decoder", new LogProtobufDecoder());

                         // 注意：Fire-and-Forget 模式下无需编码器，不发送任何响应

                         // 业务处理器
                         pipeline.addLast("handler", new LogProtobufServerHandler(logsSaveManager));
                     }
                 });

        // 异步绑定端口
        ChannelFuture future = bootstrap.bind(properties.getPort());
        future.addListener((ChannelFutureListener) bindFuture -> {
            if (bindFuture.isSuccess()) {
                serverChannel = bindFuture.channel();
                log.info("Protobuf TCP 日志服务端启动成功, 监听端口: {} (Fire-and-Forget 模式)", properties.getPort());

                // 监听通道关闭事件，触发重新绑定
                serverChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.warn("服务端通道已关闭，尝试重新绑定...");
                    if (running.get()) {
                        // 延迟一段时间后重新绑定
                        workerGroup.schedule(this::doBind, 5, TimeUnit.SECONDS);
                    }
                });
            } else {
                log.error("服务端绑定端口 {} 失败: {}", properties.getPort(), bindFuture.cause().getMessage());
                if (running.get()) {
                    // 绑定失败，延迟重试
                    workerGroup.schedule(this::doBind, 10, TimeUnit.SECONDS);
                }
            }
        });
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
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Protobuf TCP 日志服务端已关闭");
    }
}
package com.simple.common.websocket.init;

import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.initializer.WebSocketChannelInitializer;
import com.simple.common.websocket.utils.WebSocketUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket服务启动器
 *
 * @author qty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketStartInit {

    private final WebSocketProperties properties;
    private final WebSocketListeningManager webSocketListeningManager;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private volatile boolean running = false;

    /**
     * 启动WebSocket服务
     */
    public void start() {
        if (running) {
            log.warn("WebSocket服务已在运行中");
            return;
        }

        log.info("WebSocket服务启动中... [port={}, path={}]", properties.getPort(), properties.getPath());

        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, properties.getSoBacklog())
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, properties.getWriteBufferHigh())
                    .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, properties.getWriteBufferLow())
                    .childHandler(new WebSocketChannelInitializer(properties, webSocketListeningManager));

            ChannelFuture future = bootstrap.bind(properties.getPort()).sync();
            running = true;
            log.info("WebSocket服务启动成功 [port={}, path={}, bossThreads={}, workerThreads={}]",
                    properties.getPort(), properties.getPath(),
                    properties.getBossThreads(), properties.getWorkerThreads());

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("WebSocket服务启动被中断", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("WebSocket服务启动失败", e);
        } finally {
            destroy();
        }
    }

    /**
     * 关闭WebSocket服务
     */
    @PreDestroy
    public void destroy() {
        if (!running) {
            return;
        }

        log.info("WebSocket服务关闭中...");
        running = false;

        // 清理所有通道
        WebSocketUtils.clearAll();

        // 优雅关闭EventLoopGroup
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(properties.getShutdownQuietPeriod(),
                    properties.getShutdownTimeOut(), TimeUnit.MILLISECONDS);
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(properties.getShutdownQuietPeriod(),
                    properties.getShutdownTimeOut(), TimeUnit.MILLISECONDS);
        }

        log.info("WebSocket服务已关闭");
    }

    /**
     * 检查服务是否运行中
     *
     * @return 是否运行中
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 获取当前连接数
     *
     * @return 连接数
     */
    public int getConnectionCount() {
        return WebSocketUtils.getConnectionCount();
    }
}
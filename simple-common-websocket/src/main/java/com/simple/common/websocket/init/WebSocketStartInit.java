package com.simple.common.websocket.init;

import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.initializer.WebSocketChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Created with IntelliJ IDEA
 * Description: webSocket服务启动
 *
 * @author qty
 */
@Slf4j
@Component
public class WebSocketStartInit implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private WebSocketProperties webSocketProperties;

    @Autowired
    private WebSocketListeningManager webSocketListeningManager;

    @Autowired
    private WebSocketSyncManager webSocketSyncManager;

    @Autowired
    private CheckWebSocketManager checkManager;

    /**
     * 服务端监听 Channel：保留引用以便停机时显式关闭监听端口，实现端口级优雅停机
     */
    private volatile Channel serverChannel;

    private volatile EventLoopGroup bossGroup;

    private volatile EventLoopGroup workerGroup;

    /**
     * Spring应用启动完成事件处理。
     * <p>
     * 端口绑定失败等启动异常先释放已创建的 Netty 资源再向上抛出，交由 Spring 启动流程终止宿主应用，
     * 保证启动失败语义清晰（应用未就绪即失败退出），且不残留 worker 线程。
     * 事件重复发布（上下文刷新/测试重复触发）时已启动则直接忽略，避免重建线程组覆盖旧引用造成泄漏。
     * </p>
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (serverChannel != null && serverChannel.isActive()) {
            log.warn("websocket服务已启动，忽略重复的ApplicationReadyEvent [port={}]", webSocketProperties.getPort());
            return;
        }
        try {

            // 若上次启动失败残留了线程组/监听通道，先释放再重建，避免线程泄漏
            shutdownEventLoops();

            // 初始化线程组
            bossGroup = new NioEventLoopGroup(webSocketProperties.getBossThreads());
            workerGroup = new NioEventLoopGroup(webSocketProperties.getWorkerThreads());

            // 配置服务器引导
            ServerBootstrap bootstrap = new ServerBootstrap();
            configureBootstrap(bootstrap);

            // 绑定端口并保留监听 Channel 引用（停机时优先关闭，实现端口释放与优雅停机）
            serverChannel = bootstrap.bind(webSocketProperties.getPort()).sync().channel();
            log.info("websocket服务启动成功，监听端口：{}", webSocketProperties.getPort());

        } catch (InterruptedException e) {

            // 启动被中断：先释放已创建的 Netty 资源，恢复中断标记后向上抛出，交由 Spring 启动流程终止宿主应用
            log.error("websocket服务启动被中断", e);
            shutdownEventLoops();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("websocket服务启动被中断", e);
        } catch (Exception e) {

            // 启动失败（如端口被占用）：先释放已创建的 EventLoopGroup，再向上抛出，交由 Spring 启动流程终止宿主应用
            log.error("websocket服务启动失败", e);
            shutdownEventLoops();
            throw new IllegalStateException("websocket服务启动失败", e);
        }
    }

    /**
     * 配置ServerBootstrap参数
     */
    private void configureBootstrap(ServerBootstrap bootstrap) {
        bootstrap.group(bossGroup, workerGroup)
                 .channel(NioServerSocketChannel.class)
                 // TCP服务器参数
                 .option(ChannelOption.SO_BACKLOG, webSocketProperties.getSoBacklog())
                 .option(ChannelOption.SO_REUSEADDR, true)
                 .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)

                 // TCP连接参数
                 .childOption(ChannelOption.TCP_NODELAY, true)
                 .childOption(ChannelOption.SO_KEEPALIVE, true)
                 .childOption(ChannelOption.SO_REUSEADDR, true)
                 .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, webSocketProperties.getWriteBufferHigh())
                 .childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, webSocketProperties.getWriteBufferLow())
                 .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                 // 日志处理器
                 .handler(new LoggingHandler(LogLevel.INFO))
                 // 通道初始化
                 .childHandler(new WebSocketChannelInitializer(webSocketProperties, webSocketListeningManager, webSocketSyncManager, checkManager));
    }

    /**
     * Spring容器关闭时优雅停机：先关闭服务端监听 Channel（拒绝新连接并释放监听端口），
     * 再按 boss → worker 顺序关闭线程组并等待终止，避免容器销毁后残留 Netty worker 线程
     */
    @PreDestroy
    private void shutdown() {
        shutdownEventLoops();
        log.info("websocket服务已关闭");
    }

    /**
     * 释放监听 Channel 与线程组（可重入：字段置空后重复调用为空操作）。
     * 停机路径使用 syncUninterruptibly 等待终止，不受中断干扰。
     */
    private void shutdownEventLoops() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
    }
}

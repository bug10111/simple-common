package com.simple.common.websocket.init;

import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.initializer.WebSocketChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
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
 * @author 兄台丶请冷静
 */
@Slf4j
@Component
public class WebSocketStartInit implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private WebSocketProperties webSocketProperties;

    private volatile EventLoopGroup bossGroup;

    private volatile EventLoopGroup workerGroup;

    /**
     * Spring应用启动完成事件处理
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {

            // 1. 初始化线程组
            bossGroup = new NioEventLoopGroup(webSocketProperties.getBossThreads());
            workerGroup = new NioEventLoopGroup(webSocketProperties.getWorkerThreads());

            // 2. 配置服务器引导
            ServerBootstrap bootstrap = new ServerBootstrap();
            configureBootstrap(bootstrap);

            // 3. 绑定端口并启动
            bootstrap.bind(webSocketProperties.getPort()).sync();
            log.info("websocket服务启动成功，监听端口：{}", webSocketProperties.getPort());

        } catch (InterruptedException e) {
            log.error("服务器启动被中断", e);
            registerShutdownHook();
            System.exit(1);
        } catch (Exception e) {
            log.error("服务器启动失败", e);
            registerShutdownHook();
            System.exit(1);
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
                 .childHandler(new WebSocketChannelInitializer());
    }

    /**
     * 注册JVM关闭钩子（备用方案）
     */
    @PreDestroy
    private void registerShutdownHook() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("websocket服务已关闭");
    }
}

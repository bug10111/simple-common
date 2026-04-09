package com.simple.common.logs.server.common.tcp;

import com.simple.common.logs.server.common.manager.LogsSaveManager;
import com.simple.common.logs.server.common.properties.LogTcpServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.net.InetSocketAddress;

/**
 * TCP日志服务端
 * 接收客户端发送的日志数据
 *
 * @author qty
 */
@Slf4j
@Getter
@Component
@ConditionalOnProperty(prefix = "simple.logs.tcp.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogTcpServer {
    @Autowired
    private LogTcpServerProperties properties;
    @Autowired
    private LogsSaveManager logsSaveManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    /**
     * 启动服务端
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加帧解码器
                        pipeline.addLast("frameDecoder", new LogFrameDecoder());
                        // 添加帧编码器
                        pipeline.addLast("frameEncoder", new LogFrameEncoder());
                        // 添加业务处理器
                        pipeline.addLast("handler", new LogTcpServerHandler(logsSaveManager));
                    }
                });

            channelFuture = bootstrap.bind(properties.getPort()).sync();
            log.info("TCP日志服务端启动成功，监听端口: {}", properties.getPort());

            // 添加关闭监听器
            channelFuture.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                log.info("TCP日志服务端已关闭");
            });

        } catch (Exception e) {
            log.error("TCP日志服务端启动失败", e);
            shutdown();
        }
    }

    /**
     * 关闭服务端
     */
    @PreDestroy
    public void shutdown() {
        if (channelFuture != null) {
            channelFuture.channel().close();
            channelFuture = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        log.info("TCP日志服务端已关闭");
    }

}
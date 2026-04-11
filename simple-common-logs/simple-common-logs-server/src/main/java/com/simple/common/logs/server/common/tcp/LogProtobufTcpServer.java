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

/**
 * Protobuf TCP 日志服务端
 * 基于 Netty 实现，使用 Protobuf 进行序列化
 *
 * @author Admin
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

    /**
     * 启动服务端
     */
    @PostConstruct
    public void start() {
        bossGroup = new NioEventLoopGroup(properties.getBossThreads());
        workerGroup = new NioEventLoopGroup(properties.getWorkerThreads());

        try {
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

                             // Protobuf 编解码器
                             pipeline.addLast("decoder", new LogProtobufDecoder());
                             pipeline.addLast("encoder", new LogResponseEncoder());

                             // 业务处理器
                             pipeline.addLast("handler", new LogProtobufServerHandler(logsSaveManager));
                         }
                     });

            // 绑定端口
            ChannelFuture future = bootstrap.bind(properties.getPort()).sync();
            serverChannel = future.channel();
            log.info("Protobuf TCP 日志服务端启动成功, 监听端口: {}", properties.getPort());
        } catch (Exception e) {
            log.error("Protobuf TCP 日志服务端启动失败: {}", e.getMessage(), e);
            shutdown();
        }
    }

    /**
     * 关闭服务端
     */
    @PreDestroy
    public void shutdown() {
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
package com.simple.common.logs.client.common.tcp;

import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import com.simple.common.logs.proto.LogData;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Protobuf TCP 日志客户端
 * 基于 Netty 实现，使用 Protobuf 进行序列化
 *
 * @author Admin
 */
@Slf4j
@Configuration
public class LogProtobufTcpClient {

    @Autowired
    private LogTcpClientProperties logTcpClientProperties;

    private EventLoopGroup group;

    private Channel channel;

    /**
     * 启动客户端
     */
    @PostConstruct
    public void start() {
        group = new NioEventLoopGroup(logTcpClientProperties.getWorkerThreads());
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                     .channel(NioSocketChannel.class)
                     .option(ChannelOption.TCP_NODELAY, true)
                     .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) throws Exception {
                             ChannelPipeline pipeline = ch.pipeline();

                             // 心跳检测
                             pipeline.addLast("idleStateHandler", new IdleStateHandler(0, 30000, 0, TimeUnit.MILLISECONDS));

                             // Protobuf 编解码器
                             pipeline.addLast("encoder", new LogProtobufEncoder());
                             pipeline.addLast("decoder", new LogResponseDecoder());

                             // 业务处理器
                             pipeline.addLast("handler", new LogProtobufClientHandler());
                         }
                     });

            // 连接服务器
            ChannelFuture future = bootstrap.connect(logTcpClientProperties.getHost(), logTcpClientProperties.getPort()).sync();
            channel = future.channel();
            log.info("Protobuf TCP 日志客户端启动成功, 连接到 {}:{}", logTcpClientProperties.getHost(), logTcpClientProperties.getPort());
        } catch (Exception e) {
            log.error("Protobuf TCP 日志客户端启动失败: {}", e.getMessage(), e);
            group.shutdownGracefully();
        }
    }

    /**
     * 发送日志数据
     *
     * @param logData 日志数据
     */
    public void send(LogData logData) {
        if (channel == null || !channel.isActive()) {
            log.warn("通道未激活，无法发送日志");
            return;
        }

        channel.writeAndFlush(logData).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("日志发送失败: {}", future.cause().getMessage());
            }
        });
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        if (isActive()) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("Protobuf TCP 日志客户端已关闭");
    }

    /**
     * 检查连接是否活跃
     *
     * @return 是否活跃
     */
    private boolean isActive() {
        return channel != null && channel.isActive();
    }
}
package com.simple.common.logs.client.common.tcp;

import com.simple.common.logs.client.common.properties.LogTcpClientProperties;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TCP日志客户端
 * 负责与日志服务端建立连接并发送日志数据
 *
 * @author qty
 */
@Slf4j
@Configuration
public class LogTcpClient {

    @Autowired
    private LogTcpClientProperties properties;

    private EventLoopGroup workerGroup;

    private Channel channel;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private final AtomicBoolean connecting = new AtomicBoolean(false);


    /**
     * 启动客户端
     */
    public void start() {
        if (connecting.get() || connected.get()) {
            return;
        }

        connecting.set(true);
        workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                     .channel(NioSocketChannel.class)
                     .option(ChannelOption.TCP_NODELAY, true)
                     .option(ChannelOption.SO_KEEPALIVE, true)
                     .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeout())
                     .handler(new ChannelInitializer<SocketChannel>() {
                         @Override
                         protected void initChannel(SocketChannel ch) {
                             ChannelPipeline pipeline = ch.pipeline();
                             // 添加帧编码器
                             pipeline.addLast("frameEncoder", new LogFrameEncoder());
                             // 添加帧解码器
                             pipeline.addLast("frameDecoder", new LogFrameDecoder());
                             // 添加数据编码器
                             pipeline.addLast("dataEncoder", new LogDataEncoder());
                             // 添加业务处理器
                             pipeline.addLast("handler", new LogTcpClientHandler());
                         }
                     });

            ChannelFuture future = bootstrap.connect(properties.getHost(), properties.getPort()).sync();
            channel = future.channel();
            connected.set(true);
            connecting.set(false);
            log.info("TCP日志客户端启动成功，连接到 {}: {}", properties.getHost(), properties.getPort());

            // 添加连接关闭监听器
            channel.closeFuture().addListener((ChannelFutureListener) future1 -> {
                connected.set(false);
                log.warn("TCP连接已断开，准备重连...");
                scheduleReconnect();
            });

        } catch (Exception e) {
            connecting.set(false);
            log.error("TCP日志客户端启动失败", e);
            scheduleReconnect();
        }
    }

    /**
     * 计划重连
     */
    private void scheduleReconnect() {
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.schedule(() -> {
                log.info("尝试重新连接到 {}: {}", properties.getHost(), properties.getPort());
                start();
            }, properties.getReconnectInterval(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 发送数据
     *
     * @param data 数据字节数组
     * @return 是否发送成功
     */
    public boolean send(byte[] data) {
        if (!isConnected()) {
            log.warn("TCP未连接，无法发送数据");
            // 尝试重新连接
            if (!connecting.get()) {
                start();
            }
            return false;
        }

        try {
            channel.writeAndFlush(data).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.error("发送数据失败", future.cause());
                }
            });
            return true;
        } catch (Exception e) {
            log.error("发送数据异常", e);
            return false;
        }
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected.get() && channel != null && channel.isActive();
    }

    /**
     * 关闭客户端
     */
    public void shutdown() {
        connected.set(false);
        connecting.set(false);
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        log.info("TCP日志客户端已关闭");
    }
}
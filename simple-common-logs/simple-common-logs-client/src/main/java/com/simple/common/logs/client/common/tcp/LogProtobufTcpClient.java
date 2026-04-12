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
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Protobuf TCP 日志客户端（Fire-and-Forget 模式）
 * 基于 Netty 实现，使用 Protobuf 进行序列化（仅支持批量发送）
 * <p>
 * 为追求极致性能，本客户端不处理服务端响应，仅单向发送日志数据。
 * </p>
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

    private Bootstrap bootstrap;

    private final AtomicBoolean connected = new AtomicBoolean(false);

    private int reconnectAttempts = 0;

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
        bootstrap.connect(logTcpClientProperties.getHost(), logTcpClientProperties.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                connected.set(true);
                reconnectAttempts = 0;
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
     * </p>
     */
    private void scheduleReconnect() {
        if (logTcpClientProperties.getMaxReconnectAttempts() != 0) {
            int max = logTcpClientProperties.getMaxReconnectAttempts();
            if (max > 0 && reconnectAttempts >= max) {
                log.error("已达最大重连次数 {}，不再重连", max);
                return;
            }
            reconnectAttempts++;
        }
        group.schedule(this::doConnect, logTcpClientProperties.getReconnectInterval(), TimeUnit.MILLISECONDS);
    }

    /**
     * 发送批量日志（LogBatch 协议）
     * <p>
     * 将多条日志合并为一个 LogBatch 消息发送，减少网络包和编解码开销。
     * 采用 Fire-and-Forget 模式，不等待也不处理服务端响应。
     * </p>
     *
     * @param logBatch 批量日志容器
     */
    public void sendBatch(LogBatch logBatch) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(logBatch).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    log.error("批量日志发送失败: {}", future.cause().getMessage());
                } else {
                    // 发送成功，无额外处理
                    log.debug("批量日志发送成功, 数量: {}", logBatch.getLogsCount());
                }
            });
        } else {
            log.warn("通道未激活，无法发送批量日志");
            throw new IllegalStateException("通道未激活");
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
        return connected.get() && channel != null && channel.isActive();
    }

    /**
     * 关闭客户端
     * <p>
     * 该方法在 Bean 销毁前自动调用，依次关闭通道和释放线程组资源。
     * </p>
     */
    @PreDestroy
    public void shutdown() {
        if (isActive()) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("Protobuf TCP 日志客户端已关闭");
    }
}
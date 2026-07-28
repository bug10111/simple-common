package com.simple.common.websocket.initializer;

import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.handler.WebSocketAuthHandler;
import com.simple.common.websocket.handler.WebSocketServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * WebSocket通道初始化器
 *
 * @author qty
 */
public class WebSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final WebSocketProperties properties;

    private final WebSocketListeningManager webSocketListeningManager;

    private final WebSocketSyncManager webSocketSyncManager;

    private final CheckWebSocketManager checkManager;

    public WebSocketChannelInitializer(WebSocketProperties properties, WebSocketListeningManager webSocketListeningManager, WebSocketSyncManager webSocketSyncManager, CheckWebSocketManager checkManager) {
        this.properties = properties;
        this.webSocketListeningManager = webSocketListeningManager;
        this.webSocketSyncManager = webSocketSyncManager;
        this.checkManager = checkManager;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();

        // HTTP编解码器
        pipeline.addLast("http-codec", new HttpServerCodec());

        // HTTP消息聚合器
        pipeline.addLast("aggregator", new HttpObjectAggregator(properties.getMaxHttpContentLength()));

        // 分块写入处理器
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());

        // 空闲状态检测
        pipeline.addLast("idle-handler", new IdleStateHandler(properties.getReaderIdleTime(), properties.getWriterIdleTime(), properties.getAllIdleTime()));

        // 认证处理器
        pipeline.addLast("auth-handler", new WebSocketAuthHandler(properties,checkManager));

        // WebSocket消息处理器
        pipeline.addLast("websocketHandler", new WebSocketServerHandler(properties, webSocketListeningManager, webSocketSyncManager));

        // WebSocket协议处理器
        pipeline.addLast("ws-protocol-handler", new WebSocketServerProtocolHandler(properties.getPath(), true, properties.getMaxWebSocketFrameSize()));
    }
}
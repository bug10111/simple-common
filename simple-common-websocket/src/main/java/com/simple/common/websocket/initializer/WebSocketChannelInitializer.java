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
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * WebSocket通道初始化器
 * <p>
 * 数值限制（聚合长度/帧上限/空闲时间）由 {@link WebSocketProperties} 的 JSR-380 校验
 * 在绑定阶段保证为正数，此处不再重复校验。
 * </p>
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
    protected void initChannel(SocketChannel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        // HTTP编解码器
        pipeline.addLast("http-codec", new HttpServerCodec());

        // HTTP消息聚合器
        pipeline.addLast("aggregator", new HttpObjectAggregator(properties.getMaxHttpContentLength()));

        // 分块写入处理器
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());

        // 空闲状态检测
        pipeline.addLast("idle-handler", new IdleStateHandler(properties.getReaderIdleTime(), properties.getWriterIdleTime(), properties.getAllIdleTime()));

        // 认证处理器
        pipeline.addLast("auth-handler", new WebSocketAuthHandler(properties, checkManager));

        // WebSocket协议处理器（握手升级 + Close/Ping/Pong 自动处理）
        // 使用 WebSocketServerProtocolConfig.Builder 构建完整配置：
        // checkStartsWith=true 使路径前缀匹配（兼容 /simple?type=... 等带 query string 的握手）；
        // 注意：路径准入的真正裁决在 auth-handler（严格匹配 + 拒绝 "."/".." 段），
        // 本处理器的前缀匹配仅是握手层的第一道粗筛，auth-handler 必须保持在其之前，不可移除或调序；
        // maxFramePayloadLength 控制帧解码器 WebSocket08FrameDecoder 的最大帧载荷长度，
        // 必须显式设置，否则使用 Netty 默认值 65536（64KB），大数据量通讯会触发 CorruptedWebSocketFrameException。
        WebSocketServerProtocolConfig wsConfig = WebSocketServerProtocolConfig.newBuilder()
                .websocketPath(properties.getPath())
                .checkStartsWith(true)
                .maxFramePayloadLength(properties.getMaxWebSocketFrameSize())
                .build();

        pipeline.addLast("ws-protocol-handler", new WebSocketServerProtocolHandler(wsConfig));

        // WebSocket帧聚合器（合并分片帧）：协议处理器只解码不聚合，
        // 缺少本处理器时分片消息的 ContinuationWebSocketFrame 会被业务处理器当作非数据帧丢弃
        pipeline.addLast("ws-frame-aggregator", new WebSocketFrameAggregator(properties.getMaxWebSocketFrameSize()));

        // WebSocket消息处理器（仅处理 Text/Binary 数据帧）
        pipeline.addLast("websocketHandler", new WebSocketServerHandler(properties, webSocketListeningManager, webSocketSyncManager));
    }
}

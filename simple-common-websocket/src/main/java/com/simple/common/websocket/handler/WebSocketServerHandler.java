package com.simple.common.websocket.handler;

import com.alibaba.fastjson2.JSON;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.constant.WebsocketExceptionEnum;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket消息处理器
 * <p>
 * 负责处理WebSocket消息的接收、解析和分发。
 * 同时检测同步请求的回复消息，通过 requestId 匹配对应的 CompletableFuture 完成同步等待。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class WebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private final WebSocketListeningManager webSocketListeningManager;

    private final WebSocketSyncManager webSocketSyncManager;

    private final WebSocketProperties properties;

    public WebSocketServerHandler(WebSocketProperties properties, WebSocketListeningManager webSocketListeningManager, WebSocketSyncManager webSocketSyncManager) {
        this.properties = properties;
        this.webSocketListeningManager = webSocketListeningManager;
        this.webSocketSyncManager = webSocketSyncManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * 处理WebSocket帧
     * <p>
     * Close/Ping/Pong 帧已由 WebSocketServerProtocolHandler 自动处理，此处仅处理数据帧。
     * </p>
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            handleTextFrame(ctx, (TextWebSocketFrame) frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            handleBinaryFrame(ctx, (BinaryWebSocketFrame) frame);
        } else {
            log.debug("收到非数据帧，已由协议处理器处理: {}", frame.getClass().getSimpleName());
        }
    }

    /**
     * 处理文本帧
     */
    private void handleTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();

        if (text == null || text.isEmpty()) {
            log.warn("收到空消息");
            sendError(ctx, WebsocketExceptionEnum.EMPTY_MESSAGE);
            return;
        }

        int maxLength = properties.getMaxTextMessageLength();
        if (text.length() > maxLength) {
            log.warn("消息过长，拒绝处理 [length={}, max={}]", text.length(), maxLength);
            sendError(ctx, WebsocketExceptionEnum.MESSAGE_TOO_LARGE, "最大允许" + maxLength + "字节");
            return;
        }

        try {
            WebSocketRequest<?> request = JSON.parseObject(text, WebSocketRequest.class);
            if (request == null) {
                sendError(ctx, WebsocketExceptionEnum.INVALID_MESSAGE);
                return;
            }

            // 检测是否为同步请求的回复消息：若 requestId 不为空，尝试匹配待处理的同步请求
            String requestId = request.getRequestId();
            if (requestId != null) {
                boolean completed = webSocketSyncManager.complete(requestId, request.getData());
                if (completed) {
                    if (log.isDebugEnabled()) {
                        log.debug("同步请求回复已匹配 [requestId={}]", requestId);
                    }
                    return;
                }
                log.debug("同步请求ID未找到匹配项，按普通消息分发 [requestId={}]", requestId);
            }

            String channelType = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
            String channelCliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);

            if (channelType == null || channelCliKey == null) {
                sendError(ctx, WebsocketExceptionEnum.UNAUTHORIZED);
                return;
            }

            // 将通道上下文注入请求对象，业务方法可通过 request.getType()/getCliKey()/reply() 使用
            request.setType(channelType);
            request.setCliKey(channelCliKey);

            // 提交到业务线程池异步执行，避免阻塞 Netty EventLoop
            dispatchAndReply(ctx, request);
        } catch (Exception e) {
            log.error("消息处理异常", e);
            sendError(ctx, WebsocketExceptionEnum.PROCESS_ERROR, e.getMessage());
        }
    }

    /**
     * 处理二进制帧（Protobuf 信封，codec=proto 连接使用）
     */
    private void handleBinaryFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        byte[] bytes = new byte[frame.content().readableBytes()];
        frame.content().readBytes(bytes);

        com.simple.common.websocket.proto.WebSocketFrame protoFrame;
        try {
            protoFrame = com.simple.common.websocket.proto.WebSocketFrame.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            log.warn("二进制消息 Protobuf 解析失败，拒绝处理");
            sendError(ctx, WebsocketExceptionEnum.INVALID_MESSAGE, "二进制帧 Protobuf 解析失败");
            return;
        }

        // 通道标识以 Channel 属性为准（与文本帧一致）
        String channelType = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
        String channelCliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
        if (channelType == null || channelCliKey == null) {
            sendError(ctx, WebsocketExceptionEnum.UNAUTHORIZED);
            return;
        }

        WebSocketRequest<byte[]> request = new WebSocketRequest<>();
        String requestId = protoFrame.getRequestId();
        request.setRequestId(requestId.isEmpty() ? null : requestId);
        request.setType(channelType);
        request.setCliKey(channelCliKey);
        request.setData(protoFrame.getData().toByteArray());

        // 同步请求回复匹配：requestId 非空时尝试唤醒等待线程
        if (request.getRequestId() != null) {
            boolean completed = webSocketSyncManager.complete(request.getRequestId(), request.getData());
            if (completed) {
                if (log.isDebugEnabled()) {
                    log.debug("同步请求回复已匹配 [requestId={}]", request.getRequestId());
                }
                return;
            }
            log.debug("同步请求ID未找到匹配项，按普通消息分发 [requestId={}]", request.getRequestId());
        }

        // 提交到业务线程池异步执行，避免阻塞 Netty EventLoop
        dispatchAndReply(ctx, request);
    }

    /**
     * 提交到业务线程池异步执行，并按连接编解码方式回写结果
     */
    private void dispatchAndReply(ChannelHandlerContext ctx, WebSocketRequest<?> request) {
        String channelType = request.getType();
        String channelCliKey = request.getCliKey();
        ThreadUtils.supplyAsync(() -> webSocketListeningManager.invoke(channelType, channelCliKey, request))
                   .thenAccept(result -> result.ifPresent(o -> writeReply(ctx, o)))
                   .exceptionally(e -> {
                       log.error("消息处理异常 [type={}, cliKey={}]", channelType, WebSocketUtils.maskKey(channelCliKey), e);
                       sendError(ctx, WebsocketExceptionEnum.PROCESS_ERROR, e.getMessage());
                       return null;
                   });
    }

    /**
     * 按连接编解码方式回写业务结果。
     * <p>
     * json 连接：文本帧，沿用 String.valueOf 行为；
     * proto 连接：二进制信封帧，业务返回值若为 byte[] 则直接作为 data，否则按 JSON bytes 封装。
     * </p>
     */
    private void writeReply(ChannelHandlerContext ctx, Object result) {
        if (result == null) {
            return;
        }
        if (isProtoCodec(ctx)) {
            byte[] payload = result instanceof byte[] ? (byte[]) result : JSON.toJSONBytes(result);
            com.simple.common.websocket.proto.WebSocketFrame reply =
                    com.simple.common.websocket.proto.WebSocketFrame.newBuilder()
                            .setData(ByteString.copyFrom(payload))
                            .build();
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(reply.toByteArray())));
        } else {
            ctx.writeAndFlush(new TextWebSocketFrame(String.valueOf(result)));
        }
    }

    /**
     * 判断当前连接是否使用 Protobuf 编解码
     */
    private boolean isProtoCodec(ChannelHandlerContext ctx) {
        Object codec = getChannelAttr(ctx, WebSocketConstant.ATTR_CODEC);
        return WebSocketConstant.CODEC_PROTO.equalsIgnoreCase(codec == null ? null : codec.toString());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
        String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
        log.error("WebSocket异常 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
        String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
        if (type != null && cliKey != null) {
            if (log.isDebugEnabled()) {
                log.debug("连接断开 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            }
        }
    }

    private void sendError(ChannelHandlerContext ctx, WebsocketExceptionEnum exceptionEnum) {
        sendError(ctx, exceptionEnum, null);
    }

    private void sendError(ChannelHandlerContext ctx, WebsocketExceptionEnum exceptionEnum, String extraMessage) {
        String message = exceptionEnum.getMessage() + (extraMessage != null ? ": " + extraMessage : "");
        String json = String.format("{\"code\":%s,\"message\":\"%s\"}", exceptionEnum.getCode(), message);
        ctx.writeAndFlush(new TextWebSocketFrame(json));
    }

    private String getChannelAttr(ChannelHandlerContext ctx, String key) {
        Object value = ctx.channel().attr(AttributeKey.valueOf(key)).get();
        return value != null ? value.toString() : null;
    }
}
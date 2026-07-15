package com.simple.common.websocket.handler;

import com.alibaba.fastjson2.JSON;
import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.constant.WebsocketExceptionEnum;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket消息处理器
 * <p>
 * 负责处理WebSocket消息的接收、解析和分发
 *
 * @author qty
 */
@Slf4j
public class WebSocketServerHandler extends ChannelInboundHandlerAdapter {

    private final WebSocketListeningManager webSocketListeningManager;

    private final WebSocketProperties properties;

    public WebSocketServerHandler(WebSocketProperties properties, WebSocketListeningManager webSocketListeningManager) {
        this.properties = properties;
        this.webSocketListeningManager = webSocketListeningManager;
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
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            handleTextFrame(ctx, (TextWebSocketFrame) frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            handleBinaryFrame(ctx, (BinaryWebSocketFrame) frame);
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else if (frame instanceof PongWebSocketFrame) {
            log.debug("收到Pong帧");
        } else if (frame instanceof CloseWebSocketFrame) {
            log.debug("收到关闭帧");
            ctx.close();
        } else {
            log.warn("不支持的消息帧类型: {}", frame.getClass().getSimpleName());
            sendError(ctx, WebsocketExceptionEnum.UNSUPPORTED_FRAME);
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

        if (log.isDebugEnabled()) {
            String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
            String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
            log.debug("收到消息 [type={}, cliKey={}, length={}]", type, WebSocketUtils.maskKey(cliKey), text.length());
        }

        try {
            WebSocketRequest<?> request = JSON.parseObject(text, WebSocketRequest.class);
            if (request == null) {
                sendError(ctx, WebsocketExceptionEnum.INVALID_MESSAGE);
                return;
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
            ThreadUtils.supplyAsync(() -> webSocketListeningManager.invoke(channelType, channelCliKey, request))
                       .thenAccept(result -> result.ifPresent(o -> ctx.writeAndFlush(new TextWebSocketFrame(String.valueOf(o)))));
        } catch (Exception e) {
            log.error("消息处理异常", e);
            sendError(ctx, WebsocketExceptionEnum.PROCESS_ERROR, e.getMessage());
        }
    }

    /**
     * 处理二进制帧
     */
    private void handleBinaryFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        log.debug("收到二进制消息，暂不支持");
        sendError(ctx, WebsocketExceptionEnum.UNSUPPORTED_FRAME, "暂不支持二进制消息");
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
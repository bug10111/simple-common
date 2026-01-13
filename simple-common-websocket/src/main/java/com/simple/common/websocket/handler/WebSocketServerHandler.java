package com.simple.common.websocket.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.websocket.utils.WebSocketUtils;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.manager.WebSocketListeningManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private WebSocketListeningManager webSocketListeningManager;

    private String type;

    private String cliKey;

    public WebSocketServerHandler() {
        this.webSocketListeningManager = SpringUtil.getBean(WebSocketListeningManager.class);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.debug("---客户端---" + ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                log.debug("链接超时，开始断开链接");
                ctx.channel().close();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        WebSocketUtils.del(type, cliKey, ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof FullHttpRequest request) {

            //获取从权限处理器中得到的客户端信息
            cliKey = ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.CLI_KEY)).get().toString();
            type = ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.TYPE)).get().toString();

            //加入通道管理
            WebSocketUtils.addMap(type, cliKey, ctx);
            log.info("新链接加入 [{}] ===> [{}] ", type, cliKey);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // 根据帧类型处理
        if (frame instanceof TextWebSocketFrame) {
            // 处理文本帧（例如聊天消息）
            handleTextFrame(ctx, (TextWebSocketFrame) frame);
        } else if (frame instanceof BinaryWebSocketFrame) {
            // 处理二进制帧（例如文件传输）
            handleBinaryFrame(ctx, (BinaryWebSocketFrame) frame);
        } else if (frame instanceof CloseWebSocketFrame) {
            // 处理关闭帧
            ctx.close();
        } else if (frame instanceof PingWebSocketFrame) {
            // 响应 PING 帧（自动回复 PONG）
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
        } else {
            ctx.close();
            // 其他帧类型直接拒绝
            throw new UnsupportedOperationException("不支持的帧类型: " + frame.getClass().getName());
        }
    }

    /**
     * 处理文本消息
     */
    private void handleTextFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String requestText = frame.text();
        Optional<Object> distribution = webSocketListeningManager.invoke(type, cliKey, requestText);
        distribution.ifPresent(s -> ctx.writeAndFlush(new TextWebSocketFrame(s.toString())));
    }

    /**
     * 处理二进制消息
     */
    private void handleBinaryFrame(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        byte[] bytes = new byte[frame.content().readableBytes()];
        frame.content().readBytes(bytes);
        log.info("websocket收到二进制数据，（暂时未处理）长度: {}", bytes.length);
    }
}

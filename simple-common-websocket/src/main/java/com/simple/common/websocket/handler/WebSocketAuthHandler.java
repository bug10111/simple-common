package com.simple.common.websocket.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AttributeKey;

/**
 * Created with IntelliJ IDEA
 * Description: 鉴权实现
 *
 * @author qty
 */
public class WebSocketAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final CheckWebSocketManager checkWebSocketManager;

    public WebSocketAuthHandler() {
        this.checkWebSocketManager = SpringUtil.getBean(CheckWebSocketManager.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
        String cliKey = checkWebSocketManager.check(authHeader);
        AssertUtils.notEmpty(cliKey, "握手失败！token没有返回有效客户端标志！");
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.CLI_KEY)).set(cliKey);
        ctx.fireChannelRead(request.retain());
    }
}
package com.simple.common.websocket.handler;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.utils.AssertUtils;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: 鉴权实现
 *
 * @author qty
 */
@Slf4j
public class WebSocketAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final CheckWebSocketManager checkWebSocketManager;

    public WebSocketAuthHandler() {
        this.checkWebSocketManager = SpringUtil.getBean(CheckWebSocketManager.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        //解析uri，判断握手点
        String uri = request.uri();
        if (!uri.contains(WebSocketConstant.WEBSOCKET_PATH)) {
            log.error("握手端点错误：{}", WebSocketConstant.WEBSOCKET_PATH);
            return;
        }

        //获取socket type,如果每户怄气到，直接返回，并断开连接
        QueryStringDecoder queryDecoder = new QueryStringDecoder(uri);
        List<String> strings = queryDecoder.parameters().get(WebSocketConstant.TYPE);
        if (ObjUtil.isEmpty(strings)) {
            log.error("握手连接中，必须包含参数：{}", WebSocketConstant.TYPE);
            ctx.close();
            return;
        }

        String type = strings.get(0);
        if (ObjUtil.isEmpty(type)) {
            log.error("握手连接中，必须包含参数：{}", WebSocketConstant.TYPE);
            ctx.close();
            return;
        }
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.TYPE)).set(type);


        //获取socket token,如果每户怄气到，直接返回，并断开连接
        List<String> tokens = queryDecoder.parameters().get(WebSocketConstant.token);
        if (ObjUtil.isEmpty(tokens)) {
            log.error("握手连接中，必须包含参数：{}", WebSocketConstant.token);
            ctx.close();
            return;
        }

        String token = tokens.get(0);
        if (ObjUtil.isEmpty(token)) {
            log.error("握手连接中，必须包含参数：{}", WebSocketConstant.token);
            ctx.close();
            return;
        }

        token = URLDecoder.decode(token, StandardCharsets.UTF_8);
        String cliKey = checkWebSocketManager.check(token);
        AssertUtils.notEmpty(cliKey, "握手失败！token没有返回有效客户端标志！");
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.CLI_KEY)).set(cliKey);
        ctx.fireChannelRead(request.retain());
    }
}
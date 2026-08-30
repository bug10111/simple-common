package com.simple.common.websocket.handler;

import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.constant.WebsocketExceptionEnum;
import com.simple.common.websocket.common.manager.CheckWebSocketManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * WebSocket认证处理器
 * <p>
 * 负责处理握手请求、参数校验和Token认证
 *
 * @author qty
 */
@Slf4j
public class WebSocketAuthHandler extends ChannelInboundHandlerAdapter {

    private final WebSocketProperties properties;
    private final CheckWebSocketManager checkManager;

    public WebSocketAuthHandler(WebSocketProperties properties, CheckWebSocketManager checkManager) {
        this.properties = properties;
        this.checkManager = checkManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest request) {
            // 解析uri，判断握手点
            String uri = request.uri();
            if (!uri.contains(properties.getPath())) {
                log.error("握手端点错误：{}", properties.getPath());
                sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.INVALID_PATH);
                return;
            }

            // 解析握手参数
            QueryStringDecoder query = new QueryStringDecoder(request.uri());
            Map<String, List<String>> parameters = query.parameters();

            // 获取参数
            List<String> types = parameters.get(WebSocketConstant.TYPE);
            List<String> cliKeys = parameters.get(WebSocketConstant.CLI_KEY);
            List<String> tokens = parameters.get(WebSocketConstant.TOKEN);
            List<String> codecs = parameters.get(WebSocketConstant.CODEC);

            // 参数校验
            if (types == null || types.isEmpty()) {
                log.warn("缺少type参数 [uri={}]", maskUri(request.uri()));
                sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.MISSING_PARAM, "缺少type参数");
                return;
            }

            if (cliKeys == null || cliKeys.isEmpty()) {
                log.warn("缺少cliKey参数 [uri={}]", maskUri(request.uri()));
                sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.MISSING_PARAM, "缺少cliKey参数");
                return;
            }

            String type = types.get(0);
            String cliKey = cliKeys.get(0);
            String token = tokens != null && !tokens.isEmpty() ? tokens.get(0) : null;

            // 参数长度校验
            if (type.length() > 64 || cliKey.length() > 128) {
                log.warn("参数长度超限 [type={}, cliKey={}]", type.length(), cliKey.length());
                sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.INVALID_MESSAGE, "参数长度超限");
                return;
            }

            // Token校验
            if (checkManager != null) {
                boolean authResult = checkManager.checkToken(token, type, cliKey);
                if (!authResult) {
                    log.warn("Token认证失败 [type={}, cliKey={}]", type, cliKey);
                    sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.AUTH_FAILED);
                    return;
                }
            }

            // 编解码方式校验：缺省 json，仅支持 json / proto
            String codec = codecs != null && !codecs.isEmpty() ? codecs.get(0) : WebSocketConstant.CODEC_JSON;
            if (!WebSocketConstant.CODEC_JSON.equalsIgnoreCase(codec) && !WebSocketConstant.CODEC_PROTO.equalsIgnoreCase(codec)) {
                log.warn("codec参数非法，默认使用json [codec={}, uri={}]", codec, maskUri(request.uri()));
                codec = WebSocketConstant.CODEC_JSON;
            }

            // 保存客户端信息到Channel属性
            ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_TYPE)).set(type);
            ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CLI_KEY)).set(cliKey);
            ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CONNECT_TIME)).set(System.currentTimeMillis());
            ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CODEC)).set(codec.toLowerCase());

            // 保存通道
            WebSocketUtils.addMap(type, cliKey, ctx);

            // 记录连接日志
            if (log.isDebugEnabled()) {
                InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                log.debug("WebSocket连接成功 [type={}, cliKey={}, ip={}]", type, WebSocketUtils.maskKey(cliKey), remoteAddress.getAddress().getHostAddress());
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
            String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
            log.debug("连接超时，关闭连接 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            ctx.close();
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("认证处理器异常 [remote={}]", ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
        String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
        if (type != null && cliKey != null) {
            WebSocketUtils.del(type, cliKey, ctx);
            if (log.isDebugEnabled()) {
                log.debug("连接断开 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            }
        }
    }

    /**
     * 发送HTTP错误响应并关闭连接
     */
    private void sendHttpErrorAndClose(ChannelHandlerContext ctx, WebsocketExceptionEnum exceptionEnum) {
        sendHttpErrorAndClose(ctx, exceptionEnum, null);
    }

    /**
     * 发送HTTP错误响应并关闭连接（带附加信息）
     */
    private void sendHttpErrorAndClose(ChannelHandlerContext ctx, WebsocketExceptionEnum exceptionEnum, String extraMessage) {
        String message = exceptionEnum.getMessage() + (extraMessage != null ? ": " + extraMessage : "");
        String json = String.format("{\"code\":%s,\"message\":\"%s\"}", exceptionEnum.getCode(), message);
        
        ByteBuf content = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, 
                exceptionEnum == WebsocketExceptionEnum.AUTH_FAILED ? HttpResponseStatus.UNAUTHORIZED : HttpResponseStatus.BAD_REQUEST,
                content
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 获取Channel属性
     */
    private String getChannelAttr(ChannelHandlerContext ctx, String key) {
        Object value = ctx.channel().attr(AttributeKey.valueOf(key)).get();
        return value != null ? value.toString() : null;
    }

    /**
     * 对URI进行脱敏
     */
    private String maskUri(String uri) {
        if (uri == null) {
            return "null";
        }
        int queryIndex = uri.indexOf('?');
        return queryIndex > 0 ? uri.substring(0, queryIndex) + "?***" : uri;
    }
}
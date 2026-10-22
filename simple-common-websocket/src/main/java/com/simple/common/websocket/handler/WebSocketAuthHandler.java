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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import com.alibaba.fastjson2.JSON;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * WebSocket认证处理器
 * <p>
 * 负责处理握手请求、参数校验和Token认证。
 * 路径规则由本处理器强制执行（严格匹配配置路径或其子路径，拒绝 "."/".." 穿越段），
 * 管线中本处理器必须位于 ws-protocol-handler 之前；
 * 通道注册（路由表）延后到握手完成后由下游处理器执行。
 * </p>
 * <p>
 * 安全基线：未配置校验器（checkManager == null）时拒绝握手（fail-closed），
 * 不存在静默放行的未认证接入。
 * </p>
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
            boolean forwarded = false;
            try {
                forwarded = handleHandshake(ctx, request);
            } finally {

                // 未向下游转发的握手请求由本处理器负责释放引用计数，
                // 否则每次非法握手（路径不匹配/缺参数/认证失败等）都泄漏一个聚合 ByteBuf
                if (!forwarded) {
                    ReferenceCountUtil.release(request);
                }
            }
            return;
        }
        super.channelRead(ctx, msg);
    }

    /**
     * 处理握手请求。
     *
     * @return true 表示校验通过并已向下游转发该请求；false 表示已拒绝（含错误响应），请求未被转发
     */
    private boolean handleHandshake(ChannelHandlerContext ctx, FullHttpRequest request) {

        // 校验握手路径：解析 uri 取路径段做严格前缀匹配，query 不参与判断，畸形 uri 直接拒绝握手
        if (!matchHandshakePath(ctx, request.uri())) {
            return false;
        }

        // 解析握手参数
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> parameters = decoder.parameters();

        // 获取参数
        List<String> types = parameters.get(WebSocketConstant.TYPE);
        List<String> cliKeys = parameters.get(WebSocketConstant.CLI_KEY);
        List<String> tokens = parameters.get(WebSocketConstant.TOKEN);
        List<String> codecs = parameters.get(WebSocketConstant.CODEC);

        // 参数校验
        if (types == null || types.isEmpty()) {
            log.warn("缺少type参数 [uri={}]", maskUri(request.uri()));
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.MISSING_PARAM, "缺少type参数");
            return false;
        }

        if (cliKeys == null || cliKeys.isEmpty()) {
            log.warn("缺少cliKey参数 [uri={}]", maskUri(request.uri()));
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.MISSING_PARAM, "缺少cliKey参数");
            return false;
        }

        String type = types.get(0);
        String cliKey = cliKeys.get(0);
        String token = tokens != null && !tokens.isEmpty() ? tokens.get(0) : null;

        // 参数长度校验
        if (type.length() > 64 || cliKey.length() > 128) {
            log.warn("参数长度超限 [type={}, cliKey={}]", type.length(), cliKey.length());
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.INVALID_MESSAGE, "参数长度超限");
            return false;
        }

        // 认证前置校验：无校验器视为配置错误，拒绝握手（fail-closed），避免静默放行未认证接入
        if (checkManager == null) {
            log.error("CheckWebSocketManager 未配置，拒绝握手 [uri={}]", maskUri(request.uri()));
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.AUTH_FAILED);
            return false;
        }

        // Token校验
        boolean authResult = checkManager.checkToken(token, type, cliKey);
        if (!authResult) {
            log.warn("Token认证失败 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.AUTH_FAILED);
            return false;
        }

        // 编解码方式校验：缺省 json，仅支持 json / proto
        String codec = codecs != null && !codecs.isEmpty() ? codecs.get(0) : WebSocketConstant.CODEC_JSON;
        if (!WebSocketConstant.CODEC_JSON.equalsIgnoreCase(codec) && !WebSocketConstant.CODEC_PROTO.equalsIgnoreCase(codec)) {
            log.warn("codec参数非法，默认使用json [codec={}, uri={}]", codec, maskUri(request.uri()));
            codec = WebSocketConstant.CODEC_JSON;
        }

        // 保存客户端信息到Channel属性（供下游处理器与业务侧读取；
        // 通道注册延后到握手完成后进行，避免未完成升级的通道暴露给业务侧）
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_TYPE)).set(type);
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CLI_KEY)).set(cliKey);
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CONNECT_TIME)).set(System.currentTimeMillis());
        ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CODEC)).set(codec.toLowerCase());
        return true;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
            String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
            log.debug("连接空闲超时，关闭连接 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
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
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String type = getChannelAttr(ctx, WebSocketConstant.ATTR_TYPE);
        String cliKey = getChannelAttr(ctx, WebSocketConstant.ATTR_CLI_KEY);
        if (type != null && cliKey != null) {
            WebSocketUtils.del(type, cliKey, ctx);
            if (log.isDebugEnabled()) {
                log.debug("连接断开 [type={}, cliKey={}]", type, WebSocketUtils.maskKey(cliKey));
            }
        }

        // 向下游传递断开事件：吞掉该事件会阻断 ws-protocol-handler / websocketHandler 的清理逻辑
        super.channelInactive(ctx);
    }

    /**
     * 校验握手路径是否匹配配置的握手端点。
     * <p>
     * 解析请求 uri 取路径段后做严格匹配：路径全等或位于配置路径的子路径下才放行，
     * query 参数不参与匹配；uri 非法或路径段缺失时拒绝握手；
     * 含 "." / ".." 段的路径一律拒绝，防止路径穿越（如 /simple/../admin）绕过前缀匹配。
     * </p>
     *
     * @param ctx 通道上下文
     * @param uri 握手请求 uri（可能为 ws 路径形态或绝对形态）
     * @return true 路径匹配放行
     */
    private boolean matchHandshakePath(ChannelHandlerContext ctx, String uri) {
        String requestPath;

        // 解析 uri 取路径段，畸形 uri 直接拒绝握手
        try {
            URI requestUri = URI.create(uri);
            requestPath = requestUri.getPath();
        } catch (IllegalArgumentException e) {
            log.warn("握手uri非法，拒绝握手 [uri={}]", maskUri(uri));
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.INVALID_PATH);
            return false;
        }

        // 拒绝含 "." / ".." 路径段的请求：URI.getPath 解码但不规范化，
        // 不拦截时 /simple/../admin 等会被 startsWith 前缀匹配误放行
        if (requestPath != null) {
            for (String segment : requestPath.split("/")) {
                if (".".equals(segment) || "..".equals(segment)) {
                    log.warn("握手路径包含非法路径段，拒绝握手 [path={}]", maskUri(requestPath));
                    sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.INVALID_PATH);
                    return false;
                }
            }
        }

        // 严格路径匹配：全等或配置路径下子路径，避免 contains 对同串 query 的误放行
        String configuredPath = properties.getPath();
        if (requestPath == null || (!requestPath.equals(configuredPath) && !requestPath.startsWith(configuredPath + "/"))) {
            log.error("握手端点错误 [configuredPath={}, requestPath={}]", configuredPath, maskUri(requestPath));
            sendHttpErrorAndClose(ctx, WebsocketExceptionEnum.INVALID_PATH);
            return false;
        }
        return true;
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
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", exceptionEnum.getCode());
        error.put("message", exceptionEnum.getMessage() + (extraMessage != null ? ": " + extraMessage : ""));
        ByteBuf content = Unpooled.wrappedBuffer(JSON.toJSONBytes(error));
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

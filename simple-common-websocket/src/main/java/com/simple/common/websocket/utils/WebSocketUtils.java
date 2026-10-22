package com.simple.common.websocket.utils;

import com.google.protobuf.ByteString;
import com.simple.common.core.utils.IdUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.websocket.common.channel.ChannelMap;
import com.simple.common.websocket.common.constant.WebSocketConstant;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * WebSocket通讯通道管理工具类。
 * <p>
 * 提供通道管理、消息发送（异步/同步）等底层能力。
 * 同步请求-响应模式通过 Correlation ID + CompletableFuture 实现，
 * 服务端发送请求后阻塞等待客户端回复，适用于服务端主动向客户端发起命令调用。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class WebSocketUtils {

    private static final ChannelMap<String, String, ChannelHandlerContext> map = new ChannelMap<>();

    /**
     * 同步请求管理器，由 DefaultWebSocketSyncManager 初始化时注入
     * -- SETTER --
     *  设置同步请求管理器（由框架内部调用）
     *
     */
    @Setter
    private static WebSocketSyncManager syncManager;

    /**
     * 默认同步请求超时时间（秒），由 DefaultWebSocketSyncManager 从 WebSocketProperties 注入
     */
    private static long defaultSyncTimeoutSeconds = 30;

    private WebSocketUtils() {
    }

    /**
     * 设置默认同步请求超时时间（由框架内部调用）
     *
     * @param timeoutSeconds 超时时间（秒）
     */
    public static void setDefaultSyncTimeout(long timeoutSeconds) {
        defaultSyncTimeoutSeconds = timeoutSeconds;
    }

    /**
     * 添加通道，如果已存在相同键的旧通道则关闭旧通道。
     * <p>
     * 替换经 {@link ChannelMap#put} 在写锁内原子完成：
     * 检测旧通道、关闭旧通道、写入新通道不存在 check-then-act 竞态，
     * 并发重复注册同键时不会出现两条活连接互相覆盖或旧连接漏关。
     * </p>
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @param chc    通道上下文
     */
    public static void addMap(String type, String cliKey, ChannelHandlerContext chc) {
        if (type == null || cliKey == null || chc == null) {
            return;
        }
        ChannelHandlerContext oldCtx = map.put(type, cliKey, chc);
        if (oldCtx != null && oldCtx != chc) {
            oldCtx.close();
            log.debug("重复链接，关闭旧通道 [type={}, cliKey={}]", type, maskKey(cliKey));
        }
    }

    /**
     * 移除并关闭指定通道
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @param ctx    当前通道上下文
     */
    public static void del(String type, String cliKey, ChannelHandlerContext ctx) {
        if (type == null || cliKey == null || ctx == null) {
            return;
        }
        ChannelHandlerContext storedCtx = map.get(type, cliKey);
        if (storedCtx != null && storedCtx.channel().id().equals(ctx.channel().id())) {
            map.del(type, cliKey);
            // 与契约一致：移除的同时关闭通道连接（对已断开的通道 close 为幂等空操作）
            storedCtx.close();
            log.debug("通道已移除 [type={}, cliKey={}]", type, maskKey(cliKey));
        }
    }

    /**
     * 向指定类型的所有客户端发送消息
     *
     * @param type 类型标识
     * @param msg  消息内容
     */
    public static void sendMsg(String type, String msg) {
        if (type == null || msg == null) {
            return;
        }
        List<ChannelHandlerContext> ctxList = map.get(type);
        if (ctxList == null || ctxList.isEmpty()) {
            log.debug("无可用通道 [type={}]", type);
            return;
        }
        int successCount = 0;
        for (ChannelHandlerContext ctx : ctxList) {
            if (ctx != null && ctx.channel().isActive()) {
                ctx.writeAndFlush(new TextWebSocketFrame(msg)).addListener(future -> {
                    if (!future.isSuccess()) {
                        // 写失败必须可观测，避免调用方误以为消息已送达
                        log.warn("消息写入失败 [type={}]", type, future.cause());
                    }
                });
                successCount++;
            }
        }
        log.debug("消息发送完成 [type={}, 成功数={}]", type, successCount);
    }

    /**
     * 向指定客户端发送消息
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @param msg    消息内容
     */
    public static boolean sendMsg(String type, String cliKey, String msg) {
        if (type == null || cliKey == null || msg == null) {
            return false;
        }
        ChannelHandlerContext ctx = map.get(type, cliKey);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(new TextWebSocketFrame(msg)).addListener(future -> {
                if (!future.isSuccess()) {
                    log.warn("消息写入失败 [type={}, cliKey={}]", type, maskKey(cliKey), future.cause());
                }
            });
            log.debug("消息发送成功 [type={}, cliKey={}]", type, maskKey(cliKey));
            return true;
        }
        log.debug("通道不可用 [type={}, cliKey={}]", type, maskKey(cliKey));
        return false;
    }

    /**
     * 向指定类型的所有客户端发送二进制消息（Protobuf 帧，codec=proto 连接使用）
     *
     * @param type 类型标识
     * @param data 二进制数据
     */
    public static void sendMsg(String type, byte[] data) {
        if (type == null || data == null) {
            return;
        }
        List<ChannelHandlerContext> ctxList = map.get(type);
        if (ctxList == null || ctxList.isEmpty()) {
            log.debug("无可用通道 [type={}]", type);
            return;
        }
        int successCount = 0;
        for (ChannelHandlerContext ctx : ctxList) {
            if (ctx != null && ctx.channel().isActive()) {
                ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data))).addListener(future -> {
                    if (!future.isSuccess()) {
                        log.warn("二进制消息写入失败 [type={}]", type, future.cause());
                    }
                });
                successCount++;
            }
        }
        log.debug("二进制消息发送完成 [type={}, 成功数={}]", type, successCount);
    }

    /**
     * 向指定客户端发送二进制消息（Protobuf 帧，codec=proto 连接使用）
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @param data   二进制数据
     * @return true 发送成功
     */
    public static boolean sendMsg(String type, String cliKey, byte[] data) {
        if (type == null || cliKey == null || data == null) {
            return false;
        }
        ChannelHandlerContext ctx = map.get(type, cliKey);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data))).addListener(future -> {
                if (!future.isSuccess()) {
                    log.warn("二进制消息写入失败 [type={}, cliKey={}]", type, maskKey(cliKey), future.cause());
                }
            });
            log.debug("二进制消息发送成功 [type={}, cliKey={}]", type, maskKey(cliKey));
            return true;
        }
        log.debug("通道不可用 [type={}, cliKey={}]", type, maskKey(cliKey));
        return false;
    }

    /**
     * 向指定客户端发送 Protobuf 信封消息，自动组装 {@link com.simple.common.websocket.proto.WebSocketFrame}。
     * <p>
     * 适用于 codec=proto 连接的服务端主动推送：业务只需提供 type/cliKey 与 data 字节，
     * 框架自动包上信封。requestId 非空时表示同步请求，客户端需携带相同 requestId 回复。
     * </p>
     *
     * @param type      类型标识
     * @param cliKey    客户端标识
     * @param requestId 同步请求ID，可为 null
     * @param data      业务负载字节
     * @return true 发送成功
     */
    public static boolean sendProtoMsg(String type, String cliKey, String requestId, byte[] data) {
        if (type == null || cliKey == null || data == null) {
            return false;
        }
        com.simple.common.websocket.proto.WebSocketFrame.Builder builder =
                com.simple.common.websocket.proto.WebSocketFrame.newBuilder()
                        .setType(type)
                        .setCliKey(cliKey)
                        .setData(ByteString.copyFrom(data));
        if (requestId != null && !requestId.isEmpty()) {
            builder.setRequestId(requestId);
        }
        return sendMsg(type, cliKey, builder.build().toByteArray());
    }

    /**
     * 同步发送消息并等待客户端回复。
     * <p>
     * 服务端向指定客户端发送请求消息，阻塞等待客户端处理完成后返回结果。
     * 适用于服务端主动向客户端发起命令调用、请求数据等场景。
     * 使用默认超时时间（simple.websocket.sync.timeout）。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * // 向客户端发送命令，同步等待结果
     * String result = WebSocketUtils.sendSyncMsg("agent", "agent-001", commandData);
     * }</pre>
     *
     * @param <T>    返回数据类型
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @param data   请求数据
     * @return 客户端返回的数据
     * @throws RuntimeException 客户端不在线、超时或执行异常时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T sendSyncMsg(String type, String cliKey, Object data) {
        return sendSyncMsg(type, cliKey, data, defaultSyncTimeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 同步发送消息并等待客户端回复（指定超时时间）。
     * <p>
     * 服务端向指定客户端发送请求消息，阻塞等待客户端处理完成后返回结果。
     * 底层基于 Correlation ID + CompletableFuture 实现：
     * 生成唯一 requestId 注入消息，客户端回复时携带相同 requestId，
     * 框架自动匹配并唤醒等待线程。
     * </p>
     *
     * <h3>客户端协议约定：</h3>
     * <p>
     * 服务端与客户端统一使用 {@link WebSocketRequest} 格式收发消息。
     * 服务端发起同步请求时，消息中 requestId 字段非空；
     * 客户端处理完成后回复消息时需携带相同的 requestId，
     * 框架据此匹配对应的等待线程并唤醒。
     * </p>
     *
     * @param <T>     返回数据类型
     * @param type    类型标识
     * @param cliKey  客户端标识
     * @param data    请求数据
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 客户端返回的数据
     * @throws RuntimeException 客户端不在线、超时或执行异常时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T sendSyncMsg(String type, String cliKey, Object data, long timeout, TimeUnit unit) {

        // 校验同步管理器已初始化
        if (syncManager == null) {
            throw new RuntimeException("WebSocketSyncManager 未初始化，请确保服务已启动");
        }

        // 生成唯一请求ID
        String requestId = IdUtils.getSnowflakeNextIdStr();

        // 创建 CompletableFuture 用于阻塞等待
        CompletableFuture<Object> future = new CompletableFuture<>();

        // 注册到同步管理器并携带超时截止时间：任何退出路径（含载荷构建异常）都能被清理任务回收
        syncManager.register(requestId, future, System.currentTimeMillis() + unit.toMillis(timeout));

        // 构建消息并发送：按连接编解码方式选择文本(JSON)或二进制(Protobuf)帧
        boolean sent;
        try {
            if (isProtoCodec(type, cliKey)) {
                // 二进制通道：Protobuf 信封承载，data 为 byte[] 时直接透传，否则按 JSON bytes 封装
                byte[] payload = data instanceof byte[] ? (byte[]) data : JsonUtils.toJsonStr(data).getBytes(StandardCharsets.UTF_8);
                com.simple.common.websocket.proto.WebSocketFrame frame =
                        com.simple.common.websocket.proto.WebSocketFrame.newBuilder()
                                .setType(type)
                                .setCliKey(cliKey)
                                .setRequestId(requestId)
                                .setData(ByteString.copyFrom(payload))
                                .build();
                sent = sendMsg(type, cliKey, frame.toByteArray());
            } else {
                // 文本通道：使用 WebSocketRequest 统一格式，客户端统一以此格式收发
                WebSocketRequest<Object> request = new WebSocketRequest<>();
                request.setRequestId(requestId);

                // 兼容处理：若 data 为 JSON 字符串，自动 parse 为 Map 避免 JSON 序列化时二次转义
                Object payload = data;
                if (data instanceof String str) {
                    String trimmed = str.trim();
                    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        try {
                            payload = JsonUtils.toJsonObj(trimmed, Map.class);
                        } catch (Exception e) {
                            log.debug("data 字符串非合法 JSON，保持原样发送 [error={}]", e.getMessage());
                        }
                    }
                }
                request.setData(payload);
                String json = JsonUtils.toJsonStr(request);
                sent = sendMsg(type, cliKey, json);
            }
        } catch (RuntimeException e) {
            // 载荷构建/序列化异常同样要回收注册，否则 pendingRequests 悬挂至清理任务兜底
            syncManager.cancel(requestId);
            throw e;
        }

        if (!sent) {

            // 发送失败，清理 Future
            syncManager.cancel(requestId);
            throw new RuntimeException("客户端不在线 [type=" + type + ", cliKey=" + maskKey(cliKey) + "]");
        }

        try {

            // 阻塞等待客户端回复
            return (T) future.get(timeout, unit);
        } catch (TimeoutException e) {

            // 超时，清理 Future
            syncManager.cancel(requestId);
            throw new RuntimeException("同步请求超时 [type=" + type + ", cliKey=" + maskKey(cliKey) + ", timeout=" + timeout + unit.name().toLowerCase() + "]", e);
        } catch (Exception e) {

            // 其他异常，清理 Future
            syncManager.cancel(requestId);
            throw new RuntimeException("同步请求异常 [type=" + type + ", cliKey=" + maskKey(cliKey) + "]", e);
        }
    }

    /**
     * 清理无效通道。
     * <p>
     * 基于条目快照遍历；移除前二次校验存储的通道仍是快照中该失效通道——
     * 遍历期间客户端可能已用相同 type+cliKey 重连并注册新通道，不能误删新连接。
     * </p>
     *
     * @return 清理的通道数量
     */
    public static int cleanInactiveChannels() {
        int cleanedCount = 0;
        List<Map.Entry<String, Map.Entry<String, ChannelHandlerContext>>> entries = map.entries();
        for (Map.Entry<String, Map.Entry<String, ChannelHandlerContext>> entry : entries) {
            String type = entry.getKey();
            String cliKey = entry.getValue().getKey();
            ChannelHandlerContext ctx = entry.getValue().getValue();
            if (ctx == null || !ctx.channel().isActive()) {

                // 仅当存储的仍是该失效通道时才移除，防止误删遍历期间重连的新通道
                ChannelHandlerContext currentCtx = map.get(type, cliKey);
                if (currentCtx == ctx) {
                    map.del(type, cliKey);
                    cleanedCount++;
                    log.debug("清理无效通道 [type={}, cliKey={}]", type, maskKey(cliKey));
                }
            }
        }
        if (cleanedCount > 0) {
            log.info("无效通道清理完成，清理数量: {}，剩余通道数: {}", cleanedCount, map.size());
        }
        return cleanedCount;
    }

    /**
     * 获取当前连接总数
     *
     * @return 连接数
     */
    public static int getConnectionCount() {
        return map.size();
    }

    /**
     * 检查指定客户端是否在线
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @return 是否在线
     */
    public static boolean isOnline(String type, String cliKey) {
        ChannelHandlerContext ctx = map.get(type, cliKey);
        return ctx != null && ctx.channel().isActive();
    }

    /**
     * 判断指定客户端连接是否使用 Protobuf 编解码
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @return true 表示该连接以 codec=proto 建立
     */
    private static boolean isProtoCodec(String type, String cliKey) {
        ChannelHandlerContext ctx = map.get(type, cliKey);
        if (ctx == null) {
            return false;
        }
        Object codec = ctx.channel().attr(AttributeKey.valueOf(WebSocketConstant.ATTR_CODEC)).get();
        return WebSocketConstant.CODEC_PROTO.equalsIgnoreCase(codec == null ? null : codec.toString());
    }

    /**
     * 清空所有通道并关闭对应连接。
     * <p>
     * 先逐个关闭通道持有的连接（单个关闭失败仅记录日志，不中断其余通道关闭），
     * 再清空内存注册表，避免残留 TCP 连接以及服务端失忆后客户端消息无路由。
     * </p>
     */
    public static void clearAll() {
        List<Map.Entry<String, Map.Entry<String, ChannelHandlerContext>>> entries = map.entries();

        // 逐个关闭通道连接，单个失败不影响其余
        for (Map.Entry<String, Map.Entry<String, ChannelHandlerContext>> entry : entries) {
            ChannelHandlerContext ctx = entry.getValue().getValue();
            if (ctx == null) {
                continue;
            }
            try {
                ctx.close();
            } catch (Exception e) {
                log.warn("通道关闭失败 [type={}, cliKey={}]", entry.getKey(), entry.getValue().getKey(), e);
            }
        }

        map.clear();
        log.info("所有通道连接已关闭并清空");
    }

    /**
     * 对敏感键进行脱敏处理（公共方法）
     *
     * @param key 原始键
     * @return 脱敏后的键
     */
    public static String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "****";
        }
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
    }
}

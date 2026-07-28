package com.simple.common.websocket.utils;

import com.simple.common.core.utils.IdUtils;
import com.simple.common.core.utils.JsonUtils;
import com.simple.common.websocket.common.channel.ChannelMap;
import com.simple.common.websocket.common.entity.WebSocketRequest;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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

    /**
     * 设置默认同步请求超时时间（由框架内部调用）
     *
     * @param timeoutSeconds 超时时间（秒）
     */
    public static void setDefaultSyncTimeout(long timeoutSeconds) {
        defaultSyncTimeoutSeconds = timeoutSeconds;
    }

    /**
     * 添加通道，如果已存在相同键的旧通道则关闭旧通道
     *
     * @param type   类型标识
     * @param cliKey 客户端标识
     * @param chc    通道上下文
     */
    public static void addMap(String type, String cliKey, ChannelHandlerContext chc) {
        ChannelHandlerContext oldCtx = map.get(type, cliKey);
        if (oldCtx != null && oldCtx.channel().isActive()) {
            oldCtx.close();
            log.debug("重复链接，关闭旧通道 [type={}, cliKey={}]", type, maskKey(cliKey));
        }
        map.add(type, cliKey, chc);
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
                ctx.writeAndFlush(new TextWebSocketFrame(msg));
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
            ctx.writeAndFlush(new TextWebSocketFrame(msg));
            log.debug("消息发送成功 [type={}, cliKey={}]", type, maskKey(cliKey));
            return true;
        }
        log.debug("通道不可用 [type={}, cliKey={}]", type, maskKey(cliKey));
        return false;
    }

    /**
     * 同步发送消息并等待客户端回复。
     * <p>
     * 服务端向指定客户端发送请求消息，阻塞等待客户端处理完成后返回结果。
     * 适用于服务端主动向客户端发起命令调用、请求数据等场景。
     * 使用默认超时时间 30 秒。
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

        // 注册到同步管理器，等待客户端回复
        syncManager.register(requestId, future);

        // 构建消息：使用 WebSocketRequest 统一格式，客户端统一以此格式收发
        WebSocketRequest<Object> request = new WebSocketRequest<>();
        request.setRequestId(requestId);
        request.setData(data);
        String json = JsonUtils.toJsonStr(request);

        // 发送消息到客户端
        boolean sent = sendMsg(type, cliKey, json);
        if (!sent) {

            // 发送失败，清理 Future
            syncManager.cancel(requestId);
            throw new RuntimeException("客户端不在线 [type=" + type + ", cliKey=" + cliKey + "]");
        }

        log.debug("同步请求已发送 [type={}, cliKey={}, requestId={}]", type, maskKey(cliKey), requestId);

        try {

            // 阻塞等待客户端回复
            return (T) future.get(timeout, unit);
        } catch (TimeoutException e) {

            // 超时，清理 Future
            syncManager.cancel(requestId);
            throw new RuntimeException("同步请求超时 [type=" + type + ", cliKey=" + cliKey + ", timeout=" + timeout + unit.name().toLowerCase() + "]");
        } catch (Exception e) {

            // 其他异常，清理 Future
            syncManager.cancel(requestId);
            throw new RuntimeException("同步请求异常 [type=" + type + ", cliKey=" + cliKey + "]", e);
        }
    }

    /**
     * 清理无效通道
     *
     * @return 清理的通道数量
     */
    public static int cleanInactiveChannels() {
        int cleanedCount = 0;
        List<Map.Entry<String, ChannelHandlerContext>> entries = map.entries();
        for (Map.Entry<String, ChannelHandlerContext> entry : entries) {
            ChannelHandlerContext ctx = entry.getValue();
            if (ctx == null || !ctx.channel().isActive()) {
                String keyStr = entry.getKey();
                String type = map.parseKey1(keyStr);
                String cliKey = map.parseKey2(keyStr);
                map.del(type, cliKey);
                cleanedCount++;
                log.debug("清理无效通道 [type={}, cliKey={}]", type, maskKey(cliKey));
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
     * 清空所有通道
     */
    public static void clearAll() {
        map.clear();
        log.info("所有通道已清空");
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
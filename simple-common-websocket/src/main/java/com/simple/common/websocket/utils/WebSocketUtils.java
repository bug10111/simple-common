package com.simple.common.websocket.utils;

import com.simple.common.websocket.common.channel.ChannelMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * WebSocket通讯通道管理工具类
 *
 * @author qty
 */
@Slf4j
public class WebSocketUtils {

    private static final ChannelMap<String, String, ChannelHandlerContext> map = new ChannelMap<>();

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
     * 对敏感键进行脱敏处理
     *
     * @param key 原始键
     * @return 脱敏后的键
     */
    private static String maskKey(String key) {
        if (key == null || key.length() <= 4) {
            return "****";
        }
        return key.substring(0, 2) + "****" + key.substring(key.length() - 2);
    }
}

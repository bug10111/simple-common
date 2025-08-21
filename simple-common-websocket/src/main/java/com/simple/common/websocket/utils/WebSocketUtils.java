package com.simple.common.websocket.utils;

import com.simple.common.websocket.common.channel.ChannelMap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Created with IntelliJ IDEA
 * Description: websocket 通讯通道管理帮助类
 *
 * @author 兄台丶请冷静
 */
@Slf4j
public class WebSocketUtils {
    private static final ChannelMap<String, String, ChannelHandlerContext> map = new ChannelMap<>();

    /**
     * 添加通道，如果有旧的通道则关闭相同的旧通道
     *
     * @param type    type
     * @param cliKey 客户端
     * @param chc    通道
     */
    public static void addMap(String type, String cliKey, ChannelHandlerContext chc) {
        ChannelHandlerContext channelHandlerContext = map.get(type, cliKey);
        if (channelHandlerContext != null) {
            channelHandlerContext.close();
            log.debug("重复链接，关闭旧通道 [{}] ==>> [{}] ", type, cliKey);
        }
        map.add(type, cliKey, chc);
    }

    /**
     * 关闭指定通道
     *
     * @param type    type
     * @param cliKey 客户端key
     * @param ctx    通道
     */
    public static void del(String type, String cliKey, ChannelHandlerContext ctx) {
        ChannelHandlerContext channelHandlerContext = map.get(type, cliKey);
        if (channelHandlerContext != null && channelHandlerContext.channel().compareTo(ctx.channel()) == 0) {
            channelHandlerContext.close();
            log.debug("关闭通道 [{}] ==>> [{}] ", type, cliKey);
            map.del(type, cliKey);
        }
    }

    /**
     * 发送消息
     *
     * @param type type
     * @param msg 消息内容
     */
    public static void sendMsg(String type, String msg) {
        log.debug("消息发送 [{}] ====> [{}]", type, msg);
        List<ChannelHandlerContext> chc = map.get(type);
        if (chc != null) {
            for (ChannelHandlerContext cc : chc) {
                cc.writeAndFlush(new TextWebSocketFrame(msg));
            }
        }
    }
}

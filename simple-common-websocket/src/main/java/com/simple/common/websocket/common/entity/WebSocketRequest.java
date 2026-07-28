package com.simple.common.websocket.common.entity;

import com.simple.common.websocket.utils.WebSocketUtils;
import lombok.Data;

/**
 * WebSocket请求实体，框架在消息到达时自动填充通道上下文信息。
 * <p>
 * 业务监听方法可通过 {@link #getType()} / {@link #getCliKey()} 获取当前通道标识，
 * 通过 {@link #reply(String)} 向当前客户端点对点推送消息（如处理进度）。
 * </p>
 *
 * @param <T> 请求数据类型
 * @author qty
 */
@Data
public class WebSocketRequest<T> {

    /**
     * 请求数据
     */
    private T data;

    /**
     * 通道类型，与 {@link com.simple.common.websocket.common.annotation.WebSocketListening#type()} 对应。
     * 由框架在消息到达时从 Channel 属性自动填充，业务代码无需手动设置。
     */
    private String type;

    /**
     * 客户端标识，与 {@link com.simple.common.websocket.common.annotation.WebSocketListening#cliKey()} 对应。
     * 由框架在消息到达时从 Channel 属性自动填充，业务代码无需手动设置。
     */
    private String cliKey;

    /**
     * 同步请求的唯一标识。
     * <p>
     * 当服务端通过 {@link WebSocketUtils#sendSyncMsg(String, String, Object, long, java.util.concurrent.TimeUnit)}
     * 发起同步请求时，框架自动生成此ID并填充到消息中。客户端处理完成后回复消息时需携带相同的 requestId，
     * 框架据此匹配对应的等待线程并唤醒。
     * 非同步消息时此字段为 null，向后兼容。
     * </p>
     */
    private String requestId;

    /**
     * 向当前客户端点对点推送消息。
     * <p>
     * 适用于长耗时处理中推送中间进度、状态更新等场景。
     * 底层调用 {@link WebSocketUtils#sendMsg(String, String, String)}，
     * 基于 type + cliKey 精确匹配当前通道，不会广播到其他客户端。
     * </p>
     *
     * @param msg 消息内容（JSON字符串或纯文本）
     * @return true 发送成功，false 通道不可用
     */
    public boolean reply(String msg) {
        return WebSocketUtils.sendMsg(this.type, this.cliKey, msg);
    }

}
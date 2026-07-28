package com.simple.common.websocket.common.entity;

import com.simple.common.core.utils.AssertUtils;
import com.simple.common.websocket.utils.WebSocketUtils;
import lombok.Data;

/**
 * WebSocket 统一消息实体，服务端与客户端双向通信均使用此格式。
 * <p>
 * <b>客户端 → 服务端：</b>
 * 消息到达时，框架自动从 Channel 属性填充 {@link #type} 和 {@link #cliKey}，
 * 业务监听方法可通过 {@link #reply(String)} 向当前客户端点对点推送消息。
 * </p>
 * <p>
 * <b>服务端 → 客户端：</b>
 * 通过 {@link WebSocketUtils#sendMsg(String, String, Object)} 或
 * {@link WebSocketUtils#sendSyncMsg(String, String, Object)} 发送，
 * 此时 {@link #type} 和 {@link #cliKey} 可能为 null，{@link #requestId} 非空时表示同步请求。
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
     * <p>
     * 客户端 → 服务端时由框架自动填充，服务端 → 客户端时可能为 null。
     * </p>
     */
    private String type;

    /**
     * 客户端标识，与 {@link com.simple.common.websocket.common.annotation.WebSocketListening#cliKey()} 对应。
     * <p>
     * 客户端 → 服务端时由框架自动填充，服务端 → 客户端时可能为 null。
     * </p>
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
     * @return true 发送成功
     * @throws com.simple.common.core.exception.DefaultException 当 type 或 cliKey 为 null 时
     */
    public boolean reply(String msg) {

        // 断言 type 和 cliKey 非空，有通道信息才能发送消息
        AssertUtils.notNull(this.type, "type 不能为空，reply 需要通道类型信息");
        AssertUtils.notNull(this.cliKey, "cliKey 不能为空，reply 需要客户端标识信息");

        return WebSocketUtils.sendMsg(this.type, this.cliKey, msg);
    }

}
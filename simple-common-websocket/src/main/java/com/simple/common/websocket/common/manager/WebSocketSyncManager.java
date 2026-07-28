package com.simple.common.websocket.common.manager;

import java.util.concurrent.CompletableFuture;

/**
 * WebSocket 同步请求管理器接口。
 * <p>
 * 负责管理服务端发起的同步请求与客户端回复的关联。
 * 服务端调用 {@link com.simple.common.websocket.utils.WebSocketUtils#sendSyncMsg}
 * 时，框架生成唯一 requestId 并注册 CompletableFuture 到本管理器；
 * 客户端回复消息携带相同 requestId 时，框架查询本管理器找到对应的 Future 并完成，
 * 从而唤醒阻塞等待的业务线程。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 默认实现 {@link com.simple.common.websocket.manager.DefaultWebSocketSyncManager}
 * 使用 ConcurrentHashMap 存储待处理请求并支持超时自动清理。
 * 如需自定义超时策略或清理策略，可实现本接口并替换默认实现。
 * </p>
 *
 * @author qty
 */
public interface WebSocketSyncManager {

    /**
     * 注册一个待处理的同步请求。
     * <p>
     * 服务端发起同步请求时调用，将 requestId 与 CompletableFuture 关联存储。
     * 当客户端回复消息携带相同 requestId 时，框架调用 {@link #complete} 唤醒等待线程。
     * </p>
     *
     * @param requestId 请求唯一标识，由雪花算法生成
     * @param future    对应的 CompletableFuture，业务线程阻塞在 future.get() 上
     */
    void register(String requestId, CompletableFuture<Object> future);

    /**
     * 完成一个同步请求。
     * <p>
     * 客户端回复消息到达时调用，查找对应的 CompletableFuture 并调用 complete() 唤醒等待线程。
     * 完成后自动从待处理集合中移除。
     * </p>
     *
     * @param requestId 请求唯一标识
     * @param response  客户端返回的响应数据
     * @return true 表示成功匹配并完成，false 表示 requestId 不存在（可能已超时被清理）
     */
    boolean complete(String requestId, Object response);

    /**
     * 取消一个同步请求。
     * <p>
     * 当发送失败、超时或发生异常时调用，清理对应的 CompletableFuture 并释放资源。
     * </p>
     *
     * @param requestId 请求唯一标识
     */
    void cancel(String requestId);
}
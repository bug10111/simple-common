package com.simple.common.websocket.common.manager;

import java.util.concurrent.CompletableFuture;

/**
 * WebSocket 同步请求管理器接口。
 * <p>
 * 负责管理服务端发起的同步请求与客户端回复的关联。
 * 服务端调用 {@link com.simple.common.websocket.utils.WebSocketUtils#sendSyncMsg(String, String, Object)}
 * 时，框架生成唯一 requestId 并注册 CompletableFuture 到本管理器；
 * 客户端回复消息携带相同 requestId 时，框架查询本管理器找到对应的 Future 并完成，
 * 从而唤醒阻塞等待的业务线程。
 * </p>
 *
 * <h3>扩展方式：</h3>
 * <p>
 * 默认实现 {@link com.simple.common.websocket.manager.DefaultWebSocketSyncManager}
 * 使用 ConcurrentHashMap 存储待处理请求并支持按超时截止时间自动清理。
 * 如需自定义超时策略或清理策略，可实现本接口并替换默认实现。
 * </p>
 *
 * <h3>线程安全契约：</h3>
 * <p>
 * 实现<strong>必须线程安全</strong>：{@link #register} 由任意业务线程调用，
 * {@link #complete} 由 Netty I/O 线程调用，{@link #cancel} 可能来自任意线程，
 * 内部结构不得使用非并发容器（如裸 HashMap）。
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
     * <p>
     * 参数契约：requestId 与 future 均不可为 null（违规抛 {@link IllegalArgumentException}）；
     * 同一 requestId 重复注册时，实现应取消旧的 Future 并以新注册为准（不静默覆盖丢句柄）。
     * </p>
     *
     * @param requestId 请求唯一标识，由雪花算法生成
     * @param future    对应的 CompletableFuture，业务线程阻塞在 future.get() 上
     */
    void register(String requestId, CompletableFuture<Object> future);

    /**
     * 注册一个待处理的同步请求（携带超时截止时间）。
     * <p>
     * 截止时间供实现的超时清理任务使用：到期仍未完成的注册应被回收
     * （唤醒等待线程并移除），保证任何未正常 cancel 的注册最终都会被清理，不泄漏内存。
     * 默认实现委托给 {@link #register(String, CompletableFuture)}，由具体实现决定截止语义。
     * </p>
     *
     * @param requestId      请求唯一标识，由雪花算法生成
     * @param future         对应的 CompletableFuture，业务线程阻塞在 future.get() 上
     * @param deadlineMillis 超时截止时间（epoch 毫秒）
     */
    default void register(String requestId, CompletableFuture<Object> future, long deadlineMillis) {
        register(requestId, future);
    }

    /**
     * 完成一个同步请求。
     * <p>
     * 客户端回复消息到达时调用，查找对应的 CompletableFuture 并调用 complete() 唤醒等待线程。
     * 完成后自动从待处理集合中移除。
     * </p>
     * <p>
     * 返回值契约：true 表示成功匹配到注册并完成唤醒；
     * false 表示该 requestId 不存在、已超时被清理、或对应的 Future 已完成/已取消
     * （如客户端重复回复、回复晚于超时）。requestId 为 null 时抛 NPE，实现可自行前置校验。
     * </p>
     *
     * @param requestId 请求唯一标识
     * @param response  客户端返回的响应数据
     * @return true 成功匹配并完成唤醒；false requestId 不存在或对应 Future 已终结
     */
    boolean complete(String requestId, Object response);

    /**
     * 取消一个同步请求。
     * <p>
     * 当发送失败、超时或发生异常时调用，清理对应的 CompletableFuture 并释放资源。
     * 实现契约：必须对已注册的 Future 执行 {@code cancel(true)}（取消等待中的线程），
     * 保证阻塞在 future.get() 上的业务线程被唤醒而不是悬挂到自身超时；
     * requestId 不存在或对应 Future 已完成时为幂等空操作。
     * </p>
     *
     * @param requestId 请求唯一标识
     */
    void cancel(String requestId);
}

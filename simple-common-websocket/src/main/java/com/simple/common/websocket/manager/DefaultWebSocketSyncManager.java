package com.simple.common.websocket.manager;

import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * WebSocket 同步请求管理器默认实现。
 * <p>
 * 使用 ConcurrentHashMap 存储待处理的同步请求，保证线程安全。
 * 每个注册携带超时截止时间，由定时任务对到期/已终结的注册统一回收，
 * 防止调用方异常退出路径遗漏 cancel 造成的内存泄漏。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class DefaultWebSocketSyncManager implements WebSocketSyncManager {

    private final WebSocketProperties properties;

    /**
     * 待处理请求：requestId → (Future, 超时截止时间)
     */
    private final ConcurrentHashMap<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 定时清理任务句柄，用于应用关闭时取消（跨线程读写，volatile 保证可见性）
     */
    private volatile ScheduledFuture<?> cleanFuture;

    public DefaultWebSocketSyncManager(WebSocketProperties properties) {
        this.properties = properties;
    }

    /**
     * 初始化：注册自身到 WebSocketUtils 并启动超时清理任务
     */
    @PostConstruct
    public void init() {

        // 将自身注册到 WebSocketUtils，供 sendSyncMsg 静态方法调用
        WebSocketUtils.setSyncManager(this);

        // 将同步请求默认超时时间传递给 WebSocketUtils
        WebSocketProperties.SyncConfig syncConfig = properties.getSync();
        WebSocketUtils.setDefaultSyncTimeout(syncConfig.getTimeout());

        // 启动定时清理任务，对已终结或已过截止时间的注册进行回收
        cleanFuture = ThreadUtils.scheduleWithFixedDelaySafe(this::cleanTimeoutRequests, syncConfig.getInitialDelay(), syncConfig.getCleanInterval(), TimeUnit.SECONDS);
    }

    /**
     * 应用关闭时取消定时任务，唤醒所有等待线程，并解除 WebSocketUtils 的静态引用
     */
    @PreDestroy
    public void shutdown() {
        if (cleanFuture != null && !cleanFuture.isCancelled()) {
            cleanFuture.cancel(false);
        }

        // 唤醒所有仍在阻塞等待的调用线程，避免其悬挂至自身超时；随后清空注册表
        pendingRequests.forEach((requestId, pending) ->
                pending.future().completeExceptionally(new IllegalStateException("WebSocket服务已关闭，同步请求被终止 [requestId=" + requestId + "]")));
        pendingRequests.clear();

        // 解除静态引用，避免容器重启/热部署场景残留指向已销毁 Bean 的旧实例
        WebSocketUtils.setSyncManager(null);
        log.info("同步请求清理定时任务已取消，待处理请求已全部唤醒，静态引用已解除");
    }

    @Override
    public void register(String requestId, CompletableFuture<Object> future) {

        // 未显式指定截止时间的注册：按配置的默认超时时间计算
        register(requestId, future, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(properties.getSync().getTimeout()));
    }

    @Override
    public void register(String requestId, CompletableFuture<Object> future, long deadlineMillis) {
        if (requestId == null || future == null) {
            throw new IllegalArgumentException("requestId 与 future 均不可为 null");
        }

        // 冲突检测：同一 requestId 重复注册时取消旧 Future（旧等待线程立即被唤醒），以新注册为准
        PendingRequest previous = pendingRequests.putIfAbsent(requestId, new PendingRequest(future, deadlineMillis));
        if (previous != null) {
            log.warn("同步请求ID冲突，已取消旧的待处理请求 [requestId={}]", requestId);
            previous.future().cancel(true);
        }
        log.debug("同步请求已注册 [requestId={}]", requestId);
    }

    @Override
    public boolean complete(String requestId, Object response) {
        PendingRequest pending = pendingRequests.remove(requestId);
        if (pending != null) {
            boolean completed = pending.future().complete(response);
            log.debug("同步请求已完成 [requestId={}, completed={}]", requestId, completed);
            return completed;
        }
        log.debug("同步请求未找到，可能已超时清理 [requestId={}]", requestId);
        return false;
    }

    @Override
    public void cancel(String requestId) {
        PendingRequest pending = pendingRequests.remove(requestId);
        if (pending != null) {

            // cancel(true) 中断等待线程：调用方（如 future.get 阻塞线程）立即被唤醒，不悬挂到自身超时
            pending.future().cancel(true);
            log.debug("同步请求已取消 [requestId={}]", requestId);
        }
    }

    /**
     * 清理到期与已终结的注册。
     * <p>
     * 已完成/已取消的注册仅移除；已过截止时间仍未完成的注册，
     * 异常完成（唤醒等待线程）后一并移除，兑现"超时自动清理防内存泄漏"的类契约。
     * </p>
     */
    private void cleanTimeoutRequests() {
        if (pendingRequests.isEmpty()) {
            return;
        }
        int beforeSize = pendingRequests.size();
        long now = System.currentTimeMillis();
        pendingRequests.entrySet().removeIf(entry -> {
            PendingRequest pending = entry.getValue();
            if (pending.future().isDone()) {
                return true;
            }
            if (now > pending.deadlineMillis()) {
                pending.future().completeExceptionally(new TimeoutException("同步请求等待超时，已被清理任务回收 [requestId=" + entry.getKey() + "]"));
                return true;
            }
            return false;
        });
        int afterSize = pendingRequests.size();
        if (beforeSize != afterSize) {
            log.debug("同步请求清理完成 [清理前={}, 清理后={}]", beforeSize, afterSize);
        }
    }

    /**
     * 获取当前待处理请求数量（用于监控）
     *
     * @return 待处理请求数
     */
    public int getPendingCount() {
        return pendingRequests.size();
    }

    /**
     * 待处理请求条目：Future + 超时截止时间（epoch 毫秒）
     */
    private record PendingRequest(CompletableFuture<Object> future, long deadlineMillis) {
    }
}

package com.simple.common.websocket.manager;

import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.websocket.common.manager.WebSocketSyncManager;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 同步请求管理器默认实现。
 * <p>
 * 使用 ConcurrentHashMap 存储待处理的同步请求，保证线程安全。
 * 支持超时自动清理，防止内存泄漏。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultWebSocketSyncManager implements WebSocketSyncManager {

    private final WebSocketProperties properties;

    /**
     * 待处理请求映射：requestId → CompletableFuture
     */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 定时清理任务句柄，用于应用关闭时取消
     */
    private ScheduledFuture<?> cleanFuture;

    public DefaultWebSocketSyncManager(@Autowired WebSocketProperties properties) {
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

        // 启动定时清理任务，定期清理已超时或已取消的 Future
        cleanFuture = ThreadUtils.scheduleWithFixedDelaySafe(this::cleanTimeoutRequests, syncConfig.getInitialDelay(), syncConfig.getCleanInterval(), TimeUnit.SECONDS);
    }

    /**
     * 应用关闭时取消定时清理任务
     */
    @PreDestroy
    public void shutdown() {
        if (cleanFuture != null && !cleanFuture.isCancelled()) {
            cleanFuture.cancel(false);
            log.info("同步请求清理定时任务已取消");
        }
    }

    @Override
    public void register(String requestId, CompletableFuture<Object> future) {
        pendingRequests.put(requestId, future);
        log.debug("同步请求已注册 [requestId={}]", requestId);
    }

    @Override
    public boolean complete(String requestId, Object response) {
        CompletableFuture<Object> future = pendingRequests.remove(requestId);
        if (future != null) {
            boolean completed = future.complete(response);
            log.debug("同步请求已完成 [requestId={}, completed={}]", requestId, completed);
            return completed;
        }
        log.debug("同步请求未找到，可能已超时 [requestId={}]", requestId);
        return false;
    }

    @Override
    public void cancel(String requestId) {
        CompletableFuture<Object> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.cancel(true);
            log.debug("同步请求已取消 [requestId={}]", requestId);
        }
    }

    /**
     * 清理超时请求。
     * <p>
     * 遍历所有待处理请求，对已完成或已取消的 Future 进行清理。
     * 已完成的 Future 不会阻塞后续处理，但需要从 Map 中移除防止内存泄漏。
     * </p>
     */
    private void cleanTimeoutRequests() {
        int beforeSize = pendingRequests.size();
        if (beforeSize == 0) {
            return;
        }

        // 遍历并移除已完成或已取消的 Future
        pendingRequests.entrySet().removeIf(entry -> {
            CompletableFuture<Object> future = entry.getValue();
            return future.isDone() || future.isCancelled();
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
}
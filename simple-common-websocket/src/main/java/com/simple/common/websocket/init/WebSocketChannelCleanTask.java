package com.simple.common.websocket.init;

import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket无效通道清理定时任务
 * <p>
 * 定期扫描并清理已断开但未正确移除的通道。
 * 任务具备幂等性：ApplicationReadyEvent 重复发布时不会重复调度；
 * 停机时取消任务并中断正在执行的清理。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelCleanTask implements ApplicationListener<ApplicationReadyEvent> {

    private final WebSocketProperties webSocketProperties;

    /**
     * 定时任务句柄，用于应用关闭时取消。
     * 调度线程写入、停机线程读取，volatile 保证停机线程不读到过期 null 而漏取消。
     */
    private volatile ScheduledFuture<?> scheduledFuture;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        WebSocketProperties.CleanConfig cleanConfig = webSocketProperties.getClean();

        // 配置为 null（自定义器/测试场景可置 null）或未启用时直接跳过，不因纯优化任务中断启动
        if (cleanConfig == null || !cleanConfig.isEnabled()) {
            log.info("WebSocket无效通道清理任务已禁用");
            return;
        }

        // 调度参数防御：非法值会让 ScheduledThreadPoolExecutor 抛 IllegalArgumentException 导致启动失败
        if (cleanConfig.getInterval() <= 0 || cleanConfig.getInitialDelay() < 0) {
            log.error("WebSocket无效通道清理任务配置非法，任务不启动 [interval={}, initialDelay={}]",
                    cleanConfig.getInterval(), cleanConfig.getInitialDelay());
            return;
        }

        // 幂等保护：事件重复发布（上下文刷新/测试重复触发）时不再叠加调度，
        // 否则首个任务句柄被覆盖丢失，停机时无法取消
        if (scheduledFuture != null && !scheduledFuture.isDone() && !scheduledFuture.isCancelled()) {
            log.warn("WebSocket无效通道清理定时任务已在运行，忽略重复启动");
            return;
        }
        log.info("启动WebSocket无效通道清理定时任务，初始延迟: {}秒，间隔: {}秒",
                cleanConfig.getInitialDelay(), cleanConfig.getInterval());
        scheduledFuture = ThreadUtils.scheduleWithFixedDelaySafe(this::cleanInactiveChannels,
                cleanConfig.getInitialDelay(), cleanConfig.getInterval(), TimeUnit.SECONDS);
    }

    /**
     * 应用关闭时取消定时任务
     */
    @PreDestroy
    public void shutdown() {
        ScheduledFuture<?> future = this.scheduledFuture;
        if (future != null && !future.isCancelled()) {

            // cancel(true) 中断正在执行的清理，避免停机期间清理任务与 Netty 资源释放交叉运行
            future.cancel(true);
            log.info("WebSocket无效通道清理定时任务已取消");
        }
    }

    /**
     * 执行清理任务
     */
    private void cleanInactiveChannels() {
        try {
            int cleanedCount = WebSocketUtils.cleanInactiveChannels();
            if (cleanedCount > 0) {
                log.info("定时清理完成，清理无效通道: {}个，当前连接数: {}", cleanedCount, WebSocketUtils.getConnectionCount());
            }
        } catch (Exception e) {
            log.error("清理无效通道异常", e);
        }
    }
}

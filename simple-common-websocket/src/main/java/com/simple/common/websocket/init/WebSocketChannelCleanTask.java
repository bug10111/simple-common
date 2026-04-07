package com.simple.common.websocket.init;

import com.simple.common.core.utils.ThreadUtils;
import com.simple.common.websocket.common.properties.WebSocketProperties;
import com.simple.common.websocket.utils.WebSocketUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket无效通道清理定时任务
 * <p>
 * 定期扫描并清理已断开但未正确移除的通道
 *
 * @author qty
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelCleanTask implements ApplicationListener<ApplicationReadyEvent> {

    private final WebSocketProperties webSocketProperties;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        WebSocketProperties.CleanConfig cleanConfig = webSocketProperties.getClean();
        if (!cleanConfig.isEnabled()) {
            log.info("WebSocket无效通道清理任务已禁用");
            return;
        }
        log.info("启动WebSocket无效通道清理定时任务，初始延迟: {}秒，间隔: {}秒",
                cleanConfig.getInitialDelay(), cleanConfig.getInterval());
        ThreadUtils.scheduleWithFixedDelay(
                this::cleanInactiveChannels,
                cleanConfig.getInitialDelay(),
                cleanConfig.getInterval(),
                TimeUnit.SECONDS
        );
    }

    /**
     * 执行清理任务
     */
    private void cleanInactiveChannels() {
        try {
            int cleanedCount = WebSocketUtils.cleanInactiveChannels();
            if (cleanedCount > 0) {
                log.info("定时清理完成，清理无效通道: {}个，当前连接数: {}", 
                        cleanedCount, WebSocketUtils.getConnectionCount());
            }
        } catch (Exception e) {
            log.error("清理无效通道异常", e);
        }
    }
}

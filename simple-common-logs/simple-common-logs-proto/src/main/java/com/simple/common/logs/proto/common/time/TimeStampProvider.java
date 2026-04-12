package com.simple.common.logs.proto.common.time;

import com.simple.common.core.utils.ThreadUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间戳提供器
 * <p>
 * 每秒更新一次缓存的时间戳，供日志事件使用，避免每条日志都调用 System.currentTimeMillis()
 * 并创建 DateTime 对象。线程安全。
 * </p>
 *
 * @author qty
 */
@Slf4j
@Component
public class TimeStampProvider {

    /**
     * 缓存的最新时间戳（毫秒）
     */
    private final AtomicLong currentTimestamp = new AtomicLong(System.currentTimeMillis());

    /**
     * 定时更新任务的 Future，用于在销毁时取消任务
     */
    private ScheduledFuture<?> scheduledFuture;

    /**
     * 获取当前缓存的时间戳（毫秒）
     * <p>
     * 精度为秒级，适用于日志记录场景。
     * </p>
     *
     * @return 毫秒时间戳
     */
    public long getCurrentTimestamp() {
        return currentTimestamp.get();
    }

    /**
     * 启动后台定时任务，每秒更新缓存时间戳
     */
    @PostConstruct
    public void start() {
        // 定时批量处理（单位：毫秒），保存 Future 以便关闭时取消
        scheduledFuture = ThreadUtils.scheduleWithFixedDelayFuture(() -> {
            try {
                currentTimestamp.set(System.currentTimeMillis());
            } catch (Exception e) {
                log.error("TimeStampProvider时间更新失败: {}", e.getMessage());
            }
        }, 1, TimeUnit.SECONDS);
        log.debug("TimeStampProvider 已启动");
    }

    /**
     * 停止定时任务，释放资源
     */
    @PreDestroy
    public void stop() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            log.debug("TimeStampProvider 定时任务已取消");
        }
        log.debug("TimeStampProvider 已停止");
    }
}
package com.simple.common.logs.proto.common.time;

import com.simple.common.core.utils.ThreadUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间戳提供者
 * <p>
 * 每秒刷新一次缓存的时间戳，供日志事件使用，避免每条日志都调用 System.currentTimeMillis()
 * 并创建 DateTime 对象。线程安全。
 * </p>
 * <p>
 * 由 {@code com.simple.common.logs.proto.common.config.LogsProtoConfig} 以
 * {@code @Bean} 方式注册（带 {@code @ConditionalOnMissingBean} 条件），
 * 本类不再标注 {@code @Component}，避免与使用方的组件扫描重复注册。
 * </p>
 * <p>
 * 精度说明：定时任务采用固定延迟调度，缓存值与真实时钟的最大偏差约为
 * 刷新周期（1 秒）加调度延迟；对顺序性敏感的场景请直接使用 System.currentTimeMillis()。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class TimeStampProvider {

    /**
     * 缓存的最新时间戳（毫秒）
     */
    private final AtomicLong currentTimestamp = new AtomicLong(System.currentTimeMillis());

    /**
     * 定时更新任务的 Future，用于在销毁时取消任务
     * <p>
     * volatile 保证 start 所在初始化线程与 stop 所在销毁线程间的可见性，
     * 避免 stop 观察到 null 而漏取消任务。
     * </p>
     */
    private volatile ScheduledFuture<?> scheduledFuture;

    /**
     * 获取当前缓存的时间戳（毫秒）
     * <p>
     * 精度为秒级，适用于日志记录场景。缓存值最长滞后约一个刷新周期（1 秒）加调度延迟，
     * 对事件先后顺序敏感的场景请直接使用 System.currentTimeMillis()。
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
        // 幂等保护：已存在未取消的定时任务时直接返回，避免覆盖 Future 导致旧任务泄漏且无法停止
        ScheduledFuture<?> previous = scheduledFuture;
        if (previous != null && !previous.isCancelled()) {
            log.debug("TimeStampProvider 定时任务已存在，跳过重复启动");
            return;
        }
        // 定时刷新缓存时间戳（单位：毫秒），保存 Future 以便关闭时取消
        scheduledFuture = ThreadUtils.scheduleWithFixedDelayFuture(() -> {
            try {
                currentTimestamp.set(System.currentTimeMillis());
            } catch (Exception e) {
                // 携带完整堆栈记录，便于定位根因
                log.error("TimeStampProvider时间更新失败", e);
            }
        }, 1, TimeUnit.SECONDS);
        log.debug("TimeStampProvider 已启动");
    }

    /**
     * 停止定时任务，释放资源
     */
    @PreDestroy
    public void stop() {
        // 先置空再取消，保证 stop 后状态一致，且后续 start() 可正常重建
        ScheduledFuture<?> future = scheduledFuture;
        scheduledFuture = null;
        if (future != null) {
            future.cancel(false);
            log.debug("TimeStampProvider 定时任务已取消");
        }
        log.debug("TimeStampProvider 已停止");
    }
}
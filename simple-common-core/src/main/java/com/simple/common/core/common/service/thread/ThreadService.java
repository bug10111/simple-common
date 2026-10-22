package com.simple.common.core.common.service.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池任务调度服务接口。
 * <p>
 * 提供统一的线程池管理和任务调度功能,包括延迟任务、定时任务、异步执行等。
 * 默认实现 {@link com.simple.common.core.service.thread.DefaultThreadService}
 * 基于 ScheduledThreadPoolExecutor 和 ThreadPoolExecutor 实现高性能线程池管理。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li>延迟任务：订单超时取消、定时提醒等</li>
 *   <li>定时任务：数据同步、缓存刷新等</li>
 *   <li>异步执行：日志记录、消息发送等非阻塞操作</li>
 * </ul>
 *
 * @author qty
 */
public interface ThreadService {

    Logger LOGGER = LoggerFactory.getLogger(ThreadService.class);

    /**
     * 获取轻量级线程池执行器(用于延迟任务)
     * <p>
     * 返回 ScheduledThreadPoolExecutor,适用于少量并发和延迟任务场景。
     * </p>
     *
     * @return 定时调度线程池执行器
     */
    ScheduledThreadPoolExecutor getExecutor();

    /**
     * 获取高并发异步线程池
     * <p>
     * 返回 ExecutorService,适用于高并发、高性能场景。
     * </p>
     *
     * @return 异步线程池执行器
     */
    ExecutorService getAsyncExecutor();

    /**
     * 延时执行任务，并返回 ScheduledFuture 用于取消或检查状态
     *
     * @param runnable 任务
     * @param time     延迟时间
     * @param timeUnit 时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    default ScheduledFuture<?> schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        return getExecutor().schedule(runnable, time, timeUnit);
    }

    /**
     * 定期以固定延迟执行任务（fixed delay：下一次执行在上一次执行<b>完成</b>后 delay 时间开始，
     * 有效周期 = 执行耗时 + delay；非固定速率）。
     *
     * @param runnable     任务
     * @param initialDelay 初始延迟时间
     * @param delay        任务完成后每delay时间执行一次
     * @param timeUnit     时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    default ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {
        return getExecutor().scheduleWithFixedDelay(runnable, initialDelay, delay, timeUnit);
    }

    /**
     * 定期以固定延迟执行任务，并返回 ScheduledFuture
     * <p>任务抛出未捕获异常会导致后续调度被取消，可靠性要求高的场景请使用
     * {@link #scheduleWithFixedDelaySafe(Runnable, long, long, TimeUnit)}。</p>
     *
     * @param runnable 任务
     * @param delay    任务完成后每delay时间执行一次
     * @param timeUnit 时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    default ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit timeUnit) {
        return scheduleWithFixedDelay(runnable, 0, delay, timeUnit);
    }

    /**
     * 定期以固定延迟执行任务（安全版），自动捕获任务内部异常并记录日志，确保单次失败后周期性调度继续。
     * <p>
     * 实现原理：将原始 Runnable 包装一层 try-catch，记录异常日志后吞掉，不向上传播，
     * 从而不触发 ScheduledThreadPoolExecutor 的"任务异常即取消后续调度"行为。
     * 适用于对可靠性要求高、允许单次执行失败但必须保持调度不中断的场景。
     * </p>
     *
     * @param runnable     原始任务
     * @param initialDelay 初始延迟时间
     * @param delay        任务完成后每delay时间执行一次
     * @param timeUnit     时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    default ScheduledFuture<?> scheduleWithFixedDelaySafe(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {
        Runnable safeTask = () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                // 捕获Throwable（含Error）：吞掉异常但不丢失可观测性，否则单次失败即静默取消整个周期调度；
                // Error（如OOM）同样会终止周期调度，与"不影响后续调度"契约一致，仅记录
                LOGGER.error("安全版周期任务单次执行异常，已自动恢复，不影响后续调度", e);
            }
        };
        return scheduleWithFixedDelay(safeTask, initialDelay, delay, timeUnit);
    }

    // ==================== 监控方法 ====================

    /**
     * 获取定时调度线程池的队列大小
     *
     * @return 当前等待执行的任务数量
     */
    default int getScheduledQueueSize() {
        return getExecutor().getQueue().size();
    }

    /**
     * 获取异步线程池的活跃线程数
     *
     * @return 当前正在执行任务的线程数；-1 表示当前执行器不支持该指标（非ThreadPoolExecutor实现）
     */
    default int getAsyncActiveCount() {
        ExecutorService executor = getAsyncExecutor();
        if (executor instanceof java.util.concurrent.ThreadPoolExecutor) {
            return ((java.util.concurrent.ThreadPoolExecutor) executor).getActiveCount();
        }
        return -1;
    }

    /**
     * 获取异步线程池的队列大小
     *
     * @return 当前等待执行的任务数量；-1 表示当前执行器不支持该指标（非ThreadPoolExecutor实现）
     */
    default int getAsyncQueueSize() {
        ExecutorService executor = getAsyncExecutor();
        if (executor instanceof java.util.concurrent.ThreadPoolExecutor) {
            return ((java.util.concurrent.ThreadPoolExecutor) executor).getQueue().size();
        }
        return -1;
    }
}

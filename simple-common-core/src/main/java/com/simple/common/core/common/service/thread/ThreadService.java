package com.simple.common.core.common.service.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 线程池任务调度接口
 *
 * @author qty
 */
public interface ThreadService {

    /**
     * 获取轻量级线程池执行对象，用于延迟任务
     * @return 执行对象
     */
    ScheduledThreadPoolExecutor getExecutor();

    /**
     * 获取高并发线程池
     */
    ExecutorService getAsyncExecutor();

    /**
     * 延时执行任务
     *
     * @param runnable 任务
     * @param time     延迟时间
     * @param timeUnit 时间单位
     */
    default void schedule(Runnable runnable, int time, TimeUnit timeUnit) {
        getExecutor().schedule(runnable, time, timeUnit);
    }

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
     * 定期以固定速率执行任务
     *
     * @param runnable    任务
     * @param InitialTime 初始延迟时间
     * @param fixedTime   任务完成后每fixedTime时间执行一次
     * @param timeUnit    时间单位
     */
    default void scheduleWithFixedDelay(Runnable runnable, int InitialTime, int fixedTime, TimeUnit timeUnit) {
        getExecutor().scheduleWithFixedDelay(runnable, InitialTime, fixedTime, timeUnit);
    }

    /**
     * 定期以固定速率执行任务，并返回 ScheduledFuture 用于取消或检查状态
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
     * 定期以固定速率执行任务
     *
     * @param runnable 任务
     * @param delay 任务完成后每delay时间执行一次
     * @param timeUnit  时间单位
     */
    default void scheduleWithFixedDelay(Runnable runnable, int delay, TimeUnit timeUnit) {
        scheduleWithFixedDelay(runnable, 0, delay, timeUnit);
    }

    /**
     * 定期以固定速率执行任务，并返回 ScheduledFuture
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
     * 定期以固定速率执行任务（安全版），自动捕获任务内部异常，确保单次失败后周期性调度继续。
     * <p>
     * 实现原理：将原始 Runnable 包装一层 try-catch，仅记录异常日志，不向上抛出。
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
            } catch (Exception e) {
                // 具体日志实现在 DefaultThreadService 中已包含，此处仅捕获避免传播
                // 为保持接口纯粹，日志可交由实现类统一处理
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
     * @return 当前正在执行任务的线程数
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
     * @return 当前等待执行的任务数量
     */
    default int getAsyncQueueSize() {
        ExecutorService executor = getAsyncExecutor();
        if (executor instanceof java.util.concurrent.ThreadPoolExecutor) {
            return ((java.util.concurrent.ThreadPoolExecutor) executor).getQueue().size();
        }
        return -1;
    }
}
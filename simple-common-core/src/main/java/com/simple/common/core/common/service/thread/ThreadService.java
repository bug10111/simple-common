package com.simple.common.core.common.service.thread;

import java.util.concurrent.ExecutorService;
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
     * 关闭线程池
     */
    void shutdown();

    /**
     * 延时执行任务
     *
     * @param runnable     任务
     * @param time     延迟时间
     * @param timeUnit 时间单位
     */
    default void schedule(Runnable runnable, int time, TimeUnit timeUnit){
        getExecutor().schedule(runnable, time, timeUnit);
    }

    /**
     * 定期以固定速率执行任务
     *
     * @param runnable        任务
     * @param InitialTime 初始延迟时间
     * @param fixedTime   任务完成后每fixedTime时间执行一次
     * @param timeUnit    时间单位
     */
    default void scheduleWithFixedDelay(Runnable runnable, int InitialTime, int fixedTime, TimeUnit timeUnit){
        getExecutor().scheduleWithFixedDelay(runnable, InitialTime, fixedTime, timeUnit);
    }

    /**
     * 定期以固定速率执行任务
     *
     * @param runnable      任务
     * @param fixedTime 任务完成后每fixedTime时间执行一次
     * @param timeUnit  时间单位
     */
    default void scheduleWithFixedDelay(Runnable runnable, int fixedTime, TimeUnit timeUnit) {
        scheduleWithFixedDelay(runnable, 0, fixedTime, timeUnit);
    }
}

package com.simple.common.core.common.service.thread;

import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: 线程池任务调度接口
 *
 * @author 兄台丶请冷静
 */
public interface ThreadService {

    /**
     * 延时执行任务
     *
     * @param task     任务
     * @param time     延迟时间
     * @param timeUnit 时间单位
     */
    void schedule(TimerTask task, int time, TimeUnit timeUnit);

    /**
     * 定期以固定速率执行任务
     *
     * @param task        任务
     * @param InitialTime 初始延迟时间
     * @param fixedTime   任务完成后每fixedTime时间执行一次
     * @param timeUnit    时间单位
     */
    void scheduleWithFixedDelay(TimerTask task, int InitialTime, int fixedTime, TimeUnit timeUnit);

    /**
     * 获取线程池执行对象
     * @return 执行对象
     */
    ScheduledThreadPoolExecutor getExecutor();

    /**
     * 关闭线程池
     */
    void shutdown();

    /**
     * 定期以固定速率执行任务
     *
     * @param task      任务
     * @param fixedTime 任务完成后每fixedTime时间执行一次
     * @param timeUnit  时间单位
     */
    default void scheduleWithFixedDelay(TimerTask task, int fixedTime, TimeUnit timeUnit) {
        scheduleWithFixedDelay(task, 0, fixedTime, timeUnit);
    }
}

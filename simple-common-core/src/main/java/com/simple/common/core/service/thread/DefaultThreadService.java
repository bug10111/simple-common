package com.simple.common.core.service.thread;

import com.simple.common.core.common.service.thread.ThreadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultThreadService implements ThreadService, InitializingBean {

    private ScheduledThreadPoolExecutor executor;

    private ExecutorService asyncExecutor;

    @Override
    public ScheduledThreadPoolExecutor getExecutor() {
        return this.executor;
    }

    @Override
    public ExecutorService getAsyncExecutor() {
        return this.executor;
    }

    /**
     * 停止任务线程池
     */
    @Override
    public void shutdown() {
        log.info("开始关闭线程池！");
        // 先关定时任务池
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // 再关异步任务池
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
        }

        try {
            // 等待所有任务结束
            if (executor != null && !executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (asyncExecutor != null && !asyncExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("在关机期间中断，强制关机。");
            executor.shutdownNow();
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("线程池关闭成功！");
    }

    @Override
    public void afterPropertiesSet() {

        /*
          设置核心线程数和拒绝策略
          CallerRunsPolicy：在调用线程中执行
          AbortPolicy：这是默认的策略。当线程池无法接受新任务时，会抛出 RejectedExecutionException
          DiscardPolicy：该策略会悄悄地丢弃无法执行的任务，不会抛出异常
          DiscardOldestPolicy：该策略会丢弃线程池中最旧的未处理任务，然后尝试提交当前任务
          如果服务器核心数为N（Runtime.getRuntime().availableProcessors()），线程池核心线程数设置推荐：
          1. 应用为io密集型
            核心线程数：2N + 1
            最大线程数：核心线程数 * 2到4
          2. 应用为cpu密集型：
            核心线程数：N或者N + 1
            最大线程数：核心线程数 * 1或者2
         */
        int cores = Runtime.getRuntime().availableProcessors();

        executor = new ScheduledThreadPoolExecutor(Math.max(2, cores), // 至少 2 个线程处理定时任务
                                                   new BasicThreadFactory.Builder().namingPattern("scheduled-pool-%d").daemon(true).build(), new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                printException(r, t);
            }
        };

        // 设置空闲线程存活时间
        executor.setKeepAliveTime(60, TimeUnit.SECONDS);

        // 2. 普通异步任务线程池：支持弹性扩容
        int corePoolSize = cores * 2 + 1;
        int maxPoolSize = corePoolSize * 4;
        this.asyncExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000), // 有界队列防内存溢出
                                                    new BasicThreadFactory.Builder().namingPattern("async-pool-%d").daemon(true).build(), new ThreadPoolExecutor.CallerRunsPolicy()
                                                    // 或根据业务选择 DiscardPolicy
        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                printException(r, t);
            }
        };
    }

    /**
     * 打印线程异常信息
     */
    private static void printException(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?>) {
            try {
                ((Future<?>) r).get();
            } catch (Throwable e) {
                t = e.getCause() != null ? e.getCause() : e;
            }
        }
        if (t != null) {
            log.error(t.getMessage(), t);
        }
    }
}

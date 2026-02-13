package com.simple.common.core.service.thread;

import com.simple.common.core.common.service.thread.ThreadService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
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
        return this.asyncExecutor;
    }

    @PreDestroy
    public void shutdown() {
        log.info("开始关闭线程池！");
        shutdownExecutor(executor);
        shutdownExecutor(asyncExecutor);
        log.info("线程池关闭流程执行完毕！");
    }

    /**
     * 停止任务线程池
     */
    public void shutdownExecutor(ExecutorService asyncExecutor) {
        if (asyncExecutor == null || asyncExecutor.isShutdown()) {
            return;
        }
        if (!asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {

                //中断主线程，等待任务执行完毕，最大等待时间120秒
                if (!asyncExecutor.awaitTermination(120, TimeUnit.SECONDS)) {

                    //终止失败，强行终止
                    asyncExecutor.shutdownNow();
                    if (!asyncExecutor.awaitTermination(120, TimeUnit.SECONDS)) {
                        log.info("线程池强制关闭失败！");
                    }
                }
            } catch (InterruptedException ie) {

                //有任何异常，强行终止任务
                asyncExecutor.shutdownNow();

                //恢复任务，重新执行
                Thread.currentThread().interrupt();
            }
        }
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
        int corePoolSize = cores * 2 + 1;
        int maxPoolSize = corePoolSize * 2;

        executor = new ScheduledThreadPoolExecutor(Math.max(2, cores), new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                printException(r, t);
            }
        };

        // 2. 普通异步任务线程池：支持弹性扩容
        this.asyncExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(200), new ThreadPoolExecutor.CallerRunsPolicy()

        ) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                printException(r, t);
            }
        };

        log.info("线程池初始化完成！");
    }

    /**
     * 打印线程异常信息
     */
    private static void printException(Runnable r, Throwable t) {
        if (t == null && r instanceof Future<?> future) {
            try {
                if (future.isDone()) {
                    future.get();
                }
            } catch (CancellationException ce) {
                t = ce;
            } catch (ExecutionException ee) {
                t = ee.getCause();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        if (t != null) {
            log.error("线程池任务执行异常", t);
        }
    }
}

package com.simple.common.core.service.thread;

import com.simple.common.core.common.properties.ThreadProperties;
import com.simple.common.core.common.service.thread.ThreadService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Slf4j
@Service
public class DefaultThreadService implements ThreadService, InitializingBean {

    @Autowired
    private ThreadProperties threadProperties;

    private ScheduledThreadPoolExecutor executor;

    private ExecutorService asyncExecutor;

    // 用于生成唯一线程名称后缀
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

    /**
     * 自定义线程工厂：用于定时调度线程池（守护线程）
     * <p>
     * 定时任务多为后台辅助性质，设置为守护线程可避免阻塞 JVM 正常退出。
     * </p>
     */
    private static final ThreadFactory SCHEDULED_THREAD_FACTORY = r -> {
        Thread t = new Thread(r, "simple-scheduled-" + THREAD_COUNTER.incrementAndGet());
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thread, ex) -> log.error("定时任务线程 [{}] 未捕获异常", thread.getName(), ex));
        return t;
    };

    /**
     * 自定义线程工厂：用于异步任务线程池（非守护线程）
     * <p>
     * 异步任务可能涉及核心业务逻辑，设为非守护线程以保证任务能够完成。
     * 配合优雅停机机制确保任务不丢失。
     * </p>
     */
    private static final ThreadFactory ASYNC_THREAD_FACTORY = r -> {
        Thread t = new Thread(r, "simple-async-" + THREAD_COUNTER.incrementAndGet());
        t.setDaemon(false);
        t.setUncaughtExceptionHandler((thread, ex) -> log.error("异步任务线程 [{}] 未捕获异常", thread.getName(), ex));
        return t;
    };

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
        // 属性缺失（极端初始化失败场景）时兜底，避免@PreDestroy内NPE掩盖关闭流程
        ThreadProperties properties = this.threadProperties != null ? this.threadProperties : new ThreadProperties();
        shutdownExecutor(executor, properties);
        shutdownExecutor(asyncExecutor, properties);
        log.info("线程池关闭流程执行完毕！");
    }

    /**
     * 停止任务线程池
     */
    public void shutdownExecutor(ExecutorService executorService, ThreadProperties properties) {
        if (executorService == null || executorService.isShutdown()) {
            return;
        }
        executorService.shutdown();
        try {
            // 等待秒数非法（<=0会让awaitTermination立即返回，静默跳过优雅排水）时收敛到默认值
            long awaitSeconds = Math.max(1, properties.getShutdownAwaitTerminationSeconds());
            // 中断主线程，等待任务执行完毕
            if (!executorService.awaitTermination(awaitSeconds, TimeUnit.SECONDS)) {
                // 终止失败，强行终止
                List<Runnable> droppedTasks = executorService.shutdownNow();
                if (!droppedTasks.isEmpty()) {
                    log.warn("线程池 [{}] 强制关闭时丢弃了 {} 个未执行的任务", executorService, droppedTasks.size());
                }
                long nowAwaitSeconds = Math.max(1, properties.getShutdownNowAwaitTerminationSeconds());
                if (!executorService.awaitTermination(nowAwaitSeconds, TimeUnit.SECONDS)) {
                    log.warn("线程池 [{}] 强制关闭后仍未完全终止", executorService);
                }
            }
        } catch (InterruptedException ie) {
            // 有中断发生，强行终止任务
            List<Runnable> droppedTasks = executorService.shutdownNow();
            if (!droppedTasks.isEmpty()) {
                log.warn("线程池 [{}] 因中断强制关闭，丢弃了 {} 个未执行的任务", executorService, droppedTasks.size());
            }
            // 恢复中断状态
            Thread.currentThread().interrupt();
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
        // 1. 定时任务线程池
        executor = new ScheduledThreadPoolExecutor(threadProperties.getScheduledCorePoolSize(), SCHEDULED_THREAD_FACTORY, new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                printException(r, t);
            }
        };
        executor.setKeepAliveTime(threadProperties.getScheduledKeepAliveSeconds(), TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(threadProperties.isScheduledAllowCoreThreadTimeOut());

        // 2. 普通异步任务线程池
        this.asyncExecutor = new ThreadPoolExecutor(threadProperties.getAsyncCorePoolSize(), threadProperties.getAsyncMaxPoolSize(), threadProperties.getAsyncKeepAliveSeconds(),
                                                    TimeUnit.SECONDS, new LinkedBlockingQueue<>(threadProperties.getAsyncQueueCapacity()), ASYNC_THREAD_FACTORY,
                                                    new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                printException(r, t);
            }
        };

        log.info("线程池初始化完成！定时线程核心数: {}, 异步线程核心数: {}, 最大线程数: {}, 队列容量: {}", threadProperties.getScheduledCorePoolSize(),
                 threadProperties.getAsyncCorePoolSize(), threadProperties.getAsyncMaxPoolSize(), threadProperties.getAsyncQueueCapacity());
    }

    /**
     * 打印线程异常信息
     * <p>
     * 注意：submit()/schedule() 提交的任务被 FutureTask 包装，异常被捕获进 Future 结果而非传入 t，
     * 因此这里必须解包 Future 才能观测到失败；仅依赖 t 会静默吞掉所有经 submit 的任务异常。
     * </p>
     */
    private static void printException(Runnable r, Throwable t) {
        if (t != null) {
            log.error("线程池任务执行异常", t);
            return;
        }
        // 解包FutureTask：submit/schedule路径的异常存在Future结果里，t恒为null
        if (r instanceof Future<?> future && future.isDone() && !future.isCancelled()) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.util.concurrent.ExecutionException e) {
                log.error("线程池任务执行异常(Future结果)", e.getCause());
            } catch (java.util.concurrent.CancellationException ignored) {
                // 任务被取消属正常路径
            }
        }
    }
}
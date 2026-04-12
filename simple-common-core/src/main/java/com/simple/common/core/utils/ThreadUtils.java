package com.simple.common.core.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.core.common.service.thread.ThreadService;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Created with IntelliJ  IDEA
 * Description: 多线程帮助类
 * CompletableFuture 常见用法：
 * 1. supplyAsync(Supplier<T>)：异步执行有返回值的任务。
 * 2. runAsync(Runnable)：异步执行无返回值的任务。
 * 3. thenApply(Function<T, R>)：对结果进行转换，有输入有输出。
 * 4. thenAccept(Consumer<T>)：消费结果，有输入无输出。
 * 5. thenRun(Runnable)：任务完成后执行操作，无输入无输出。
 * 6. thenCompose(Function<T, CompletableFuture<R>>)：扁平化嵌套异步任务（串行依赖）。
 * 7. thenCombine(CompletableFuture<U>, BiFunction<T, U, R>)：合并两个独立异步任务的结果。
 * 8. allOf(CompletableFuture<?>...)：等待所有任务完成。
 * 9. anyOf(CompletableFuture<?>...)：任意一个任务完成即触发。
 * 10. exceptionally(Function<Throwable, T>)：异常时提供默认结果。
 * 11. handle(BiFunction<T, Throwable, R>)：统一处理正常结果或异常。
 * 12. whenComplete(BiConsumer<T, Throwable>)：任务完成时执行回调（不改变结果）。
 * 13. join()：阻塞等待结果，若任务抛出异常，则将异常以 unchecked 形式（CompletionException）抛出。
 * 14. get()：阻塞等待结果，声明式抛出 checked 异常（InterruptedException, ExecutionException）。
 * 区别：
 * - join() 更简洁，适合在 lambda 或流式编程中使用，无需 try-catch。
 * - get() 是 Future 接口的标准方法，适合需要显式处理中断或执行异常的场景。
 *
 * @author qty
 */
public class ThreadUtils {

    /**
     * 异步执行任务，并使用高性能线程池
     *
     * @param supplier 待执行任务
     * @param <U>      需要返回的对象
     */
    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(supplier, getAsyncExecutor());
    }

    /**
     * 异步执行无返回值的任务，并使用高性能线程池
     */
    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, getAsyncExecutor());
    }

    /**
     * 延时执行任务
     *
     * @param runnable 任务
     * @param time     延迟时间
     * @param timeUnit 时间单位
     */
    public static void schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        getThreadService().schedule(runnable, time, timeUnit);
    }

    /**
     * 延时执行任务，并返回 ScheduledFuture 用于取消或检查状态
     *
     * @param runnable 任务
     * @param time     延迟时间
     * @param timeUnit 时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    public static ScheduledFuture<?> scheduleWithFuture(Runnable runnable, long time, TimeUnit timeUnit) {
        return getThreadService().schedule(runnable, time, timeUnit);
    }

    /**
     * 定期以固定速率执行任务
     *
     * @param runnable    任务
     * @param initialTime 初始延迟时间
     * @param fixedTime   任务完成后每fixedTime时间执行一次
     * @param timeUnit    时间单位
     */
    public static void scheduleWithFixedDelay(Runnable runnable, long initialTime, long fixedTime, TimeUnit timeUnit) {
        getThreadService().scheduleWithFixedDelay(runnable, initialTime, fixedTime, timeUnit);
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
    public static ScheduledFuture<?> scheduleWithFixedDelayFuture(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {
        return getThreadService().scheduleWithFixedDelay(runnable, initialDelay, delay, timeUnit);
    }

    /**
     * 定期以固定速率执行任务
     *
     * @param runnable  任务
     * @param fixedTime 任务完成后每fixedTime时间执行一次
     * @param timeUnit  时间单位
     */
    public static void scheduleWithFixedDelay(Runnable runnable, long fixedTime, TimeUnit timeUnit) {
        scheduleWithFixedDelay(runnable, 0, fixedTime, timeUnit);
    }

    /**
     * 定期以固定速率执行任务，并返回 ScheduledFuture 用于取消或检查状态
     *
     * @param runnable 任务
     * @param delay    任务完成后每delay时间执行一次
     * @param timeUnit 时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    public static ScheduledFuture<?> scheduleWithFixedDelayFuture(Runnable runnable, long delay, TimeUnit timeUnit) {
        return getThreadService().scheduleWithFixedDelay(runnable, 0, delay, timeUnit);
    }

    /**
     * 定期以固定速率执行任务，并自动捕获任务内部异常，保证单次失败后周期性调度继续。
     * <p>
     * 注意：此方法内部已包含 try-catch，使用者无需再自行处理异常，
     * 但业务异常将被记录日志，不会向上抛出。
     * </p>
     *
     * @param runnable     原始任务
     * @param initialDelay 初始延迟时间
     * @param delay        任务完成后每delay时间执行一次
     * @param timeUnit     时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    public static ScheduledFuture<?> scheduleWithFixedDelaySafe(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit) {
        return getThreadService().scheduleWithFixedDelaySafe(runnable, initialDelay, delay, timeUnit);
    }

    /**
     * 定期以固定速率执行任务（无初始延迟），并自动捕获任务内部异常，保证单次失败后周期性调度继续。
     *
     * @param runnable 原始任务
     * @param delay    任务完成后每delay时间执行一次
     * @param timeUnit 时间单位
     * @return ScheduledFuture 对象，可用于取消任务
     */
    public static ScheduledFuture<?> scheduleWithFixedDelaySafe(Runnable runnable, long delay, TimeUnit timeUnit) {
        return scheduleWithFixedDelaySafe(runnable, 0, delay, timeUnit);
    }

    /**
     * 获取线程池对象,可以结合CompletableFuture使用，一般用于少量并发和延迟任务
     */
    public static ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        return getThreadService().getExecutor();
    }

    /**
     * 获取线程池对象,可以结合CompletableFuture使用，一般用于高并发 高性能场景
     */
    public static ExecutorService getAsyncExecutor() {
        return getThreadService().getAsyncExecutor();
    }

    // ==================== 监控方法 ====================

    /**
     * 获取定时调度线程池的队列大小
     *
     * @return 当前等待执行的任务数量
     */
    public static int getScheduledQueueSize() {
        return getThreadService().getScheduledQueueSize();
    }

    /**
     * 获取异步线程池的活跃线程数
     *
     * @return 当前正在执行任务的线程数
     */
    public static int getAsyncActiveCount() {
        return getThreadService().getAsyncActiveCount();
    }

    /**
     * 获取异步线程池的队列大小
     *
     * @return 当前等待执行的任务数量
     */
    public static int getAsyncQueueSize() {
        return getThreadService().getAsyncQueueSize();
    }

    /**
     * 获取 ThreadService
     * <p>
     * 注意：每次调用均从 Spring 容器获取最新 Bean，不进行静态缓存，
     * 以避免容器刷新或关闭后引用失效的问题。
     * </p>
     */
    private static ThreadService getThreadService() {
        ThreadService bean = SpringUtil.getBean(ThreadService.class);
        AssertUtils.notEmpty(bean, "ThreadService 未加载，请在spring初始化完成后使用");
        return bean;
    }
}
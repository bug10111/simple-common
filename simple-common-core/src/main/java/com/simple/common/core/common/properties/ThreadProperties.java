package com.simple.common.core.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线程池配置属性
 * <p>
 * 支持通过 application.yml 或 application.properties 覆盖默认值。
 * 配置前缀：simple.thread-pool
 * </p>
 *
 * @author qty
 */
@Data
@Component
@ConfigurationProperties(prefix = "simple.thread-pool")
public class ThreadProperties {

    /**
     * 异步线程池核心线程数
     * <p>默认值：CPU核心数 * 2 + 1（适用于I/O密集型任务）</p>
     */
    private int asyncCorePoolSize = Runtime.getRuntime().availableProcessors() * 2 + 1;

    /**
     * 异步线程池最大线程数
     * <p>默认值：核心线程数 * 2</p>
     */
    private int asyncMaxPoolSize = 0; // 初始值设为0，在 @PostConstruct 中动态计算

    /**
     * 异步线程池队列容量
     * <p>默认值：2000</p>
     */
    private int asyncQueueCapacity = 2000;

    /**
     * 异步线程池空闲线程存活时间（秒）
     * <p>默认值：60</p>
     */
    private long asyncKeepAliveSeconds = 60L;

    /**
     * 定时调度线程池核心线程数
     * <p>默认值：min(4, max(2, CPU核心数 / 2))，适用于轻量级周期性任务</p>
     */
    private int scheduledCorePoolSize = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    /**
     * 定时调度线程池空闲线程存活时间（秒）
     * <p>默认值：60</p>
     */
    private long scheduledKeepAliveSeconds = 60L;

    /**
     * 是否允许定时调度线程池核心线程超时回收
     * <p>默认值：true（减少空闲资源占用）</p>
     */
    private boolean scheduledAllowCoreThreadTimeOut = true;

    /**
     * 线程池关闭时等待任务完成的最大时间（秒）
     * <p>默认值：120</p>
     */
    private long shutdownAwaitTerminationSeconds = 120L;

    /**
     * 线程池强制关闭后的二次等待时间（秒）
     * <p>默认值：5</p>
     */
    private long shutdownNowAwaitTerminationSeconds = 5L;

    /**
     * 在Spring属性注入完成后执行，统一做参数规范化：先校准核心线程数，再基于合法值派生最大线程数。
     * <p>
     * 解决两个隐蔽缺陷：(1) 派生先于校验时，非法core值（如负数）会先算出错误的max再被覆盖，
     * 丢失"max=2*core"的派生关系；(2) core极大时 {@code core * 2} int乘法溢出为负/0。
     * 其余入池参数（队列容量、保活秒数、调度核心数、停机等待秒数）在此一并规范化，
     * 避免非法值到 ThreadPoolExecutor 构造期才以模糊的IllegalArgumentException暴露。
     * </p>
     */
    @PostConstruct
    public void init() {
        // 1. 先规范核心线程数
        if (asyncCorePoolSize <= 0) {
            asyncCorePoolSize = 1;
        }

        // 2. 再基于合法core派生max：long运算避免大core下int溢出为负
        if (asyncMaxPoolSize == 0) {
            asyncMaxPoolSize = (int) Math.min(Integer.MAX_VALUE, (long) asyncCorePoolSize * 2);
        }

        // 3. 兜底关系校验与其余入池参数规范化（非法值收敛到安全默认，避免构造期异常）
        if (asyncMaxPoolSize < asyncCorePoolSize) {
            asyncMaxPoolSize = asyncCorePoolSize;
        }
        if (asyncQueueCapacity <= 0) {
            asyncQueueCapacity = 2000;
        }
        if (asyncKeepAliveSeconds < 0) {
            asyncKeepAliveSeconds = 60L;
        }
        if (scheduledCorePoolSize <= 0) {
            scheduledCorePoolSize = 1;
        }
        if (scheduledKeepAliveSeconds < 0) {
            scheduledKeepAliveSeconds = 60L;
        }
        if (shutdownAwaitTerminationSeconds < 0) {
            shutdownAwaitTerminationSeconds = 120L;
        }
        if (shutdownNowAwaitTerminationSeconds < 0) {
            shutdownNowAwaitTerminationSeconds = 5L;
        }
    }
}
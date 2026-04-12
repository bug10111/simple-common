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
     * 在 Spring 属性注入完成后执行，用于修正依赖字段的动态计算。
     * <p>
     * 解决用户仅配置 asyncCorePoolSize 时，asyncMaxPoolSize 仍保持字段初始化时计算值的隐蔽问题。
     * </p>
     */
    @PostConstruct
    public void init() {
        if (asyncMaxPoolSize == 0) {
            asyncMaxPoolSize = asyncCorePoolSize * 2;
        }

        // 添加校验逻辑
        if (asyncCorePoolSize <= 0) asyncCorePoolSize = 1;
        if (asyncMaxPoolSize < asyncCorePoolSize) asyncMaxPoolSize = asyncCorePoolSize;
    }
}
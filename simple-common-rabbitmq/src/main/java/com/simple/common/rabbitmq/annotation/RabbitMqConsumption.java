package com.simple.common.rabbitmq.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: RabbitMQ消息消费处理注解，使用需要设置手动ack模式
 * <p>
 * 消息确认由框架统一处理（见 {@code com.simple.common.rabbitmq.aspeck.RabbitMqProcessAspect}），
 * 业务方法无需也不允许自行调用 basicAck/basicNack。
 * </p>
 *
 * @author qty
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqConsumption {

    /**
     * 业务执行时长（单位由 {@link #timeUnit()} 决定，必须大于 0）。
     * 这个时间内，新的重复消息无法消费（防重锁持有中），超过这个时间后锁自动过期释放。
     * 正常消费完成后，锁会被更新为完成状态并按 {@link #effectiveTime()} 计时。
     * <p>
     * 该值同时决定锁续期间隔（约 businessTime/3 刷新一次），请按业务真实耗时留出余量。
     * </p>
     */
    int businessTime() default 10;

    /**
     * 完成状态有效时长，固定以秒为单位（不受 {@link #timeUnit()} 影响），用于判断重复消费的最大时间。
     * 业务成功后锁值会写入完成标记并按该时长保留，供后续重复消息做幂等判定与完成校验。
     */
    int effectiveTime() default 60 * 30;

    /**
     * 业务执行时长的单位，仅作用于 {@link #businessTime()}；{@link #effectiveTime()} 固定以秒为单位，不受本配置影响。
     * <p>
     * 建议使用 {@link TimeUnit#SECONDS} 及以上单位：{@link TimeUnit#NANOSECONDS}/{@link TimeUnit#MICROSECONDS}
     * 换算到 Redis 过期时间时会取整为 0（或不足 1 毫秒），会导致锁立即过期、防重失效，框架会直接拒绝该配置。
     * </p>
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重试次数，仅统计首次消费失败之后的重试次数（0 表示不重试，不允许负数）。
     * 重试次数耗尽后消息进入死信兜底（默认本地文件持久化），不再重投。
     */
    int retryCount() default 3;
}

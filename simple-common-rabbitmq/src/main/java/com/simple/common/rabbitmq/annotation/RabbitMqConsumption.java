package com.simple.common.rabbitmq.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Description: RabbitMQ消息消费处理注解，使用需要设置手动ack模式
 *
 * @author 兄台丶请冷静
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RabbitMqConsumption {

    /**
     * 业务执行时长。这个时间内，新的队列无法消费，超过这个时间后自动释放。正常消费后，会更新为有效时长
     */
    int businessTime() default 10;

    /**
     * 消息有效时长，用于判断重复消费的最大时间,单位秒
     */
    int effectiveTime() default 60 * 30;

    /**
     * 消息有效时长单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 重试次数
     */
    int retryCount() default 3;
}

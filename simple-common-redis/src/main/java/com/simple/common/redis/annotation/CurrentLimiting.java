package com.simple.common.redis.annotation;

import com.simple.common.redis.common.enums.CurrentLimitingRulesEnum;

import java.lang.annotation.*;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentLimiting {

    /**
     * 限流标志
     */
    CurrentLimitingRulesEnum key() default CurrentLimitingRulesEnum.URL;

    /**
     * 单位时长(秒)
     */
    int time() default 10;

    /**
     * 单位时长允许的请求次数
     */
    int sum() default 2;

    /**
     * 没有获取到令牌时，最大的等待时间
     */
    int waitingTime() default 5;

    /**
     * 最大等待时间后，抛出的异常
     */
    String waitingTimeErrorStr() default "";
}

package com.simple.common.redis.annotation;

import com.simple.common.redis.common.enums.CurrentLimitingRulesEnum;

import java.lang.annotation.*;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentLimiting {

    /**
     * 限流标识
     */
    CurrentLimitingRulesEnum key() default CurrentLimitingRulesEnum.URL;

    /**
     * 单位时长（秒），必须 >= 1；< 1 的取值编译期不拦截，运行期进入限流管理器时快速失败
     */
    int time() default 10;

    /**
     * 单位时长内允许的请求次数，必须 >= 1；< 1 的取值编译期不拦截，运行期进入限流管理器时快速失败
     */
    int sum() default 2;

    /**
     * 没有获取到令牌时，最大的等待时间（秒）
     * <p>
     * 默认 0 = 立即失败（fail-fast），不阻塞请求线程；
     * 该等待发生在请求线程内，配置过大（如误按毫秒填写 500）会长时间占用容器线程，
     * 请按秒语义谨慎设置
     */
    int waitingTime() default 0;

    /**
     * 最大等待时间后，抛出的异常提示文案
     */
    String waitingTimeErrorStr() default "";
}

package com.simple.common.redis.annotation;

import com.simple.common.core.response.R;
import com.simple.common.redis.common.constant.RedisCacheConstant;

import java.lang.annotation.*;

/**
 * Created by IntelliJ IDEA
 *
 * @author qty
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {

    /**
     * 缓存标志，默认请求url
     */
    String head() default RedisCacheConstant.REQ_URL;

    /**
     * 缓存时长
     */
    int cacheTime() default 10;

    /**
     * 追加随机时长的范围，0为不追加
     */
    int appendRandomDuration() default 5;

    /**
     * 缓存返回值得格式化对象
     */
    Class<?> aclass() default R.class;
}

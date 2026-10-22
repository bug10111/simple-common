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
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {

    /**
     * 缓存标识，默认使用请求 url
     */
    String head() default RedisCacheConstant.REQ_URL;

    /**
     * 缓存时长（秒），必须大于 0；Redis 侧对 0/负值 TTL 直接拒绝，
     * 配置非法时首次写入即抛异常，请勿配置 0 或负数
     */
    int cacheTime() default 10;

    /**
     * 追加随机时长上限（秒），用于打散过期时间防雪崩；
     * 随机区间为 [0, appendRandomDuration)（上限不含），0 或负数为不追加；
     * 建议不超过 cacheTime，否则实际 TTL 可能接近翻倍
     */
    int appendRandomDuration() default 5;

    /**
     * 缓存返回值的反序列化目标类型
     * <p>
     * 契约：必须与被注解方法的返回类型一致；未显式指定时默认按 {@link R} 反序列化，
     * 返回非 R 类型的方法若遗漏配置，会在缓存命中时反序列化出错误类型或抛出类型转换异常。
     * 注意：单一 Class 无法保留泛型信息，{@code R<List<T>>} 等泛型返回类型的元素
     * 会反序列化为 Map/List 结构，此类方法建议方法级配合真实返回类型处理
     * （切面已优先按方法返回类型反序列化，aclass 仅作后备）。
     */
    Class<?> aclass() default R.class;
}

package com.simple.common.redis.common.manager;

import com.simple.common.redis.annotation.RedisCache;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Redis缓存切面管理器接口。
 * <p>
 * 用于管理 {@link RedisCache} 注解的缓存key构建和缓存策略处理。
 * 默认实现 {@code DefaultRedisCacheAspectManager} 提供了
 * 基于方法参数的缓存key生成逻辑。
 * </p>
 *
 * @author qty
 */
public interface RedisCacheAspectManager {

    /**
     * 获取缓存的hash名称，在此处同时用作锁的key
     * <p>
     * 返回值契约：实现必须返回非 null、确定（同一方法+同一参数集返回相同值）、
     * 且按 方法+参数列表 唯一的 key——该值同时用作缓存值键与分布式锁键
     * （见 RedisCacheAspect），不确定性、null 或与参数无关的 key 会导致
     * 缓存串写或锁粒度过宽。
     *
     * @param redisCache 注解对象，非 null
     * @param joinPoint  切点对象，非 null；其参数可能为空数组
     * @return 非 null、确定且唯一的缓存 key（同时用作分布式锁 key）
     */
    String getCacheKey(RedisCache redisCache, ProceedingJoinPoint joinPoint);

    /**
     * 获取缓存时间
     *
     * @param cacheTime            基础缓存时间（秒），必须大于 0
     * @param appendRandomDuration 追加随机时长（秒），防止雪崩；0 或负数表示不追加随机时长
     * @return 最终缓存时间（秒），保证非 null 且大于 0
     */
    int getCacheTime(int cacheTime, int appendRandomDuration);

}

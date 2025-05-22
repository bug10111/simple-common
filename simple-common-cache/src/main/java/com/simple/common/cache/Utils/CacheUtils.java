package com.simple.common.cache.Utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.simple.common.cache.common.function.GetDBFunction;
import com.simple.common.cache.common.function.GetRedisFunction;
import com.simple.common.redis.common.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;

/**
 * Created with IntelliJ IDEA
 * Description: 本地缓存构建器
 *
 * @author qty
 */
@Slf4j
public class CacheUtils {

    private static volatile RedissonLockService redissonLockService;

    /**
     * 获取数据，这里的逻辑是，先获取redis数据，如果没有获取到，再从db获取，并将值存入redis
     *
     * @param request       请求参数
     * @param lockKey       锁
     * @param redisFunction redis获取数据
     * @param dbFunction    db获取数据
     * @param <T>           返回数据
     * @param <R>           请求参数
     */
    @SuppressWarnings("unchecked")
    public static <T, R> T get(R request, String lockKey, GetRedisFunction<T, R> redisFunction, GetDBFunction<T, R> dbFunction) {

        // 第一次缓存检查
        T cacheValue = redisFunction.get(request);
        if (cacheValue != null) {
            log.debug("一级缓存命中 key: {}", lockKey);
            return cacheValue;
        }

        // 获取分布式锁服务
        RedissonLockService lockService = getRedissonLockService();

        // 通过分布式锁执行临界区操作
        return (T) lockService.lockHaveValue(lockKey, () -> {
            // 双重检查缓存
            T lockedCacheValue = redisFunction.get(request);
            if (lockedCacheValue != null) {
                log.debug("一级缓存命中 key: {}", lockKey);
                return lockedCacheValue;
            }

            // 数据库查询
            log.debug("缓存未命中，查询数据库 key: {}", lockKey);
            T dbValue = dbFunction.get(request);

            // 设置缓存（包含空值处理）
            redisFunction.set(dbValue);
            return dbValue;
        });

    }

    /**
     * 双重检查锁获取Redisson服务
     */
    private static RedissonLockService getRedissonLockService() {
        if (redissonLockService == null) {
            synchronized (RedissonLockService.class) {
                if (redissonLockService == null) {
                    redissonLockService = SpringUtil.getBean(RedissonLockService.class);
                }
            }
        }
        return redissonLockService;
    }

    /**
     * 获取缓存时间，随机数是防止雪崩
     *
     * @param cacheTime            缓存时间
     * @param appendRandomDuration 随机数
     */
    public Integer getCacheTime(Integer cacheTime, Integer appendRandomDuration) {
        return cacheTime + RandomUtil.randomInt(0, appendRandomDuration);
    }
}

package com.simple.common.cache.Utils;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import com.simple.common.cache.common.function.GetDBFunction;
import com.simple.common.cache.common.function.GetRedisFunction;
import com.simple.common.redis.common.service.RedissonLockService;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

/**
 * 缓存工具类，提供两种缓存能力：
 * <ul>
 *     <li>分布式缓存（Redis + DB）：通过 {@link #get(Object, String, GetRedisFunction, GetDBFunction)} 实现缓存穿透保护，
 *         并支持通过 {@link #evict(Object, String, Consumer)} 主动删除缓存</li>
 *     <li>本地缓存（Caffeine）：通过静态方法直接创建/移除本地缓存实例，无需手动注入</li>
 * </ul>
 *
 * @author qty
 */
@Slf4j
public final class CacheUtils {

    private static volatile RedissonLockService redissonLockService;

    private CacheUtils() {
        // 工具类禁止实例化
    }

    // ==================== 本地缓存 API（静态门面） ====================

    /**
     * 创建本地手动加载缓存（Caffeine）
     * <p>
     * 直接通过 {@link LocalCacheFactory#getInstance()} 单例创建，无需 Spring 注入。
     *
     * @param cacheName    缓存唯一标识
     * @param configurator 配置函数（使用 lambda 表达式配置参数）
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 配置完成的 Cache 实例
     */
    public static <K, V> Cache<K, V> createLocalCache(String cacheName, Consumer<LocalCacheFactory.CacheSpec<K, V>> configurator) {
        return LocalCacheFactory.getInstance().createCache(cacheName, configurator);
    }

    /**
     * 创建本地自动加载缓存（Caffeine LoadingCache）
     * <p>
     * 直接通过 {@link LocalCacheFactory#getInstance()} 单例创建，无需 Spring 注入。
     *
     * @param cacheName    缓存唯一标识
     * @param loader       缓存加载器（定义如何加载数据）
     * @param configurator 配置函数
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 配置完成的 LoadingCache 实例
     */
    public static <K, V> LoadingCache<K, V> createLocalLoadingCache(String cacheName, CacheLoader<K, V> loader, Consumer<LocalCacheFactory.CacheSpec<K, V>> configurator) {
        return LocalCacheFactory.getInstance().createLoadingCache(cacheName, loader, configurator);
    }

    /**
     * 移除并失效指定的本地缓存实例
     * <p>
     * 移除后缓存注册表中不再保留该实例，并立即执行 Caffeine 的清理（失效全部条目、
     * 完成挂起的维护任务如移除监听器回调与统计回收）。
     * 注意：此前已持有该 Cache 引用的调用方仍持有的是一个已脱离注册表的实例，应停止使用。
     * 若缓存不存在，则静默忽略。
     *
     * @param cacheName 缓存名称
     * @param cacheType 缓存类型（手动/自动加载）
     * @return true 表示成功移除已存在的缓存，false 表示缓存不存在
     */
    public static boolean removeLocalCache(String cacheName, LocalCacheFactory.CacheType cacheType) {
        return LocalCacheFactory.getInstance().removeCache(cacheName, cacheType);
    }

    // ==================== 分布式缓存 API ====================

    /**
     * 获取数据，这里的逻辑是，先获取redis数据，如果没有获取到，再从db获取，并将值存入redis
     * <p>
     * 注意：lockKey 必须与 redisFunction 内部使用的缓存 key 保持一致，否则分布式锁无法正确保护缓存击穿场景。
     * <p>
     * 降级语义：Redis 读取/回写异常不会中断请求——读取异常时降级查询数据库，回写异常时仅记录日志并返回数据库数据；
     * 只有数据库查询本身或分布式锁执行异常时才向上抛出。
     *
     * @param request       请求参数
     * @param lockKey       分布式锁的 key，必须与缓存 key 相同
     * @param redisFunction redis获取数据与回写逻辑
     * @param dbFunction    db获取数据逻辑
     * @param <T>           返回数据类型
     * @param <R>           请求参数类型
     * @return 缓存或数据库中的数据（可能为 null，取决于 dbFunction 的实现）
     * @throws RuntimeException 当数据库查询或分布式锁执行异常时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T, R> T get(R request, String lockKey, GetRedisFunction<T, R> redisFunction, GetDBFunction<T, R> dbFunction) {
        if (StrUtil.isBlank(lockKey)) {
            throw new IllegalArgumentException("lockKey 不能为空");
        }

        // 第一次缓存检查（Redis 异常时降级查询数据库，不中断请求）
        T cacheValue = getQuietly(redisFunction, request, lockKey);
        if (cacheValue != null) {
            log.debug("一级缓存命中 key: {}", lockKey);
            return cacheValue;
        }

        // 获取分布式锁服务
        RedissonLockService lockService = getRedissonLockService();

        try {
            // 通过分布式锁执行临界区操作
            // 注意：lockHaveValue 返回值类型为 Object，此处强转是安全的，因业务逻辑保证了返回值类型为 T
            return (T) lockService.lockHaveValue(lockKey, () -> {
                // 双重检查缓存（Redis 异常时降级查询数据库，不中断请求）
                T lockedCacheValue = getQuietly(redisFunction, request, lockKey);
                if (lockedCacheValue != null) {
                    log.debug("获取锁后缓存命中 key: {}", lockKey);
                    return lockedCacheValue;
                }

                // 数据库查询
                log.info("缓存未命中，查询数据库 key: {}", lockKey);
                T dbValue = dbFunction.get(request);

                // 缓存回写（包含空值处理），回写失败只记录日志，不影响已成功获取的数据库数据
                try {
                    redisFunction.set(dbValue);
                } catch (Exception e) {
                    log.error("缓存回写失败，lockKey: {}", lockKey, e);
                }
                return dbValue;
            });
        } catch (Exception e) {
            log.error("缓存获取失败，lockKey: {}, request: {}", lockKey, request, e);
            throw new RuntimeException("获取缓存数据异常", e);
        }
    }

    /**
     * 读取 Redis 缓存，异常时降级返回 null（视为未命中）
     */
    private static <T, R> T getQuietly(GetRedisFunction<T, R> redisFunction, R request, String lockKey) {
        try {
            return redisFunction.get(request);
        } catch (Exception e) {
            log.warn("缓存读取失败，降级查询数据库 key: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 主动删除分布式缓存（用于数据更新时使缓存失效）
     * <p>
     * 注意：删除 key 必须与写入时的 key 一致，建议使用与 {@link #get} 相同的 lockKey。
     * 删除失败不会抛出异常（仅记录日志），调用方应检查返回值以确认缓存是否已真正失效，
     * 必要时执行重试或补偿逻辑。
     *
     * @param request      请求参数（用于定位缓存 key）
     * @param cacheKey     缓存 key（与 get 方法中的 lockKey 保持一致）
     * @param evictHandler 执行删除操作的回调（例如调用 redisTemplate.delete(key)）
     * @param <R>          请求参数类型
     * @return true 表示删除回调执行成功；false 表示删除失败（回调异常），调用方应自行决定是否重试
     */
    public static <R> boolean evict(R request, String cacheKey, Consumer<String> evictHandler) {
        if (StrUtil.isBlank(cacheKey)) {
            throw new IllegalArgumentException("cacheKey 不能为空");
        }
        if (evictHandler == null) {
            log.warn("evictHandler is null, skip evict for key: {}", cacheKey);
            return false;
        }
        try {
            log.info("主动删除缓存 key: {}", cacheKey);
            evictHandler.accept(cacheKey);
            return true;
        } catch (Exception e) {
            log.error("缓存删除失败，key: {}, request: {}", cacheKey, request, e);
            return false;
        }
    }

    /**
     * 双重检查锁获取Redisson服务
     */
    private static RedissonLockService getRedissonLockService() {
        if (redissonLockService == null) {
            synchronized (CacheUtils.class) {
                if (redissonLockService == null) {
                    redissonLockService = SpringUtil.getBean(RedissonLockService.class);
                }
            }
        }
        return redissonLockService;
    }

    /**
     * 获取缓存时间，随机数是防止雪崩
     * <p>
     * 使用示例：
     * <pre>
     *     int baseTime = 3600; // 基础缓存1小时
     *     int randomBound = 600; // 随机增加最多10分钟
     *     int expireTime = CacheUtils.getCacheTime(baseTime, randomBound);
     * </pre>
     *
     * @param cacheTime            基础缓存时间（秒），必须大于 0
     * @param appendRandomDuration 随机增加的上限（秒），0 或负数表示不追加随机时长
     * @return 最终的缓存过期时间（秒），结果保证为正数
     */
    public static int getCacheTime(int cacheTime, int appendRandomDuration) {
        if (cacheTime <= 0) {
            throw new IllegalArgumentException("cacheTime 必须为正数: " + cacheTime);
        }
        // 不追加随机时长时直接返回基础缓存时间，避免随机数上限非正数导致异常
        if (appendRandomDuration <= 0) {
            return cacheTime;
        }
        // 追加随机时长，打散过期时间点防止缓存雪崩；使用 long 计算防止大值下 int 溢出
        long result = (long) cacheTime + RandomUtil.randomInt(0, appendRandomDuration);
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}

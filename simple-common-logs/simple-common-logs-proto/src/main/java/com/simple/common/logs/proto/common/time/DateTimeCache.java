package com.simple.common.logs.proto.common.time;

import cn.hutool.core.date.DateTime;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DateTime 秒级缓存
 * <p>
 * 以秒为单位缓存 DateTime 的毫秒值，避免高频日志场景下重复构造。
 * 同一秒内所有调用返回毫秒值完全一致（该秒起点值）的 DateTime，
 * 且每次返回防御性副本：缓存实例互不共享，调用方的 {@code setTime(...)}
 * 等变更操作不会污染缓存、不影响其他线程。
 * </p>
 *
 * @author qty
 */
public final class DateTimeCache {

    /**
     * 缓存最大容量（保留最近 300 秒，防止无限增长）
     */
    private static final int MAX_CACHE_SIZE = 300;

    /**
     * 缓存 Map，key 为秒级时间戳（毫秒 / 1000）
     */
    private static final Map<Long, DateTime> CACHE = new ConcurrentHashMap<>();

    /**
     * 上次清理时间（单调时钟，纳秒）。使用 nanoTime 保证时钟回拨不会使清理永久失效。
     */
    private static final AtomicLong lastCleanupTime = new AtomicLong(System.nanoTime());

    private DateTimeCache() {
    }

    /**
     * 根据毫秒时间戳获取缓存的 DateTime 对象
     * <p>
     * 以秒为单位进行缓存：同一秒内返回毫秒值一致（该秒起点值）的防御性副本。
     * 按需清理过期缓存并做容量兜底，防止内存无限增长。
     * </p>
     *
     * @param timestampMs 毫秒时间戳
     * @return DateTime 对象
     */
    public static DateTime get(long timestampMs) {
        long secondKey = timestampMs / 1000;

        // computeIfAbsent 保证同一秒只有一个线程构建缓存条目，命中与未命中取值确定（均为该秒起点毫秒值），
        // 修复原先"取首个调用者的毫秒值"导致同秒内不同调用返回毫秒值漂移、取值依赖调用顺序的问题
        DateTime cached = CACHE.computeIfAbsent(secondKey, key -> new DateTime(key * 1000L));
        // 每次调用都触发清理检查（内部有每秒一次的频控，避免低流量时段缓存只增不减）
        cleanupIfNeeded();
        // 返回防御性副本：缓存的共享实例一旦被调用方 setTime(...) 修改会污染所有线程，
        // 修复共享可变 DateTime 被外部篡改后永久毒化缓存条目的安全隐患
        return new DateTime(cached.getTime());
    }

    /**
     * 按需清理过期缓存（超过 MAX_CACHE_SIZE 秒的条目），并对缓存容量做兜底约束。
     * <p>
     * 触发时基使用单调时钟（nanoTime），修复时钟回拨导致清理被永久跳过的问题；
     * 容量兜底淘汰解决时钟偏移产生"未来 key"时缓存无界增长的问题。
     * </p>
     */
    private static void cleanupIfNeeded() {
        long now = System.nanoTime();
        long last = lastCleanupTime.get();
        // 每秒最多清理一次（单调时钟，不受系统时钟回拨影响）
        if (now - last < 1_000_000_000L) {
            return;
        }
        // CAS 更新，确保只有一个线程执行清理
        if (lastCleanupTime.compareAndSet(last, now)) {
            long expireSecond = (System.currentTimeMillis() / 1000) - MAX_CACHE_SIZE;
            CACHE.keySet().removeIf(key -> key < expireSecond);
            // 容量兜底：时钟偏移产生的"未来 key"不会被时间条件淘汰，按 key（时间）从旧到新淘汰，保证缓存有界
            int oversize = CACHE.size() - MAX_CACHE_SIZE;
            if (oversize > 0) {
                CACHE.keySet().stream()
                        .sorted(Comparator.naturalOrder())
                        .limit(oversize)
                        .forEach(CACHE::remove);
            }
        }
    }
}
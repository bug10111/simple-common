package com.simple.common.logs.proto.common.time;

import cn.hutool.core.date.DateTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DateTime 秒级缓存
 * <p>
 * 以秒为单位缓存 DateTime 对象，避免高频日志场景下重复创建。
 * 同一秒内的日志共享同一个 DateTime 实例。
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
     * 上次清理时间（毫秒）
     */
    private static final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());

    private DateTimeCache() {
    }

    /**
     * 根据毫秒时间戳获取缓存的 DateTime 对象
     * <p>
     * 以秒为单位进行缓存，同一秒内返回相同对象。
     * 定期清理过期缓存，防止内存泄漏。
     * </p>
     *
     * @param timestampMs 毫秒时间戳
     * @return DateTime 对象
     */
    public static DateTime get(long timestampMs) {
        long secondKey = timestampMs / 1000;

        DateTime dateTime = CACHE.get(secondKey);
        if (dateTime == null) {
            dateTime = new DateTime(timestampMs);
            CACHE.put(secondKey, dateTime);
            // 触发清理检查
            cleanupIfNeeded();
        }
        return dateTime;
    }

    /**
     * 按需清理过期缓存（超过 MAX_CACHE_SIZE 秒的条目）
     */
    private static void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        long last = lastCleanupTime.get();
        // 每秒最多清理一次
        if (now - last < 1000) {
            return;
        }
        // CAS 更新，确保只有一个线程执行清理
        if (lastCleanupTime.compareAndSet(last, now)) {
            long expireSecond = (now / 1000) - MAX_CACHE_SIZE;
            CACHE.entrySet().removeIf(entry -> entry.getKey() < expireSecond);
        }
    }
}
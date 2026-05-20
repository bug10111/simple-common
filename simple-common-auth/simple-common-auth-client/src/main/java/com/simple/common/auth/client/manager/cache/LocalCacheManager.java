package com.simple.common.auth.client.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Caffeine 的本地缓存实现，通过 LocalCacheFactory 管理缓存实例。
 * <p>
 * 注意：Caffeine 不支持单条记录的独立过期时间，因此带有过期时间的 set 方法将忽略 seconds 参数，
 * 统一使用缓存实例的默认过期策略。如需精细控制过期时间，建议使用 RedisCacheManager。
 *
 * @author qty
 */
@Slf4j
public class LocalCacheManager implements CacheManager {

    private final LocalCacheFactory cacheFactory = LocalCacheFactory.getInstance();

    /**
     * 字符串类型缓存（用于基础 set/get 操作）
     */
    private final Cache<String, String> stringCache;

    /**
     * Hash 类型缓存（存储 Map<Object, Object>）
     */
    private final Cache<String, Map<Object, Object>> hashCache;

    /**
     * Set 类型缓存（存储 Set<String>）
     */
    private final Cache<String, Set<String>> setCache;

    /**
     * 默认构造器，使用预设的缓存配置。
     * 适用于 Spring 通过工厂创建时的默认场景。
     */
    public LocalCacheManager() {
        this.stringCache = cacheFactory.createCache("auth:string", spec -> spec.maximumSize(10000).expireAfterWrite(1800));
        this.hashCache = cacheFactory.createCache("auth:hash", spec -> spec.maximumSize(5000).expireAfterWrite(1800));
        this.setCache = cacheFactory.createCache("auth:set", spec -> spec.maximumSize(5000).expireAfterWrite(1800));
    }

    /**
     * 可自定义缓存实例的构造器，供工厂或测试使用。
     *
     * @param stringCache 字符串缓存实例
     * @param hashCache   Hash 缓存实例
     * @param setCache    Set 缓存实例
     */
    public LocalCacheManager(Cache<String, String> stringCache, Cache<String, Map<Object, Object>> hashCache, Cache<String, Set<String>> setCache) {
        this.stringCache = stringCache;
        this.hashCache = hashCache;
        this.setCache = setCache;
    }

    // ==================== 基础 String 操作 ====================

    @Override
    public void set(String key, String value) {
        stringCache.put(key, value);
    }

    @Override
    public void set(String key, String value, long seconds) {
        stringCache.put(key, value);
        log.debug("Local cache ignores per-entry TTL, key: {}", key);
    }

    @Override
    public String get(String key) {
        return stringCache.getIfPresent(key);
    }

    @Override
    public void delete(String key) {
        stringCache.invalidate(key);
    }

    @Override
    public boolean hasKey(String key) {
        return stringCache.getIfPresent(key) != null;
    }

    @Override
    public void expire(String key, long seconds) {
        log.warn("Local cache does not support dynamic expire for key: {}", key);
    }

    @Override
    public long getExpire(String key) {
        return -2;
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        log.warn("Local cache does not support dynamic expire for key: {}", key);
    }

    // ==================== 原子自增/自减 ====================

    @Override
    public Long increment(String key, long delta) {
        return Long.valueOf(stringCache.asMap().compute(key, (k, v) -> {
            long current = 0;
            if (v != null) {
                try {
                    current = Long.parseLong(v);
                } catch (NumberFormatException e) {
                    log.warn("Value for key {} is not a number, reset to 0", k);
                }
            }
            return String.valueOf(current + delta);
        }));
    }

    @Override
    public Long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    // ==================== Hash 操作 ====================

    @Override
    public Map<Object, Object> hashGetAll(String key) {
        Map<Object, Object> map = hashCache.getIfPresent(key);
        return map != null ? map : new ConcurrentHashMap<>();
    }

    @Override
    public void hashPutAll(String key, Map<Object, Object> map) {
        // 防止传入 null Map
        if (map == null || map.isEmpty()) {
            log.warn("hashPutAll called with null or empty map for key: {}", key);
            return;
        }
        
        // 原子性地获取或创建 Map，然后合并数据
        Map<Object, Object> existing = hashCache.get(key, k -> new ConcurrentHashMap<>());
        
        // 过滤掉 null 键值对，ConcurrentHashMap 不允许 null key/value
        map.forEach((k, v) -> {
            if (k != null && v != null) {
                existing.put(k, v);
            } else {
                log.warn("Skipping null key or value in hashPutAll for key: {}, key={}, value={}", key, k, v);
            }
        });
    }

    @Override
    public Object hashGet(String key, String field) {
        Map<Object, Object> map = hashCache.getIfPresent(key);
        return map != null ? map.get(field) : null;
    }

    @Override
    public void hashPut(String key, String field, String value) {
        // 防止 null 键值，ConcurrentHashMap 不允许 null key/value
        if (field == null || value == null) {
            log.warn("hashPut called with null field or value for key: {}, field={}, value={}", key, field, value);
            return;
        }
        
        // 原子性地获取或创建 Map，然后 put
        Map<Object, Object> map = hashCache.get(key, k -> new ConcurrentHashMap<>());
        map.put(field, value);
    }

    @Override
    public Boolean hashHasKey(String key, String field) {
        Map<Object, Object> map = hashCache.getIfPresent(key);
        return map != null && map.containsKey(field);
    }

    @Override
    public Long hashDelete(String key, String... fields) {
        Map<Object, Object> map = hashCache.getIfPresent(key);
        if (map == null) {
            return 0L;
        }
        long count = 0;
        for (String field : fields) {
            if (map.remove(field) != null) {
                count++;
            }
        }
        // 如果 Map 为空，可选择删除缓存 key，但需注意并发问题，这里不做自动删除
        return count;
    }

    // ==================== Set 操作 ====================

    @Override
    public Long setAdd(String key, String... values) {
        // 原子性地获取或创建 Set，然后添加元素
        Set<String> set = setCache.get(key, k -> ConcurrentHashMap.newKeySet());
        for (String val : values) {
            set.add(val);
        }
        return (long) values.length;
    }

    @Override
    public Set<String> setMembers(String key) {
        Set<String> set = setCache.getIfPresent(key);
        return set != null ? set : Set.of();
    }

    @Override
    public Boolean setIsMember(String key, String value) {
        Set<String> set = setCache.getIfPresent(key);
        return set != null && set.contains(value);
    }

    @Override
    public Long setRemove(String key, String... values) {
        Set<String> set = setCache.getIfPresent(key);
        if (set == null) {
            return 0L;
        }
        long count = 0;
        for (String val : values) {
            if (set.remove(val)) {
                count++;
            }
        }
        return count;
    }
}
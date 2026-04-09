package com.simple.common.auth.client.common.manager.cache;

import com.simple.common.cache.common.factory.LocalCacheFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存管理器实现
 *
 * @author Admin
 */
public class LocalCacheManager implements CacheManager {

    private final LocalCacheFactory localCacheFactory;

    /**
     * Hash缓存存储
     */
    private final ConcurrentHashMap<String, Map<Object, Object>> hashCache = new ConcurrentHashMap<>();

    /**
     * Set缓存存储
     */
    private final ConcurrentHashMap<String, Set<String>> setCache = new ConcurrentHashMap<>();

    public LocalCacheManager(LocalCacheFactory localCacheFactory) {
        this.localCacheFactory = localCacheFactory;
    }

    @Override
    public void set(String key, String value) {
        localCacheFactory.put(key, value);
    }

    @Override
    public void set(String key, String value, long seconds) {
        localCacheFactory.put(key, value, seconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return localCacheFactory.get(key);
    }

    @Override
    public void delete(String key) {
        localCacheFactory.remove(key);
    }

    @Override
    public boolean hasKey(String key) {
        return localCacheFactory.containsKey(key);
    }

    @Override
    public void expire(String key, long seconds) {
        localCacheFactory.expire(key, seconds, TimeUnit.SECONDS);
    }

    @Override
    public long getExpire(String key) {
        return localCacheFactory.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public Long increment(String key, long delta) {
        String value = localCacheFactory.get(key);
        long newValue = (value == null ? 0 : Long.parseLong(value)) + delta;
        localCacheFactory.put(key, String.valueOf(newValue));
        return newValue;
    }

    @Override
    public Map<Object, Object> hashGetAll(String key) {
        return hashCache.getOrDefault(key, new HashMap<>());
    }

    @Override
    public void hashPutAll(String key, Map<Object, Object> map) {
        hashCache.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).putAll(map);
    }

    @Override
    public Object hashGet(String key, String field) {
        Map<Object, Object> map = hashCache.get(key);
        return map != null ? map.get(field) : null;
    }

    @Override
    public void hashPut(String key, String field, String value) {
        hashCache.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(field, value);
    }

    @Override
    public Boolean hashHasKey(String key, String field) {
        Map<Object, Object> map = hashCache.get(key);
        return map != null && map.containsKey(field);
    }

    @Override
    public Long hashDelete(String key, String... fields) {
        Map<Object, Object> map = hashCache.get(key);
        if (map == null) {
            return 0L;
        }
        long count = 0;
        for (String field : fields) {
            if (map.remove(field) != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Long setAdd(String key, String... values) {
        Set<String> set = setCache.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        long count = 0;
        for (String value : values) {
            if (set.add(value)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Set<String> setMembers(String key) {
        return setCache.getOrDefault(key, new HashSet<>());
    }

    @Override
    public Boolean setIsMember(String key, String value) {
        Set<String> set = setCache.get(key);
        return set != null && set.contains(value);
    }

    @Override
    public Long setRemove(String key, String... values) {
        Set<String> set = setCache.get(key);
        if (set == null) {
            return 0L;
        }
        long count = 0;
        for (String value : values) {
            if (set.remove(value)) {
                count++;
            }
        }
        return count;
    }
}
package com.simple.common.auth.server.common.manager.cache;

import com.simple.common.cache.common.factory.LocalCacheFactory;

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
     * Hash缓存，用于模拟Redis的Hash结构
     */
    private final Map<String, Map<Object, Object>> hashCache = new ConcurrentHashMap<>();

    /**
     * Set缓存，用于模拟Redis的Set结构
     */
    private final Map<String, Set<String>> setCache = new ConcurrentHashMap<>();

    public LocalCacheManager(LocalCacheFactory localCacheFactory) {
        this.localCacheFactory = localCacheFactory;
    }

    @Override
    public void set(String key, String value) {
        localCacheFactory.put(key, value);
    }

    @Override
    public void set(String key, String value, long seconds) {
        localCacheFactory.put(key, value, seconds);
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
        return localCacheFactory.get(key) != null;
    }

    @Override
    public void expire(String key, long seconds) {
        String value = localCacheFactory.get(key);
        if (value != null) {
            localCacheFactory.put(key, value, seconds);
        }
    }

    @Override
    public Long increment(String key) {
        return increment(key, 1);
    }

    @Override
    public Long increment(String key, long delta) {
        String value = localCacheFactory.get(key);
        long result = (value == null ? 0 : Long.parseLong(value)) + delta;
        localCacheFactory.put(key, String.valueOf(result));
        return result;
    }

    @Override
    public Long decrement(String key) {
        return decrement(key, 1);
    }

    @Override
    public Long decrement(String key, long delta) {
        String value = localCacheFactory.get(key);
        long result = (value == null ? 0 : Long.parseLong(value)) - delta;
        localCacheFactory.put(key, String.valueOf(result));
        return result;
    }

    @Override
    public long getExpire(String key) {
        return localCacheFactory.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        localCacheFactory.expire(key, timeout, unit);
    }

    @Override
    public Map<Object, Object> hashGetAll(String key) {
        return hashCache.getOrDefault(key, new ConcurrentHashMap<>());
    }

    @Override
    public void hashPutAll(String key, Map<Object, Object> map) {
        hashCache.put(key, new ConcurrentHashMap<>(map));
    }

    @Override
    public Object hashGet(String key, String field) {
        Map<Object, Object> hash = hashCache.get(key);
        return hash != null ? hash.get(field) : null;
    }

    @Override
    public void hashPut(String key, String field, String value) {
        hashCache.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(field, value);
    }

    @Override
    public Boolean hashHasKey(String key, String field) {
        Map<Object, Object> hash = hashCache.get(key);
        return hash != null && hash.containsKey(field);
    }

    @Override
    public Long hashDelete(String key, String... fields) {
        Map<Object, Object> hash = hashCache.get(key);
        if (hash == null) {
            return 0L;
        }
        long count = 0;
        for (String field : fields) {
            if (hash.remove(field) != null) {
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
        return setCache.getOrDefault(key, ConcurrentHashMap.newKeySet());
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
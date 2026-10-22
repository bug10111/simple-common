package com.simple.common.auth.client.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.simple.common.auth.client.common.manager.cache.CacheManager;
import com.simple.common.cache.common.factory.LocalCacheFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
        if (stringCache.getIfPresent(key) == null) {
            return -2;
        }
        // Caffeine 不暴露单条记录剩余 TTL，返回 -1 表示存在但无法获取剩余过期时间
        return -1;
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
                    // 损坏的计数器不得静默清零：数据丢失且与 Redis 实现行为不一致，直接失败暴露问题
                    throw new IllegalStateException("缓存中的值不是合法数字，无法执行自增，key=" + k + ", value=" + v, e);
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
        // 返回快照副本，避免调用方绕过 hashPut/hashDelete 直接改写缓存内部状态
        Map<Object, Object> map = hashCache.getIfPresent(key);
        return map != null ? new ConcurrentHashMap<>(map) : new ConcurrentHashMap<>();
    }

    @Override
    public void hashPutAll(String key, Map<Object, Object> map) {
        // 防止传入 null Map
        if (map == null || map.isEmpty()) {
            log.warn("hashPutAll called with null or empty map for key: {}", key);
            return;
        }

        // 在缓存的 per-key 锁内完成「获取或创建 + 合并」，避免与淘汰失效竞态时写入孤儿 Map
        hashCache.asMap().compute(key, (k, existing) -> {
            Map<Object, Object> target = existing != null ? existing : new ConcurrentHashMap<>();
            // 过滤掉 null 键值对，ConcurrentHashMap 不允许 null key/value
            map.forEach((mk, mv) -> {
                if (mk != null && mv != null) {
                    target.put(mk, mv);
                } else {
                    log.warn("Skipping null key or value in hashPutAll for key: {}, key={}, value={}", key, mk, mv);
                }
            });
            return target;
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
        // 与 RedisCacheManager 保持一致的空入参防御（SADD 不允许空成员列表）
        if (values == null || values.length == 0) {
            return 0L;
        }
        long count = 0;
        Set<String> set = setCache.get(key, k -> ConcurrentHashMap.newKeySet());
        for (String value : values) {
            // null 成员会被 ConcurrentHashMap.newKeySet() 拒绝（NPE），跳过而非崩溃
            if (value != null && set.add(value)) {
                count++;
            }
        }
        // 契约：返回实际新增的元素数量（已存在的元素不计入）
        return count;
    }

    @Override
    public Set<String> setMembers(String key) {
        // 返回快照副本，避免调用方直接改写缓存内部集合
        Set<String> set = setCache.getIfPresent(key);
        return set != null ? new HashSet<>(set) : Set.of();
    }

    @Override
    public boolean setIfAbsent(String key, String value, long seconds) {
        if (key == null || value == null) {
            return false;
        }
        // 原子写入字符串缓存；TTL 受缓存实例默认过期策略约束（见类注释），Local 实现为尽力而为
        return stringCache.asMap().putIfAbsent(key, value) == null;
    }

    @Override
    public String getAndDelete(String key) {
        // compute 返回 null 即移除条目，检查+删除在同一 per-key 锁内原子完成
        AtomicReference<String> ref = new AtomicReference<>();
        stringCache.asMap().computeIfPresent(key, (k, v) -> {
            ref.set(v);
            return null;
        });
        return ref.get();
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
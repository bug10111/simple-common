package com.simple.common.auth.client.manager.cache;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存管理器实现。
 *
 * @author qty
 */
public class RedisCacheManager implements CacheManager {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    @Override
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean hasKey(String key) {
        Boolean hasKey = redisTemplate.hasKey(key);
        return hasKey != null && hasKey;
    }

    @Override
    public void expire(String key, long seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    @Override
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -2;
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    @Override
    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public void hashPutAll(String key, Map<Object, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        
        // 将所有 key 和 value 转换为 String 类型，以适配 StringRedisSerializer
        Map<String, String> stringMap = new java.util.HashMap<>(map.size());
        map.forEach((k, v) -> {
            if (k != null && v != null) {
                String valueStr;
                // 对于集合类型，使用 JSON 序列化，保持数据结构
                if (v instanceof java.util.Collection || v instanceof java.util.Map) {
                    valueStr = com.simple.common.core.utils.JsonUtils.toJsonStr(v);
                } else {
                    valueStr = v.toString();
                }
                stringMap.put(k.toString(), valueStr);
            }
        });
        
        redisTemplate.opsForHash().putAll(key, stringMap);
    }

    @Override
    public Object hashGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    @Override
    public void hashPut(String key, String field, String value) {
        if (field == null || value == null) {
            return;
        }
        
        // 确保 value 为 String 类型，以适配 StringRedisSerializer
        redisTemplate.opsForHash().put(key, field, value);
    }

    @Override
    public Boolean hashHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    @Override
    public Long hashDelete(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    @Override
    public Long setAdd(String key, String... values) {
        // Redis SADD 不允许空成员列表，调用方可能传入空数组，需防御
        if (values == null || values.length == 0) {
            return 0L;
        }
        return redisTemplate.opsForSet().add(key, values);
    }

    @Override
    public Set<String> setMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    @Override
    public Boolean setIsMember(String key, String value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    @Override
    public Long setRemove(String key, String... values) {
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }
}
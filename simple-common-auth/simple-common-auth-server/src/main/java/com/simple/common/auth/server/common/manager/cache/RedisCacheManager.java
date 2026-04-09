package com.simple.common.auth.server.common.manager.cache;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存管理器实现
 *
 * @author Admin
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
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    @Override
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    @Override
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    @Override
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        redisTemplate.expire(key, timeout, unit);
    }

    @Override
    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    @Override
    public void hashPutAll(String key, Map<Object, Object> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    @Override
    public Object hashGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    @Override
    public void hashPut(String key, String field, String value) {
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
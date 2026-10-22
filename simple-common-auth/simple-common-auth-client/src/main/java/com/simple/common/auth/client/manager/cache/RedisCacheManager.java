package com.simple.common.auth.client.manager.cache;

import com.simple.common.auth.client.common.manager.cache.CacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存管理器实现。
 * <p>
 * Hash 值契约：写入时 Collection/Map 类型值序列化为 JSON 字符串，其余转 String；
 * 读取返回 Redis 原始字符串，调用方需按「对象/JSON 字符串」双形态处理（与 Local 实现返回原始对象的差异见 CacheManager 契约）。
 * </p>
 *
 * @author qty
 */
@Slf4j
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
    public boolean setIfAbsent(String key, String value, long seconds) {
        // SET NX EX：检查与写入原子完成（防重放 nonce 等一次性语义依赖此原子性）
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofSeconds(seconds));
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public String getAndDelete(String key) {
        // GETDEL：读取与删除原子完成（一次性 Token 消费），老版本 spring-data-redis 不支持时抛异常，此处显式声明
        return redisTemplate.opsForValue().getAndDelete(key);
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
            } else {
                // null 键值对静默丢弃会让调用方误以为写入成功，至少告警暴露数据缺失
                log.warn("hashPutAll skipped null key or value for key: {}, field={}, value={}", key, k, v);
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
        // 与 setAdd 保持一致的空入参防御：HDEL 无字段参数会被 Redis 拒绝
        if (fields == null || fields.length == 0) {
            return 0L;
        }
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
        // 与 setAdd 保持一致的空入参防御：SREM 无成员参数会被 Redis 拒绝
        if (values == null || values.length == 0) {
            return 0L;
        }
        return redisTemplate.opsForSet().remove(key, (Object[]) values);
    }
}
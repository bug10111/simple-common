package com.simple.common.auth.client.common.manager.cache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 统一缓存管理器接口。
 *
 * @author qty
 */
public interface CacheManager {

    /**
     * 保存缓存（无过期时间，使用默认策略）。
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void set(String key, String value);

    /**
     * 保存缓存并设置过期时间。
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param seconds 过期时间（秒）
     */
    void set(String key, String value, long seconds);

    /**
     * 原子性 set-if-absent：仅当 key 不存在时写入并设置过期时间。
     * <p>
     * 内置实现（Redis: SET NX EX；Local: 并发 Map putIfAbsent）保证「检查+写入」的原子性，
     * 可用于防重放 nonce 等一次性语义场景。默认实现为 get+set 兜底（非原子），
     * 仅供第三方自定义实现过渡使用。
     * </p>
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param seconds 过期时间（秒）
     * @return true 表示写入成功（key 原不存在）；false 表示 key 已存在，未写入
     */
    default boolean setIfAbsent(String key, String value, long seconds) {
        if (hasKey(key)) {
            return false;
        }
        set(key, value, seconds);
        return true;
    }

    /**
     * 原子性读取并删除（Redis GETDEL 语义），用于一次性 Token 的消费场景。
     * <p>
     * 默认实现为 get+delete 兜底（非原子），内置实现均为原子操作。
     * </p>
     *
     * @param key 缓存键
     * @return 删除前的缓存值，不存在返回 null
     */
    default String getAndDelete(String key) {
        String value = get(key);
        if (value != null) {
            delete(key);
        }
        return value;
    }

    /**
     * 获取缓存。
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回 null
     */
    String get(String key);

    /**
     * 删除缓存。
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断缓存是否存在。
     *
     * @param key 缓存键
     * @return true 存在，false 不存在
     */
    boolean hasKey(String key);

    /**
     * 设置过期时间（秒）。
     *
     * @param key     缓存键
     * @param seconds 过期时间（秒）
     */
    void expire(String key, long seconds);

    /**
     * 获取剩余过期时间（秒）。
     * <p>
     * 契约：-2 表示 key 不存在；-1 表示存在但无法获取/无独立过期时间（如 Local 实现）。
     * 注意：Local 实现的 expire/set(key,value,seconds) 为尽力而为语义（受缓存实例默认过期策略约束），
     * 依赖精确 TTL 的场景（如登录错误计数）应使用 REDIS 缓存类型。
     * </p>
     *
     * @param key 缓存键
     * @return 剩余秒数，-2 表示不存在，-1 表示无法获取剩余 TTL
     */
    long getExpire(String key);

    /**
     * 设置过期时间（指定单位）。
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     */
    void expire(String key, long timeout, TimeUnit unit);

    /**
     * 原子自增（默认步长 1）。
     *
     * @param key 缓存键
     * @return 自增后的值；Redis 实现在管道/异常场景可能返回 null，调用方需判空
     */
    default Long increment(String key) {
        return increment(key, 1L);
    }

    /**
     * 原子自增（指定步长）。
     *
     * @param key   缓存键
     * @param delta 步长
     * @return 自增后的值；Redis 实现在管道/异常场景可能返回 null，调用方需判空
     */
    Long increment(String key, long delta);

    /**
     * 原子自减（默认步长 1）。
     *
     * @param key 缓存键
     * @return 自减后的值；Redis 实现在管道/异常场景可能返回 null，调用方需判空
     */
    default Long decrement(String key) {
        return decrement(key, 1L);
    }

    /**
     * 原子自减（指定步长）。
     *
     * @param key   缓存键
     * @param delta 步长
     * @return 自减后的值
     */
    Long decrement(String key, long delta);

    // ==================== Hash 操作 ====================

    /**
     * 获取 Hash 中的所有键值对。
     *
     * @param key Hash 键
     * @return 所有键值对，不存在返回空 Map
     */
    Map<Object, Object> hashGetAll(String key);

    /**
     * 批量设置 Hash 中的键值对。
     *
     * @param key Hash 键
     * @param map 待添加的键值对
     */
    void hashPutAll(String key, Map<Object, Object> map);

    /**
     * 获取 Hash 中指定字段的值。
     *
     * @param key   Hash 键
     * @param field 字段名
     * @return 字段值，不存在返回 null
     */
    Object hashGet(String key, String field);

    /**
     * 设置 Hash 中指定字段的值。
     *
     * @param key   Hash 键
     * @param field 字段名
     * @param value 字段值
     */
    void hashPut(String key, String field, String value);

    /**
     * 判断 Hash 中是否存在指定字段。
     *
     * @param key   Hash 键
     * @param field 字段名
     * @return true 存在，false 不存在
     */
    Boolean hashHasKey(String key, String field);

    /**
     * 删除 Hash 中的指定字段。
     *
     * @param key    Hash 键
     * @param fields 待删除的字段名数组
     * @return 实际删除的字段数量
     */
    Long hashDelete(String key, String... fields);

    // ==================== Set 操作 ====================

    /**
     * 向 Set 中添加元素。
     *
     * @param key    Set 键
     * @param values 待添加的元素数组
     * @return 实际添加的元素数量
     */
    Long setAdd(String key, String... values);

    /**
     * 获取 Set 中的所有元素。
     *
     * @param key Set 键
     * @return 元素集合，不存在返回空 Set
     */
    Set<String> setMembers(String key);

    /**
     * 判断元素是否在 Set 中。
     *
     * @param key   Set 键
     * @param value 待判断的元素
     * @return true 存在，false 不存在
     */
    Boolean setIsMember(String key, String value);

    /**
     * 移除 Set 中的指定元素。
     *
     * @param key    Set 键
     * @param values 待移除的元素数组
     * @return 实际移除的元素数量
     */
    Long setRemove(String key, String... values);
}
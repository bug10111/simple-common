package com.simple.common.auth.client.common.manager.cache;

import java.util.Map;
import java.util.Set;

/**
 * 统一缓存管理器接口
 *
 * @author Admin
 */
public interface CacheManager {

    /**
     * 保存缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void set(String key, String value);

    /**
     * 保存缓存并设置过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param seconds 过期时间（秒）
     */
    void set(String key, String value, long seconds);

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值
     */
    String get(String key);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean hasKey(String key);

    /**
     * 设置过期时间
     *
     * @param key     缓存键
     * @param seconds 过期时间（秒）
     */
    void expire(String key, long seconds);

    /**
     * 获取过期时间
     *
     * @param key 缓存键
     * @return 过期时间（秒），-2表示不存在，-1表示永不过期
     */
    long getExpire(String key);

    /**
     * 自增操作
     *
     * @param key 缓存键
     * @param delta 自增步长
     * @return 自增后的值
     */
    Long increment(String key, long delta);

    // ==================== Hash操作 ====================

    /**
     * 获取Hash中的所有键值对
     *
     * @param key Hash键
     * @return 所有键值对
     */
    Map<Object, Object> hashGetAll(String key);

    /**
     * 设置Hash中的多个键值对
     *
     * @param key Hash键
     * @param map 键值对
     */
    void hashPutAll(String key, Map<Object, Object> map);

    /**
     * 获取Hash中的指定字段值
     *
     * @param key   Hash键
     * @param field 字段
     * @return 字段值
     */
    Object hashGet(String key, String field);

    /**
     * 设置Hash中的指定字段值
     *
     * @param key   Hash键
     * @param field 字段
     * @param value 值
     */
    void hashPut(String key, String field, String value);

    /**
     * 判断Hash中是否存在指定字段
     *
     * @param key   Hash键
     * @param field 字段
     * @return 是否存在
     */
    Boolean hashHasKey(String key, String field);

    /**
     * 删除Hash中的指定字段
     *
     * @param key    Hash键
     * @param fields 字段
     * @return 删除的字段数量
     */
    Long hashDelete(String key, String... fields);

    // ==================== Set操作 ====================

    /**
     * 向Set中添加元素
     *
     * @param key    Set键
     * @param values 元素值
     * @return 添加的元素数量
     */
    Long setAdd(String key, String... values);

    /**
     * 获取Set中的所有元素
     *
     * @param key Set键
     * @return 所有元素
     */
    Set<String> setMembers(String key);

    /**
     * 判断Set中是否存在指定元素
     *
     * @param key   Set键
     * @param value 元素值
     * @return 是否存在
     */
    Boolean setIsMember(String key, String value);

    /**
     * 移除Set中的元素
     *
     * @param key    Set键
     * @param values 元素值
     * @return 移除的元素数量
     */
    Long setRemove(String key, String... values);
}
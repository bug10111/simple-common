package com.simple.common.eventbus.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件过程中存值操作类
 *
 * @author qty
 */
public class EventThreadLocalUtils {

    private static final ThreadLocal<ConcurrentHashMap<String, Object>> EVENT_BUS = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /**
     * 存值
     *
     * @param key   key
     * @param value 值
     */
    public static void put(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("key不能为空");
        }
        EVENT_BUS.get().put(key, value);
    }

    /**
     * 取值
     *
     * @param key key
     * @param <T> 返回值类型
     * @return 值，可能为null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        if (key == null) {
            return null;
        }
        return (T) EVENT_BUS.get().get(key);
    }

    /**
     * 移除指定值
     *
     * @param key 要移除的key
     */
    public static void remove(String key) {
        if (key == null) {
            return;
        }
        EVENT_BUS.get().remove(key);
    }

    /**
     * 清除所有值
     */
    public static void clear() {
        ConcurrentHashMap<String, Object> map = EVENT_BUS.get();
        if (map != null) {
            map.clear();
        }
        EVENT_BUS.remove();
    }
}
package com.simple.common.eventbus.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA
 * Description: 事件过程中存值操作类
 *
 * @author 兄台丶请冷静
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
        EVENT_BUS.get().put(key, value);
    }

    /**
     * 移除指定值
     *
     * @param key 要移除的key
     */
    public static void remove(String key) {
        EVENT_BUS.get().remove(key);
    }

    /**
     * 清除所有值
     */
    public static void clear() {
        EVENT_BUS.remove();
    }

}

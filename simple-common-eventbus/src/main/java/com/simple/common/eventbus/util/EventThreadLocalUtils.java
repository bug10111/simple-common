package com.simple.common.eventbus.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 事件过程中存值操作类
 * <p>线程内事件上下文：数据天然线程封闭，使用 HashMap 即可（无并发收益，
 * 且 ConcurrentHashMap 拒绝 null 值；配合 put 的显式校验，HashMap 语义更宽松且开销更低）。</p>
 *
 * @author qty
 */
public class EventThreadLocalUtils {

    private static final ThreadLocal<Map<String, Object>> EVENT_CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private EventThreadLocalUtils() {
    }

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
        // 显式拒绝 null 值：HashMap 虽允许存 null，但「存了 null」与「未存」在 get 上不可区分，
        // 且与 put(key) 的严格校验语义不一致，统一给出可读错误而不是依赖容器行为
        if (value == null) {
            throw new IllegalArgumentException("value不能为空");
        }
        EVENT_CONTEXT.get().put(key, value);
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
        return (T) EVENT_CONTEXT.get().get(key);
    }

    /**
     * 类型安全取值
     * <p>key 不存在返回 null；存在但类型不符时立即抛出带 key 与两侧类型信息的异常，
     * 避免 unchecked 强转把 ClassCastException 推迟到无关调用点难以定位</p>
     *
     * @param key  key
     * @param type 期望值类型
     * @param <T>  返回值类型
     * @return 值，可能为null
     */
    public static <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalStateException("事件上下文取值类型不匹配, key: " + key
                    + ", 期望: " + type.getName() + ", 实际: " + value.getClass().getName());
        }
        return type.cast(value);
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
        EVENT_CONTEXT.get().remove(key);
    }

    /**
     * 清除当前线程全部事件上下文
     * <p>直接移除整个 ThreadLocal：惰性初始化的 map 随之丢弃，
     * 无需为「清空」先 get() 出一个转瞬即弃的空 map</p>
     */
    public static void clear() {
        EVENT_CONTEXT.remove();
    }
}

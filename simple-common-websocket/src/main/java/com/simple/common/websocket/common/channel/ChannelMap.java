package com.simple.common.websocket.common.channel;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Created with IntelliJ IDEA
 * Description: 通道存储map，key-key(多)-values(多)
 *
 * @author qty
 */
@Slf4j
public class ChannelMap<K, V, T> {

    //键
    private final Map<K, Set<V>> keyMap = new HashMap<>();

    //值
    private final Map<String, T> valueMap = new HashMap<>();

    synchronized public void add(K key, V key2, T value) {
        String keyStr = getKey(key, key2);
        valueMap.put(keyStr, value);
        Set<V> vs = keyMap.computeIfAbsent(key, k -> new HashSet<>());
        vs.add(key2);
    }

    /**
     * 根据key获取所有
     */
    public List<T> get(K key) {
        List<T> list = new ArrayList<>();
        Set<V> vs = keyMap.get(key);
        if (vs != null) {
            for (V v : vs) {
                list.add(get(key, v));
            }
            return list;
        } else {
            return null;
        }
    }

    public T get(K key, V key2) {
        return valueMap.get(getKey(key, key2));
    }

    public void del(K key, V key2) {
        String key1 = getKey(key, key2);
        valueMap.remove(key1);
        Set<V> vs = keyMap.get(key);
        if (vs != null && !vs.isEmpty()) {
            vs.remove(key2);
        }
        //TODO 键为空可删除
    }

    public void del(K key) {
        Set<V> vs = keyMap.get(key);
        if (vs != null) {
            for (V v : vs) {
                valueMap.remove(getKey(key, v));
            }
            vs.clear();
        }
    }

    private String getKey(K key, V key2) {
        if (key != null && key2 != null) {
            return key + "&&" + key2;
        } else {
            return "";
        }
    }
}

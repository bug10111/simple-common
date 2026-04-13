package com.simple.common.websocket.common.channel;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 通道存储Map，支持key-key(多)-value(多)的映射关系
 * <p>
 * 线程安全实现，使用读写锁保证复合操作的原子性
 *
 * @author qty
 */
@Slf4j
public class ChannelMap<K, V, T> {

    /**
     * 主键到子键集合的映射
     */
    private final Map<K, Set<V>> keyMap = new ConcurrentHashMap<>();

    /**
     * 复合键到值的映射
     */
    private final Map<String, T> valueMap = new ConcurrentHashMap<>();

    /**
     * 读写锁，用于保护复合操作
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 添加通道
     *
     * @param key   主键
     * @param key2  子键
     * @param value 值
     */
    public void add(K key, V key2, T value) {
        lock.writeLock().lock();
        try {
            String keyStr = getKey(key, key2);
            valueMap.put(keyStr, value);
            keyMap.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(key2);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 根据主键获取所有值
     *
     * @param key 主键
     * @return 值列表，如果不存在返回null
     */
    public List<T> get(K key) {
        lock.readLock().lock();
        try {
            Set<V> vs = keyMap.get(key);
            if (vs == null || vs.isEmpty()) {
                return null;
            }
            List<T> list = new ArrayList<>(vs.size());
            for (V v : vs) {
                T value = valueMap.get(getKey(key, v));
                if (value != null) {
                    list.add(value);
                }
            }
            return list.isEmpty() ? null : list;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 根据复合键获取值
     *
     * @param key  主键
     * @param key2 子键
     * @return 值
     */
    public T get(K key, V key2) {
        return valueMap.get(getKey(key, key2));
    }

    /**
     * 删除指定通道
     *
     * @param key  主键
     * @param key2 子键
     */
    public void del(K key, V key2) {
        lock.writeLock().lock();
        try {
            String keyStr = getKey(key, key2);
            valueMap.remove(keyStr);
            Set<V> vs = keyMap.get(key);
            if (vs != null) {
                vs.remove(key2);
                if (vs.isEmpty()) {
                    keyMap.remove(key);
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 删除主键下的所有通道
     *
     * @param key 主键
     */
    public void del(K key) {
        lock.writeLock().lock();
        try {
            Set<V> vs = keyMap.remove(key);
            if (vs != null) {
                for (V v : vs) {
                    valueMap.remove(getKey(key, v));
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 清空所有通道
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            valueMap.clear();
            keyMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取通道总数
     *
     * @return 通道数量
     */
    public int size() {
        return valueMap.size();
    }

    /**
     * 获取主键数量
     *
     * @return 主键数量
     */
    public int keySize() {
        return keyMap.size();
    }

    /**
     * 检查是否包含指定的复合键
     *
     * @param key  主键
     * @param key2 子键
     * @return 是否存在
     */
    public boolean contains(K key, V key2) {
        return valueMap.containsKey(getKey(key, key2));
    }

    /**
     * 获取所有条目（用于遍历清理）
     *
     * @return 条目列表
     */
    public List<Map.Entry<String, T>> entries() {
        return new ArrayList<>(valueMap.entrySet());
    }

    /**
     * 生成复合键字符串
     */
    private String getKey(K key, V key2) {
        if (key != null && key2 != null) {
            return key + "&&" + key2;
        }
        return "";
    }

    /**
     * 解析复合键获取主键
     *
     * @param keyStr 复合键字符串
     * @return 主键
     */
    public K parseKey1(String keyStr) {
        if (keyStr == null || !keyStr.contains("&&")) {
            return null;
        }
        @SuppressWarnings("unchecked")
        K key = (K) keyStr.substring(0, keyStr.indexOf("&&"));
        return key;
    }

    /**
     * 解析复合键获取子键
     *
     * @param keyStr 复合键字符串
     * @return 子键
     */
    public V parseKey2(String keyStr) {
        if (keyStr == null || !keyStr.contains("&&")) {
            return null;
        }
        @SuppressWarnings("unchecked")
        V key2 = (V) keyStr.substring(keyStr.indexOf("&&") + 2);
        return key2;
    }
}
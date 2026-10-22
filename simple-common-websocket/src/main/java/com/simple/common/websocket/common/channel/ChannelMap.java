package com.simple.common.websocket.common.channel;

import lombok.extern.slf4j.Slf4j;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 通道存储Map，支持key-key2(多)-value(多)的映射关系。
 * <p>
 * 线程安全实现：主键到子键采用嵌套 Map 结构（{@code Map<K, Map<V, T>>}），
 * 复合操作经读写锁保证原子性；键与值均不允许 null（null 复合键无法区分归属，
 * 静默合成占位键会让不同键的通道互相覆盖）。
 * </p>
 *
 * @author qty
 */
@Slf4j
public class ChannelMap<K, V, T> {

    /**
     * 主键到子键值映射的嵌套结构
     */
    private final Map<K, Map<V, T>> keyMap = new ConcurrentHashMap<>();

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
        put(key, key2, value);
    }

    /**
     * 原子放入并返回该复合键的旧值。
     * <p>
     * 校验在触碰任何 Map 之前完成，失败不会留下半写状态；
     * 返回旧值供调用方在同一把写锁内完成"替换并关闭旧通道"等复合逻辑，
     * 避免 check-then-act 竞态。
     * </p>
     *
     * @param key   主键（不可为 null）
     * @param key2  子键（不可为 null）
     * @param value 值（不可为 null）
     * @return 该复合键此前映射的值；不存在时返回 null
     */
    public T put(K key, V key2, T value) {
        validate(key, key2, value);
        lock.writeLock().lock();
        try {
            return keyMap.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(key2, value);
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
        if (key == null) {
            return null;
        }
        lock.readLock().lock();
        try {
            Map<V, T> subMap = keyMap.get(key);
            if (subMap == null || subMap.isEmpty()) {
                return null;
            }
            return new ArrayList<>(subMap.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 根据复合键获取值
     *
     * @param key  主键
     * @param key2 子键
     * @return 值，不存在时返回 null
     */
    public T get(K key, V key2) {
        if (key == null || key2 == null) {
            return null;
        }
        Map<V, T> subMap = keyMap.get(key);
        return subMap == null ? null : subMap.get(key2);
    }

    /**
     * 删除指定通道
     *
     * @param key  主键
     * @param key2 子键
     */
    public void del(K key, V key2) {
        if (key == null || key2 == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            Map<V, T> subMap = keyMap.get(key);
            if (subMap != null) {
                subMap.remove(key2);
                if (subMap.isEmpty()) {
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
        if (key == null) {
            return;
        }
        lock.writeLock().lock();
        try {
            keyMap.remove(key);
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
        lock.readLock().lock();
        try {
            int total = 0;
            for (Map<V, T> subMap : keyMap.values()) {
                total += subMap.size();
            }
            return total;
        } finally {
            lock.readLock().unlock();
        }
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
        return get(key, key2) != null;
    }

    /**
     * 获取所有条目快照（用于遍历清理）。
     * <p>
     * 返回不可变条目副本：调用方无法通过 Entry 引用穿透写共享结构，
     * 避免绕过写锁破坏不变式；返回结构携带原始主键/子键，
     * 调用方无需再做字符串键反解。
     * </p>
     *
     * @return 主键 → (子键 → 值) 的条目快照列表
     */
    public List<Map.Entry<K, Map.Entry<V, T>>> entries() {
        lock.readLock().lock();
        try {
            List<Map.Entry<K, Map.Entry<V, T>>> result = new ArrayList<>();
            keyMap.forEach((k, subMap) -> subMap.forEach((v, t) ->
                    result.add(new AbstractMap.SimpleImmutableEntry<>(k,
                            new AbstractMap.SimpleImmutableEntry<>(v, t)))));
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 键值校验：任一为 null 直接拒绝，防止出现无归属的孤儿条目
     */
    private void validate(K key, V key2, T value) {
        if (key == null || key2 == null) {
            throw new IllegalArgumentException("ChannelMap 复合键的 key 与 key2 均不可为 null");
        }
        Objects.requireNonNull(value, "ChannelMap value 不可为 null");
    }
}

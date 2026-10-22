package com.simple.common.eventbus.common.function;

/**
 * 双参数处理函数，用于对 (key, value) 对执行操作。
 * <p>典型用法：事件管理器以 {@code EventHandlerFunction<EventHandlerKey, EventHandlerValue>}
 * 对每个注册键执行注册/注销动作，第一个参数为查找键，第二个参数为映射值。</p>
 *
 * @param <K> 第一个参数类型（键，如 EventHandlerKey）
 * @param <V> 第二个参数类型（值，如 EventHandlerValue）
 * @author qty
 */
@FunctionalInterface
public interface EventHandlerFunction<K, V> {

    /**
     * 处理单个 (key, value) 对。
     * <p>异常契约：本接口未声明受检异常；实现抛出的非受检异常会沿调用链向上传播
     * （例如注册场景中会中断剩余键的注册流程），实现方应自行决定是否在内部捕获。</p>
     *
     * @param key   第一个参数（键）
     * @param value 第二个参数（值）
     */
    void handler(K key, V value);

}

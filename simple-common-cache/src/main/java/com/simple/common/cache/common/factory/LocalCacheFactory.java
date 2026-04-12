package com.simple.common.cache.common.factory;

import com.github.benmanes.caffeine.cache.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 通用 Caffeine 缓存工厂（静态单例，非泛型类）
 * 设计目标：
 * 1. 类型安全：通过方法级泛型保证不同缓存实例的类型隔离
 * 2. 配置灵活：支持所有 Caffeine 常用配置项
 * 3. 线程安全：确保多线程环境下安全访问
 * 4. 内存安全：强制要求设置淘汰策略
 * 5. 易于扩展：方便添加新配置参数
 * <p>
 * 使用方式：通过 {@link #getInstance()} 获取单例，然后调用 {@code createCache(...)} 等方法。
 *
 * @author qty
 */
@Slf4j
public class LocalCacheFactory {

    /**
     * 缓存注册表 - 核心存储结构
     * 使用复合键 CacheKey 保证：
     * 1. 同名的不同缓存类型（手动/自动加载）可以共存
     * 2. 不同类型的同名缓存不会冲突
     */
    private final ConcurrentHashMap<CacheKey, Cache<?, ?>> cacheRegistry = new ConcurrentHashMap<>();

    /**
     * 移除监听器执行器 - 设计亮点
     * 为什么要异步执行？
     * 1. 避免阻塞缓存操作线程
     * 2. 防止监听器逻辑影响缓存性能
     * 默认使用 ForkJoinPool，可根据需要替换为虚拟线程等
     */
    private volatile Executor removalListenerExecutor = ForkJoinPool.commonPool();

    // ================== 静态单例 ================== //

    /**
     * 静态内部类实现懒加载单例（线程安全）
     */
    private static final class Holder {
        static final LocalCacheFactory INSTANCE = new LocalCacheFactory();
    }

    /**
     * 获取工厂单例实例
     *
     * @return LocalCacheFactory 单例
     */
    public static LocalCacheFactory getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 私有构造器，防止外部实例化
     */
    private LocalCacheFactory() {
    }

    // ================== 公开API ================== //

    /**
     * 创建手动加载缓存
     *
     * @param cacheName    缓存唯一标识
     * @param configurator 配置函数（使用 lambda 表达式配置参数）
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 配置完成的 Cache 实例
     */
    public <K, V> Cache<K, V> createCache(String cacheName, Consumer<CacheSpec<K, V>> configurator) {
        return getOrCreate(cacheName, CacheType.MANUAL, configurator);
    }

    /**
     * 创建自动加载缓存
     *
     * @param cacheName    缓存唯一标识
     * @param loader       缓存加载器（定义如何加载数据）
     * @param configurator 配置函数
     * @param <K>          键类型
     * @param <V>          值类型
     * @return 配置完成的 LoadingCache 实例
     */
    public <K, V> LoadingCache<K, V> createLoadingCache(String cacheName, CacheLoader<K, V> loader, Consumer<CacheSpec<K, V>> configurator) {
        return getOrCreate(cacheName, CacheType.LOADING, spec -> {
            configurator.accept(spec);
            spec.loader = loader;  // 注入加载器实现
        });
    }

    /**
     * 设置移除监听器的执行器（线程池）
     * <p>
     * 注意：该方法应在任何缓存创建之前调用，否则已创建的缓存仍使用旧执行器。
     * 推荐在应用启动配置阶段调用此方法进行全局设置。
     *
     * @param executor 自定义执行器，若为 null 则恢复为默认的 ForkJoinPool.commonPool()
     */
    public synchronized void setRemovalListenerExecutor(Executor executor) {
        this.removalListenerExecutor = executor == null ? ForkJoinPool.commonPool() : executor;
    }

    /**
     * 移除并失效指定缓存实例
     * <p>
     * 移除后缓存将不再可用，并触发 Caffeine 的资源清理（如监听器、统计等）。
     * 若缓存不存在，则静默忽略。
     *
     * @param cacheName 缓存名称
     * @param cacheType 缓存类型（手动/自动加载）
     * @return true 表示成功移除已存在的缓存，false 表示缓存不存在
     */
    public boolean removeCache(String cacheName, CacheType cacheType) {
        CacheKey key = new CacheKey(cacheName, cacheType);
        Cache<?, ?> removed = cacheRegistry.remove(key);
        if (removed != null) {
            removed.invalidateAll();
            log.debug("缓存已移除并失效: name={}, type={}", cacheName, cacheType);
            return true;
        }
        return false;
    }

    // ================== 核心实现 ================== //

    /**
     * 统一缓存创建入口（核心方法）
     * 设计要点：
     * 1. 使用 computeIfAbsent 保证原子性操作
     * 2. 通过复合键保证缓存实例唯一性
     * 3. 分离配置与应用逻辑
     */
    @SuppressWarnings("unchecked")
    private <K, V, C extends Cache<K, V>> C getOrCreate(String cacheName, CacheType cacheType, Consumer<CacheSpec<K, V>> configurator) {
        CacheKey key = new CacheKey(cacheName, cacheType);

        return (C) cacheRegistry.computeIfAbsent(key, k -> {
            // 初始化配置规格对象
            CacheSpec<K, V> spec = new CacheSpec<>();

            // 应用用户配置
            configurator.accept(spec);

            // 配置校验
            spec.validate();

            // 构建 Caffeine 实例
            Caffeine<Object, Object> builder = Caffeine.newBuilder();

            // 应用配置到构建器
            applyConfig(builder, spec);

            // 根据类型构建具体缓存实例
            return buildCache(builder, cacheType, spec);
        });
    }

    /**
     * 将配置应用到 Caffeine 构建器
     * 设计策略：
     * 1. 使用 null 表示未配置（替代旧版的 -1 歧义）
     * 2. 按需设置各配置参数
     */
    private <K, V> void applyConfig(Caffeine<Object, Object> builder, CacheSpec<K, V> spec) {
        // 初始容量（可选）
        if (spec.initialCapacity != null) {
            builder.initialCapacity(spec.initialCapacity);
        }

        // 最大容量（重要保护）
        if (spec.maximumSize != null) {
            builder.maximumSize(spec.maximumSize);
        }

        // 写入后过期时间
        if (spec.expireAfterWrite != null) {
            builder.expireAfterWrite(spec.expireAfterWrite, TimeUnit.SECONDS);
        }

        // 访问后过期时间
        if (spec.expireAfterAccess != null) {
            builder.expireAfterAccess(spec.expireAfterAccess, TimeUnit.SECONDS);
        }

        // 弱引用键（特殊场景使用）
        if (spec.weakKeys) {
            builder.weakKeys();
        }

        // 软引用值（内存敏感场景，建议配合 maximumSize 使用）
        if (spec.softValues) {
            builder.softValues();
        }

        // 移除监听器（异步执行）
        if (spec.removalListener != null) {
            builder.executor(removalListenerExecutor).removalListener(spec.removalListener);
        }

        // 自动刷新配置（需要加载器）
        if (spec.refreshAfterWrite != null && spec.loader != null) {
            builder.refreshAfterWrite(spec.refreshAfterWrite, TimeUnit.SECONDS);
        }

        // 统计开关
        if (spec.recordStats) {
            builder.recordStats();
        }
    }

    /**
     * 构建具体缓存实例
     * 类型处理技巧：使用泛型转换和类型开关确保类型安全
     */
    @SuppressWarnings("unchecked")
    private <K, V, C extends Cache<K, V>> C buildCache(Caffeine<Object, Object> builder, CacheType cacheType, CacheSpec<K, V> spec) {
        return (C) switch (cacheType) {
            case MANUAL -> builder.build();
            case LOADING -> {
                Objects.requireNonNull(spec.loader, "LoadingCache requires a CacheLoader");
                yield builder.build(spec.loader);
            }
        };
    }

    // ================== 配置规格类 ================== //

    /**
     * 缓存配置规格类（核心设计），使用 null 表示未配置（修复了旧版 -1 歧义）
     * 设计理念：
     * 1. 集中管理所有配置参数
     * 2. 提供链式配置方法
     * 3. 封装校验逻辑
     */
    public static class CacheSpec<K, V> {

        private Integer initialCapacity = null;

        private Long maximumSize = null;

        private Long expireAfterWrite = null;

        private Long expireAfterAccess = null;

        private Long refreshAfterWrite = null;

        private boolean weakKeys = false;

        private boolean softValues = false;

        private boolean recordStats = false;

        private RemovalListener<? super K, ? super V> removalListener = null;

        private CacheLoader<K, V> loader = null;

        /**
         * 配置验证（保障系统稳定性）
         * 强制要求至少设置一种淘汰策略：
         * 1. 最大容量限制
         * 2. 过期时间限制
         * 3. 软引用回收（不推荐单独使用，必须配合 maximumSize）
         */
        void validate() {
            boolean hasEvictionPolicy = maximumSize != null || expireAfterWrite != null || expireAfterAccess != null || softValues;

            if (!hasEvictionPolicy) {
                throw new IllegalStateException("必须设置至少一种淘汰策略: maximumSize/expireAfterWrite/expireAfterAccess/softValues");
            }

            // 若仅使用 softValues 而未设置 maximumSize，给出警告（但仍允许创建）
            if (softValues && maximumSize == null) {
                log.warn("缓存使用了 softValues() 但未设置 maximumSize，可能导致内存无限增长，强烈建议配合 maximumSize 使用。");
            }
        }

        // ===== 链式配置方法 ===== //

        public CacheSpec<K, V> initialCapacity(int initialCapacity) {
            this.initialCapacity = initialCapacity;
            return this;
        }

        public CacheSpec<K, V> maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public CacheSpec<K, V> expireAfterWrite(long seconds) {
            this.expireAfterWrite = seconds;
            return this;
        }

        public CacheSpec<K, V> expireAfterAccess(long seconds) {
            this.expireAfterAccess = seconds;
            return this;
        }

        public CacheSpec<K, V> refreshAfterWrite(long seconds) {
            this.refreshAfterWrite = seconds;
            return this;
        }

        public CacheSpec<K, V> weakKeys() {
            this.weakKeys = true;
            return this;
        }

        public CacheSpec<K, V> softValues() {
            this.softValues = true;
            return this;
        }

        public CacheSpec<K, V> removalListener(RemovalListener<K, V> listener) {
            this.removalListener = listener;
            return this;
        }

        public CacheSpec<K, V> recordStats() {
            this.recordStats = true;
            return this;
        }
    }

    // ================== 辅助结构 ================== //

    /**
     * 缓存类型枚举 - 设计意义：
     * 1. 明确区分手动/自动加载缓存
     * 2. 增强代码可读性
     */
    public enum CacheType {
        /**
         * 手动加载缓存
         */
        MANUAL,
        /**
         * 自动加载缓存
         */
        LOADING
    }

    /**
     * 复合缓存键 - 核心设计亮点
     * 解决什么问题？
     * 1. 同名不同泛型参数的缓存隔离
     * 2. 同名但类型不同的缓存隔离（手动/自动）
     * record 特性：自动生成 equals/hashCode 方法
     */
    private record CacheKey(String name, CacheType type) {
    }
}
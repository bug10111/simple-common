# simple-common-cache 模块文档

> 两级缓存模块：Caffeine 本地缓存 + Redis 分布式缓存，提供防缓存击穿 / 穿透 / 雪崩的完整能力。
>
> @author qty

---

## 1. 模块介绍

`simple-common-cache` 是 simple-common 框架的缓存基础设施模块，整合了 **Caffeine 本地缓存** 与 **Redis 分布式缓存** 两套能力，通过统一的 [`CacheUtils`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:27) 静态门面对外提供服务。

### 核心能力

| 能力 | 实现类 | 说明 |
|------|--------|------|
| 分布式缓存获取（防击穿） | [`CacheUtils.get()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:99) | Redis 缓存未命中时，通过 Redisson 分布式锁 + 双重检查机制保护数据库，防止缓存击穿 |
| 缓存穿透防护 | [`GetRedisFunction.getNullable()`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:51) | 数据库查询结果为 null 时，缓存空值占位对象，防止穿透 |
| 缓存雪崩防护 | [`CacheUtils.getCacheTime()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:187) | 在基础过期时间上追加随机时长，避免大量缓存同时失效 |
| 主动缓存失效 | [`CacheUtils.evict()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:145) | 数据更新时主动删除缓存 |
| 本地手动缓存 | [`CacheUtils.createLocalCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:48) | 创建 Caffeine `Cache<K,V>` 实例，手动 put/get |
| 本地自动加载缓存 | [`CacheUtils.createLocalLoadingCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:64) | 创建 Caffeine `LoadingCache<K,V>` 实例，自动加载 |
| 本地缓存移除 | [`CacheUtils.removeLocalCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:78) | 移除并失效指定本地缓存实例 |

### 两级缓存协作机制

```
┌──────────────────────────────────────────────────────────────────┐
│ 业务代码                                                          │
│    │                                                              │
│    ├─► CacheUtils.get()          ← 分布式缓存（Redis+DB，防击穿）│
│    │                                                              │
│    └─► CacheUtils.createLocalCache() ← 本地缓存（Caffeine，JVM） │
│                                                                   │
│  选择策略：                                                        │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ 单机数据 → 本地缓存（Caffeine，性能最优）                     ││
│  │ 共享数据 → 分布式缓存（CacheUtils.get，防击穿/穿透/雪崩）    ││
│  │ 热点数据 → 本地缓存 + 分布式缓存 两级联动                    ││
│  └─────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Maven 依赖

### 2.1 引入依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-cache</artifactId>
</dependency>
```

### 2.2 模块自身依赖

[`pom.xml`](simple-common-cache/pom.xml:14) 中声明了以下依赖：

| 依赖 | 说明 |
|------|------|
| `simple-common-core` | 框架核心基础（`R`、`JsonUtils`、`AssertUtils` 等） |
| `simple-common-redis` | Redis 服务，提供 [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43) 分布式锁 |
| `caffeine` | Caffeine 本地缓存引擎 |

### 2.3 自动装配

模块通过 Spring Boot 自动装配机制注册，配置文件位于：

[`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`](simple-common-cache/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1)

注册的自动配置类为 [`CacheConfig`](simple-common-cache/src/main/java/com/simple/common/cache/common/config/CacheConfig.java:15)，该类通过 `@ComponentScan(basePackages = {"com.simple.common.cache"})` 扫描模块内所有组件。

> **注意**：引入依赖后无需额外配置，Spring Boot 启动时自动加载。但分布式缓存功能依赖 `simple-common-redis` 提供的 [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43) Bean，需确保 Redis 环境可用。

---

## 3. 核心功能表格

### 3.1 CacheUtils 静态方法

| 方法 | 签名 | 功能 |
|------|------|------|
| [`get()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:99) | `static <T, R> T get(R request, String lockKey, GetRedisFunction<T, R> redisFunction, GetDBFunction<T, R> dbFunction)` | 分布式缓存获取，防击穿/穿透 |
| [`evict()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:145) | `static <R> void evict(R request, String cacheKey, Consumer<String> evictHandler)` | 主动删除分布式缓存 |
| [`getCacheTime()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:187) | `static int getCacheTime(int cacheTime, int appendRandomDuration)` | 计算带随机偏移的缓存过期时间，防雪崩 |
| [`createLocalCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:48) | `static <K, V> Cache<K, V> createLocalCache(String cacheName, Consumer<LocalCacheFactory.CacheSpec<K, V>> configurator)` | 创建本地手动加载缓存 |
| [`createLocalLoadingCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:64) | `static <K, V> LoadingCache<K, V> createLocalLoadingCache(String cacheName, CacheLoader<K, V> loader, Consumer<LocalCacheFactory.CacheSpec<K, V>> configurator)` | 创建本地自动加载缓存 |
| [`removeLocalCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:78) | `static boolean removeLocalCache(String cacheName, LocalCacheFactory.CacheType cacheType)` | 移除并失效本地缓存 |

### 3.2 函数式接口

| 接口 | 方法 | 说明 |
|------|------|------|
| [`GetRedisFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:12) | `T get(R request)` | 从 Redis 获取缓存数据 |
| [`GetRedisFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:26) | `void setHandler(T value)` | 将数据写入 Redis |
| [`GetRedisFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:35) | `default void set(T value)` | 判空后写入（null 则写占位对象） |
| [`GetRedisFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:51) | `T getNullable()` | 返回空值占位对象（必须非 null） |
| [`GetDBFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetDBFunction.java:34) | `T get(R request)` | 从数据库查询数据，不存在返回 null |

### 3.3 LocalCacheFactory 核心方法

| 方法 | 签名 | 功能 |
|------|------|------|
| [`getInstance()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:60) | `static LocalCacheFactory getInstance()` | 获取工厂单例 |
| [`createCache()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:81) | `<K, V> Cache<K, V> createCache(String cacheName, Consumer<CacheSpec<K, V>> configurator)` | 创建手动加载缓存 |
| [`createLoadingCache()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:95) | `<K, V> LoadingCache<K, V> createLoadingCache(String cacheName, CacheLoader<K, V> loader, Consumer<CacheSpec<K, V>> configurator)` | 创建自动加载缓存 |
| [`removeCache()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:124) | `boolean removeCache(String cacheName, CacheType cacheType)` | 移除并失效指定缓存 |
| [`setRemovalListenerExecutor()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:110) | `synchronized void setRemovalListenerExecutor(Executor executor)` | 设置移除监听器执行器 |

---

## 4. 配置说明

### 4.1 自动配置

本模块无需任何 YAML / properties 配置，引入 Maven 依赖后自动生效。

### 4.2 前置条件

| 条件 | 说明 |
|------|------|
| Redis 环境 | 分布式缓存功能（`get` / `evict`）依赖 `simple-common-redis` 提供的 [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43) Bean |
| Spring 上下文 | [`CacheUtils`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:27) 通过 `SpringUtil.getBean()` 懒加载获取 [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43)，需在 Spring 容器环境中使用 |

### 4.3 CacheSpec 配置项

本地缓存通过 [`CacheSpec`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:246) 配置参数，支持链式调用：

| 配置方法 | 参数类型 | 说明 |
|---------|---------|------|
| [`initialCapacity(int)`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:298) | `int` | 初始容量，避免频繁扩容 |
| [`maximumSize(long)`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:313) | `long` | 最大容量，LRU 淘汰策略 |
| [`expireAfterWrite(long)`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:326) | `long`（秒） | 写入后过期时间 |
| [`expireAfterAccess(long)`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:339) | `long`（秒） | 访问后过期时间 |
| [`refreshAfterWrite(long)`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:355) | `long`（秒） | 写入后异步刷新时间（仅 LoadingCache） |
| [`weakKeys()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:370) | 无 | 启用弱引用键 |
| [`softValues()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:384) | 无 | 启用软引用值 |
| [`removalListener(RemovalListener)`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:399) | `RemovalListener<K,V>` | 移除监听器（异步执行） |
| [`recordStats()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:413) | 无 | 开启统计信息收集 |

> **强制规则**：[`CacheSpec.validate()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:275) 要求至少设置一种淘汰策略（`maximumSize` / `expireAfterWrite` / `expireAfterAccess` / `softValues`），否则抛出 `IllegalStateException`。

---

## 5. 核心类与接口详细说明

### 5.1 CacheUtils

[`CacheUtils`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:27) 是 `final` 工具类，私有构造器禁止实例化，所有方法均为 `static`。

#### 5.1.1 分布式缓存获取 — `get()`

```java
public static <T, R> T get(R request, String lockKey,
    GetRedisFunction<T, R> redisFunction, GetDBFunction<T, R> dbFunction)
```

**执行流程**（防缓存击穿）：

```
CacheUtils.get(request, lockKey, redisFunction, dbFunction)
                        │
                        ▼
            ┌─────────────────────┐
            │ ① redisFunction.get │◄── 先查Redis缓存
            │   (request)         │
            └─────────┬───────────┘
                      │
                命中? │
                 是   │   否
            ┌─────────┴─────────┐
            ▼                   ▼
       ┌────────┐     ┌─────────────────────┐
       │返回缓存│     │ ② 获取分布式锁        │
       │数据    │     │ lockHaveValue(lockKey)│
       └────────┘     └─────────┬───────────┘
                                │
                                ▼
                      ┌─────────────────────┐
                      │ ③ 双重检查           │◄── 再次查Redis
                      │ redisFunction.get()  │    防止并发重复查DB
                      └─────────┬───────────┘
                                │
                          命中? │
                           是   │   否
                      ┌─────────┴─────────┐
                      ▼                   ▼
                 ┌────────┐    ┌─────────────────────┐
                 │返回缓存│    │ ④ dbFunction.get()  │
                 └────────┘    │ 查询数据库           │
                               └─────────┬───────────┘
                                         │
                                         ▼
                               ┌─────────────────────┐
                               │ ⑤ redisFunction.set()│
                               │ 写回Redis (含空值)   │
                               │ getCacheTime()防雪崩 │
                               └─────────┬───────────┘
                                         │
                                         ▼
                                    ┌────────┐
                                    │返回数据│
                                    └────────┘
```

**关键设计**：

| 步骤 | 防护目标 | 实现方式 |
|------|---------|---------|
| ① 第一次缓存检查 | 性能优化 | 命中直接返回，避免锁开销 |
| ② 分布式锁 | 缓存击穿 | [`RedissonLockService.lockHaveValue()`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43) 保证同一 key 只有一个线程查 DB |
| ③ 双重检查 | 缓存击穿 | 获取锁后再次查 Redis，防止并发重复查 DB |
| ④ 数据库查询 | 数据加载 | [`GetDBFunction.get()`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetDBFunction.java:53) 从数据库获取 |
| ⑤ 缓存回写 | 缓存穿透 | [`GetRedisFunction.set()`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:35) 判空后写入，null 则写占位对象 |

**RedissonLockService 懒加载**：

[`getRedissonLockService()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:162) 使用双重检查锁（DCL）模式懒加载 [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43) Bean：

```java
private static volatile RedissonLockService redissonLockService;

private static RedissonLockService getRedissonLockService() {
    if (redissonLockService == null) {
        synchronized (CacheUtils.class) {
            if (redissonLockService == null) {
                redissonLockService = SpringUtil.getBean(RedissonLockService.class);
            }
        }
    }
    return redissonLockService;
}
```

#### 5.1.2 主动缓存失效 — `evict()`

```java
public static <R> void evict(R request, String cacheKey, Consumer<String> evictHandler)
```

数据更新时调用，通过 `evictHandler` 回调执行实际的 Redis 删除操作。删除失败仅记录日志，不抛出异常。

#### 5.1.3 防雪崩时间计算 — `getCacheTime()`

```java
public static int getCacheTime(int cacheTime, int appendRandomDuration)
```

在基础缓存时间上追加 `[0, appendRandomDuration)` 的随机秒数，避免大量缓存同时过期导致雪崩。

#### 5.1.4 本地缓存门面方法

[`CacheUtils`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:27) 对 [`LocalCacheFactory`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:27) 单例做了静态门面封装，直接委托调用：

| 门面方法 | 委托目标 |
|---------|---------|
| [`createLocalCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:48) | [`LocalCacheFactory.createCache()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:81) |
| [`createLocalLoadingCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:64) | [`LocalCacheFactory.createLoadingCache()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:95) |
| [`removeLocalCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:78) | [`LocalCacheFactory.removeCache()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:124) |

---

### 5.2 LocalCacheFactory

[`LocalCacheFactory`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:27) 是 Caffeine 本地缓存的工厂类，采用**静态内部类懒加载单例**模式。

#### 5.2.1 核心数据结构

| 字段 | 类型 | 说明 |
|------|------|------|
| [`cacheRegistry`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:35) | `ConcurrentHashMap<CacheKey, Cache<?, ?>>` | 缓存注册表，存储所有已创建的缓存实例 |
| [`removalListenerExecutor`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:44) | `volatile Executor` | 移除监听器执行器，默认 `ForkJoinPool.commonPool()` |

#### 5.2.2 复合键设计 — CacheKey

```java
private record CacheKey(String name, CacheType type) {}
```

使用 Java `record` 自动生成 `equals` / `hashCode`，通过 `(name, type)` 复合键保证：
- 同名但不同类型（手动/自动加载）的缓存可以共存
- 不同类型的同名缓存不会冲突

#### 5.2.3 缓存类型枚举 — CacheType

```java
public enum CacheType {
    MANUAL,    // 手动加载缓存
    LOADING    // 自动加载缓存
}
```

#### 5.2.4 原子性创建 — getOrCreate()

[`getOrCreate()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:145) 使用 `ConcurrentHashMap.computeIfAbsent()` 保证缓存实例创建的原子性，同一 `CacheKey` 只会创建一次。

#### 5.2.5 配置校验 — CacheSpec.validate()

[`validate()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:275) 强制要求至少设置一种淘汰策略：

```java
boolean hasEvictionPolicy = maximumSize != null
    || expireAfterWrite != null
    || expireAfterAccess != null
    || softValues;

if (!hasEvictionPolicy) {
    throw new IllegalStateException(
        "必须设置至少一种淘汰策略: maximumSize/expireAfterWrite/expireAfterAccess/softValues");
}
```

若仅使用 `softValues` 而未设置 `maximumSize`，会输出警告日志（但仍允许创建）。

---

### 5.3 GetRedisFunction

[`GetRedisFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:12) 封装 Redis 缓存的读取与回写逻辑，需实现 3 个方法：

| 方法 | 必须实现 | 说明 |
|------|---------|------|
| `T get(R request)` | 是 | 从 Redis 获取缓存数据 |
| `void setHandler(T value)` | 是 | 将数据写入 Redis（需设置过期时间） |
| `T getNullable()` | 是 | 返回空值占位对象，**必须非 null**，防缓存穿透 |

`set(T value)` 为 `default` 方法，自动判空：非 null 直接 `setHandler`，null 则写入 `getNullable()` 返回的占位对象。

> **关键约束**：[`getNullable()`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:51) 必须返回非 null 的标记对象，否则缓存穿透防护失效。

---

### 5.4 GetDBFunction

[`GetDBFunction<T, R>`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetDBFunction.java:34) 是 `@FunctionalInterface` 函数式接口，仅需实现一个方法：

```java
T get(R request);
```

从数据库查询数据，数据不存在时返回 `null`，由上层 [`GetRedisFunction.set()`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:35) 决定缓存空值占位对象。

---

### 5.5 CacheConfig

[`CacheConfig`](simple-common-cache/src/main/java/com/simple/common/cache/common/config/CacheConfig.java:15) 是模块的 Spring 自动配置类：

```java
@Configuration
@ComponentScan(basePackages = { "com.simple.common.cache" })
public class CacheConfig {
}
```

通过 `@ComponentScan` 扫描 `com.simple.common.cache` 包下所有组件，确保模块内的 Bean 被正确注册。

---

## 6. 使用示例

以下示例来自 [`simple-common-test`](simple-common-test/src/main/java/com/simple/common/test/controller/CacheController.java:30) 中的 [`CacheController`](simple-common-test/src/main/java/com/simple/common/test/controller/CacheController.java:30)。

### 6.1 本地手动缓存 — 字符串缓存

在 `InitializingBean.afterPropertiesSet()` 中初始化缓存实例，后续直接使用：

```java
@Slf4j
@RequestMapping("cache")
@Tag(name = "缓存（展示多种构建方式）")
@RestController
public class CacheController implements InitializingBean {

    private Cache<String, String> string;

    @Operation(summary = "字符串缓存")
    @GetMapping("string")
    @SneakyThrows
    public R<Object> string() {
        // get 时若 key 不存在，通过 lambda 加载默认值
        log.debug(string.get("key", s -> "重新读取value"));
        log.debug(string.getIfPresent("key"));
        return R.ok();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化本地缓存，最大容量 100
        string = CacheUtils.createLocalCache("string", config -> config.maximumSize(100));
    }
}
```

### 6.2 本地手动缓存 — 对象缓存（含过期验证）

```java
@Operation(summary = "对象缓存")
@GetMapping("obj")
@SneakyThrows
public R<Object> obj() {

    // 创建本地缓存，写入后 6 秒过期
    Cache<String, DocTestEntity> cache = CacheUtils.createLocalCache(
        "string1", config -> config.expireAfterWrite(6));

    // 手动 put
    cache.put("string1", new DocTestEntity().setName("名字").setAge(18).setSex("女"));
    log.debug(JsonUtils.toJsonStr(cache.getIfPresent("string1")));

    // get 时若 key 不存在，通过 lambda 加载
    log.debug(JsonUtils.toJsonStr(
        cache.get("string1", s -> new DocTestEntity().setName("名字1").setAge(18).setSex("女"))));

    // 等待 10 秒，验证过期后自动重新加载
    Thread.sleep(10000);
    log.debug(JsonUtils.toJsonStr(cache.getIfPresent("string1")));
    log.debug(JsonUtils.toJsonStr(
        cache.get("string1", s -> new DocTestEntity().setName("名字1").setAge(18).setSex("女"))));

    return R.ok();
}
```

### 6.3 分布式缓存 — Redis + DB 防击穿

```java
@Operation(summary = "redis")
@GetMapping("redis/string")
@SneakyThrows
public R<Object> redisObj() {

    DocTestEntity docTestEntity = new DocTestEntity().setName("名字").setAge(18).setSex("女");

    // 实现 GetRedisFunction：Redis 读取 + 回写 + 空值占位
    GetRedisFunction<DocTestEntity, DocTestEntity> getRedisFunction = new GetRedisFunction<>() {
        @Override
        public DocTestEntity get(DocTestEntity request) {
            request.setName("名字redis");
            return null;
        }

        @Override
        public void setHandler(DocTestEntity value) {
            log.debug("赋值：==>{}", JsonUtils.toJsonStr(value));
        }

        @Override
        public DocTestEntity getNullable() {
            return new DocTestEntity();
        }
    };

    // 实现 GetDBFunction：数据库查询
    GetDBFunction<DocTestEntity, DocTestEntity> getDBFunction = (request) -> {
        request.setName("名字DB");
        log.debug("从数据库加载新的数据==>{}", JsonUtils.toJsonStr(request));
        return request;
    };

    // 分布式缓存获取（防击穿/穿透）
    DocTestEntity result = CacheUtils.get(docTestEntity, "localKey", getRedisFunction, getDBFunction);
    log.debug(JsonUtils.toJsonStr(result));

    return R.ok();
}
```

### 6.4 本地自动加载缓存（LoadingCache）

```java
// 创建自动加载缓存，key 不存在时自动调用 loader 加载
LoadingCache<String, UserDTO> userLoadingCache = CacheUtils.createLocalLoadingCache(
    "userLoading",
    key -> userRepository.findById(key),  // CacheLoader
    spec -> {
        spec.maximumSize(10000);
        spec.expireAfterWrite(300);
    }
);

// 自动加载，无需手动 put
UserDTO user = userLoadingCache.get("user:123");
```

### 6.5 主动删除缓存

```java
// 删除分布式缓存
CacheUtils.evict(userId, "user:cache:" + userId, key -> redisTemplate.delete(key));

// 移除本地缓存
CacheUtils.removeLocalCache("users", LocalCacheFactory.CacheType.MANUAL);
```

### 6.6 防雪崩过期时间

```java
// 基础 3600 秒 + 随机 0~600 秒
int expireTime = CacheUtils.getCacheTime(3600, 600);
redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
```

---

## 7. 扩展点与自定义方式

### 7.1 自定义移除监听器执行器

[`LocalCacheFactory.setRemovalListenerExecutor()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:110) 允许替换移除监听器的线程池：

```java
// 替换为虚拟线程池（Java 21+）
LocalCacheFactory.getInstance().setRemovalListenerExecutor(
    Executors.newVirtualThreadPerTaskExecutor());

// 恢复默认
LocalCacheFactory.getInstance().setRemovalListenerExecutor(null);
```

> **注意**：该方法应在任何缓存创建之前调用，否则已创建的缓存仍使用旧执行器。推荐在应用启动配置阶段调用。

### 7.2 自定义移除监听器

通过 [`CacheSpec.removalListener()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:399) 设置缓存条目移除时的回调：

```java
Cache<String, UserDTO> cache = CacheUtils.createLocalCache("users", spec -> {
    spec.maximumSize(10000);
    spec.expireAfterWrite(300);
    spec.removalListener((key, value, cause) -> {
        log.info("缓存移除: key={}, cause={}", key, cause);
    });
});
```

监听器在独立线程池中异步执行，不会阻塞缓存操作。

### 7.3 自定义 CacheLoader

[`createLocalLoadingCache()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:64) 接受 Caffeine 原生 [`CacheLoader`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:95) 接口，可实现复杂的加载逻辑：

```java
LoadingCache<String, UserDTO> cache = CacheUtils.createLocalLoadingCache(
    "users",
    new CacheLoader<String, UserDTO>() {
        @Override
        public UserDTO load(String key) {
            return userRepository.findById(key);
        }

        @Override
        public Map<String, UserDTO> loadAll(Iterable<? extends String> keys) {
            // 批量加载
            return userRepository.findAllById(keys);
        }

        @Override
        public UserDTO reload(String key, UserDTO oldValue) {
            // 异步刷新逻辑
            return userRepository.findById(key);
        }
    },
    spec -> {
        spec.maximumSize(10000);
        spec.expireAfterWrite(300);
        spec.refreshAfterWrite(60);  // 60秒后异步刷新
    }
);
```

### 7.4 自定义 GetRedisFunction 实现

[`GetRedisFunction`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:12) 可根据业务需求自定义 Redis 操作逻辑，例如结合 `RedisTemplate`：

```java
GetRedisFunction<UserDTO, String> redisFunction = new GetRedisFunction<>() {
    @Override
    public UserDTO get(String userId) {
        return (UserDTO) redisTemplate.opsForValue().get("user:cache:" + userId);
    }

    @Override
    public void setHandler(UserDTO value) {
        int expire = CacheUtils.getCacheTime(3600, 600);  // 防雪崩
        redisTemplate.opsForValue().set("user:cache:" + userId, value, expire, TimeUnit.SECONDS);
    }

    @Override
    public UserDTO getNullable() {
        return new UserDTO();  // 空值占位对象，防穿透
    }
};
```

### 7.5 开启统计信息

通过 [`CacheSpec.recordStats()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:413) 开启 Caffeine 统计：

```java
Cache<String, UserDTO> cache = CacheUtils.createLocalCache("users", spec -> {
    spec.maximumSize(10000);
    spec.expireAfterWrite(300);
    spec.recordStats();
});

// 获取统计信息
CacheStats stats = cache.stats();
log.info("命中率: {}, 平均加载时间: {}ms", stats.hitRate(), stats.averageLoadPenalty());
```

---

## 8. 注意事项

### 8.1 lockKey 与缓存 key 一致性

[`CacheUtils.get()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:99) 中的 `lockKey` **必须与** [`GetRedisFunction`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:12) 内部使用的缓存 key 保持一致，否则分布式锁无法正确保护缓存击穿场景。

### 8.2 getNullable() 必须非 null

[`GetRedisFunction.getNullable()`](simple-common-cache/src/main/java/com/simple/common/cache/common/function/GetRedisFunction.java:51) 必须返回一个非 null 的占位对象，用于标识"数据库无此数据"的状态。若返回 null，则缓存穿透防护失效。

### 8.3 强制淘汰策略

[`CacheSpec.validate()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:275) 强制要求至少设置一种淘汰策略（`maximumSize` / `expireAfterWrite` / `expireAfterAccess` / `softValues`），否则抛出 `IllegalStateException`。这是内存安全的保障措施。

### 8.4 softValues 配合 maximumSize

若仅使用 `softValues` 而未设置 `maximumSize`，可能导致内存无限增长。[`CacheSpec.validate()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:275) 会输出警告日志，强烈建议配合 `maximumSize` 使用。

### 8.5 weakKeys 的 == 比较

使用 [`weakKeys()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:370) 后，键比较使用 `==` 而非 `equals()`。请确保键对象在 JVM 中唯一（如 `String.intern()` 或使用枚举），否则可能导致缓存无法命中。

### 8.6 refreshAfterWrite 仅对 LoadingCache 生效

[`refreshAfterWrite()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:355) 仅对 `LoadingCache` 生效，且需要配合 `CacheLoader` 使用。刷新操作异步执行，不会阻塞读取。

### 8.7 移除监听器异步执行

移除监听器在独立线程池中异步执行，不会阻塞缓存操作。默认使用 `ForkJoinPool.commonPool()`，可通过 [`setRemovalListenerExecutor()`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:110) 替换。

### 8.8 evict 异常处理

[`CacheUtils.evict()`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:145) 删除失败时仅记录日志，不抛出异常。若业务要求强一致性，需在 `evictHandler` 内部自行处理异常。

### 8.9 RedissonLockService 懒加载

[`CacheUtils`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java:27) 通过双重检查锁懒加载 [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java:43) Bean，首次调用 `get()` 时才从 Spring 容器获取。需确保调用时 Spring 上下文已就绪。

### 8.10 缓存实例唯一性

[`LocalCacheFactory`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java:27) 通过 `(cacheName, cacheType)` 复合键保证缓存实例唯一性。同一名称和类型的缓存只会创建一次，重复调用返回已有实例。

---
name: "simple-common-cache"
description: "Provides complete API documentation for simple-common-cache module (multi-level cache). Invoke when using CacheUtils distributed cache or Caffeine local cache with simple-common framework."
---

# simple-common-cache 认知文档

**Maven**: `simple-common-cache`
**包路径**: `com.simple.common.cache`

## CacheUtils — 分布式缓存（Redis + DB，防击穿）

**CacheUtils 是静态工具类，所有方法直接通过类名调用，无需注入。**

### 分布式缓存获取（防击穿）

```java
UserDTO user = CacheUtils.get(
    userId,                    // R request: 请求参数
    "user:cache:" + userId,    // String lockKey: 分布式锁key（必须与缓存key一致）
    new GetRedisFunction<UserDTO, String>() {
        @Override
        public UserDTO get(String key) {
            return (UserDTO) redisTemplate.opsForValue().get("user:cache:" + key);
        }
        @Override
        public void setHandler(UserDTO value) {
            int expire = CacheUtils.getCacheTime(3600, 600);
            redisTemplate.opsForValue().set("user:cache:" + userId, value, expire, TimeUnit.SECONDS);
        }
        @Override
        public UserDTO getNullable() {
            return new UserDTO();  // 空值占位对象，防缓存穿透
        }
    },
    new GetDBFunction<UserDTO, String>() {
        @Override
        public UserDTO get(String key) {
            return userRepository.findById(key);
        }
    }
);
```

### 完整 API 签名

```java
// 分布式缓存获取（防击穿）
public static <T, R> T get(R request, String lockKey,
    GetRedisFunction<T, R> redisFunction, GetDBFunction<T, R> dbFunction);

// 分布式缓存删除
public static <R> void evict(R request, String cacheKey, Consumer<String> evictHandler);

// 缓存时间计算（防雪崩）
public static int getCacheTime(int cacheTime, int appendRandomDuration);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `request` | `R` | 请求参数，如主键ID |
| `lockKey` | `String` | 分布式锁key，**必须与缓存key一致** |
| `redisFunction` | `GetRedisFunction<T,R>` | 实现 `get()`、`setHandler()`、`getNullable()` 三个方法 |
| `dbFunction` | `GetDBFunction<T,R>` | 实现 `get()` 方法，从数据库查询 |
| `evictHandler` | `Consumer<String>` | 删除回调，如 `key -> redisTemplate.delete(key)` |
| `cacheTime` | `int` | 基础缓存秒数 |
| `appendRandomDuration` | `int` | 随机追加的最大秒数 |

### GetRedisFunction 接口（必须实现3个方法）

| 方法 | 说明 |
|------|------|
| `T get(R request)` | 从Redis获取缓存数据 |
| `void setHandler(T value)` | 将数据写入Redis（设置过期时间） |
| `T getNullable()` | 返回空值占位对象，**必须非null**，防缓存穿透 |

### GetDBFunction 接口（函数式接口，1个方法）

| 方法 | 说明 |
|------|------|
| `T get(R request)` | 从数据库查询数据，不存在返回null |

### 删除缓存

```java
CacheUtils.evict(userId, "user:cache:" + userId, key -> redisTemplate.delete(key));
```

## 本地缓存（Caffeine）

```java
// 手动缓存
Cache<String, UserDTO> userCache = CacheUtils.createLocalCache("users", spec -> {
    spec.maximumSize = 10000L;
    spec.expireAfterWrite = 300L;    // 300秒过期
});

UserDTO user = userCache.get("user:123", key -> userRepository.findById(key));
userCache.put("user:123", user);

// 自动加载缓存
LoadingCache<String, UserDTO> userLoadingCache = CacheUtils.createLocalLoadingCache(
    "userLoading",
    key -> userRepository.findById(key),  // CacheLoader
    spec -> {
        spec.maximumSize = 10000L;
        spec.expireAfterWrite = 300L;
    }
);

UserDTO user = userLoadingCache.get("user:123");  // 自动加载

// 移除缓存
CacheUtils.removeLocalCache("users", LocalCacheFactory.CacheType.MANUAL);
```

### 本地缓存完整 API 签名

```java
// 创建手动缓存（Cache）
public static <K, V> Cache<K, V> createLocalCache(
    String cacheName, Consumer<LocalCacheFactory.CacheSpec<K, V>> configurator);

// 创建自动加载缓存（LoadingCache）
public static <K, V> LoadingCache<K, V> createLocalLoadingCache(
    String cacheName, CacheLoader<K, V> loader, Consumer<LocalCacheFactory.CacheSpec<K, V>> configurator);

// 移除缓存
public static boolean removeLocalCache(String cacheName, LocalCacheFactory.CacheType cacheType);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `cacheName` | `String` | 缓存唯一标识 |
| `configurator` | `Consumer<CacheSpec>` | 配置lambda，可设 `maximumSize`、`expireAfterWrite`、`expireAfterAccess` 等 |
| `loader` | `CacheLoader<K,V>` | 缓存加载器，定义key→value的加载逻辑 |
| `cacheType` | `CacheType` | `MANUAL` 手动缓存 或 `LOADING` 自动加载缓存 |

## 核心流程图形化

### CacheUtils.get() 防击穿流程

```
CacheUtils.get(request, lockKey, redisFunction, dbFunction)
                        │
                        ▼
            ┌─────────────────────┐
            │ ① redisFunction.get │◄── 先查Redis缓存
            │   (request)         │
            └─────────┬───────────┘
                      │
            ┌─────────┴─────────┐
            │ 命中?              │
            └─────────┬─────────┘
                是    │    否
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
                      ┌─────────┴─────────┐
                      │ 命中?              │
                      └─────────┬─────────┘
                          是    │    否
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

### 两级缓存架构

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

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-cache</artifactId>
</dependency>
```
---
name: "simple-common-redis"
description: "Provides complete API documentation for simple-common-redis module (cache/rate-limit/distributed-lock). Invoke when using @RedisCache, @CurrentLimiting, or RedissonLockService with simple-common framework."
---

# simple-common-redis 认知文档

**Maven**: `simple-common-redis`
**包路径**: `com.simple.common.redis`

## @RedisCache — 声明式缓存

```java
@RedisCache(cacheTime = 300, appendRandomDuration = 60)
public List<SysUser> findAll() {
    return repository.selectList(null);
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `head()` | `String` | `REQ_URL` | 缓存key标识，默认用请求URL |
| `cacheTime()` | `int` | `10` | 缓存时长（秒） |
| `appendRandomDuration()` | `int` | `5` | 随机追加时长范围（秒），0=不追加，防缓存雪崩 |
| `aclass()` | `Class<?>` | `R.class` | 缓存返回值类型 |

## @CurrentLimiting — 接口限流

```java
@CurrentLimiting(key = CurrentLimitingRulesEnum.USER_ID, time = 10, sum = 5, waitingTime = 5)
@PostMapping("/submit")
public R<?> submit(@RequestBody SubmitRequest req) {
    // 每10秒最多5次请求，最多等待5秒
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key()` | `CurrentLimitingRulesEnum` | `URL` | 限流维度：`URL`(按接口) / `USER_ID`(按用户) / `IP`(按IP) |
| `time()` | `int` | `10` | 单位时长（秒） |
| `sum()` | `int` | `2` | 单位时长内允许的请求次数 |
| `waitingTime()` | `int` | `5` | 无令牌时最大等待时间（秒） |
| `waitingTimeErrorStr()` | `String` | `""` | 超时后抛出的异常消息 |

## RedissonLockService — 高级锁

（继承自 `LockService`，提供闭锁+信号量）

```java
@Autowired
private RedissonLockService redissonLockService;

// ==== 闭锁（CountDownLatch）— 等待N个任务全部完成 ====
redissonLockService.countDownLatch("batch:task:123", 20);   // 初始化计数器=20
redissonLockService.decreaseCountDownLatch("batch:task:123"); // 每完成一个减1，归零时唤醒等待线程

// ==== 信号量（Semaphore）— 控制并发数 ====
redissonLockService.semaphoreLock("parking:lot:A", 3);        // 初始化3个许可（3个车位）
redissonLockService.increaseSemaphoreLock("parking:lot:A", 1); // 获取1个许可（车进入）
redissonLockService.decreaseSemaphoreLock("parking:lot:A", 1); // 释放1个许可（车离开）
```

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-redis</artifactId>
</dependency>
```
## 4. simple-common-redis

### 模块介绍

Redis封装模块，提供分布式锁、限流、缓存等功能。

### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 分布式锁服务 | `RedissonLockService` | 可重入锁、公平锁、闭锁、信号量 |
| 限流注解 | `@CurrentLimiting` | 方法级限流，支持URL/用户/IP三种维度 |
| 缓存注解 | `@RedisCache` | 方法级缓存，防雪崩、防击穿 |
| 限流管理 | `CurrentLimitingManager` | 基于令牌桶的限流实现 |
| 缓存Key生成 | `RedisCacheAspectManager` | 缓存Key生成策略 |

### 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-redis</artifactId>
    <version>${version}</version>
</dependency>
```

**步骤2：配置Redis连接（必须）**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your_password
      database: 0
```

**步骤3：启用Redisson（可选）**

```yaml
redisson:
  open: true  # 默认开启，用于分布式锁等功能
```

**重要说明：**
- Redisson默认开启，提供分布式锁、限流、缓存等功能
- 如果不需要分布式锁功能，可以关闭Redisson以节省资源
- 限流和缓存注解需要Redisson支持

### 使用示例

**1. 可重入锁（无返回值）**

```java
@Service
public class OrderService {
    
    @Autowired
    private RedissonLockService lockService;
    
    public void createOrder(Order order) {
        String lockKey = "order:create:" + order.getUserId();
        
        lockService.lock(lockKey, () -> {
            // 业务逻辑，自动加锁/解锁
            // 锁会自动续期，每30秒刷新一次
            orderMapper.insert(order);
        });
    }
}
```

**2. 可重入锁（有返回值）**

```java
@Service
public class ProductService {
    
    @Autowired
    private RedissonLockService lockService;
    
    public Product getProduct(String productId) {
        String lockKey = "product:query:" + productId;
        
        return (Product) lockService.lockHaveValue(lockKey, () -> {
            // 查询数据库
            return productMapper.selectById(productId);
        });
    }
}
```

**3. 公平锁**

```java
@Service
public class ResourceService {
    
    @Autowired
    private RedissonLockService lockService;
    
    public void accessResource(String resourceId) {
        String lockKey = "resource:access:" + resourceId;
        
        // 按请求顺序获取锁，先来的先执行
        lockService.fairLock(lockKey, () -> {
            // 业务逻辑
            processResource(resourceId);
        });
    }
}
```

**4. 闭锁（CountDownLatch）**

```java
@Service
public class BatchTaskService {
    
    @Autowired
    private RedissonLockService lockService;
    
    /**
     * 等待多个子任务完成
     */
    public void executeBatchTask(String taskId, int taskCount) {
        String lockKey = "task:batch:" + taskId;
        
        // 启动主线程等待
        new Thread(() -> {
            lockService.countDownLatch(lockKey, taskCount);
            log.info("所有子任务完成，继续执行");
            // 后续处理
        }).start();
        
        // 启动子任务
        for (int i = 0; i < taskCount; i++) {
            int finalI = i;
            new Thread(() -> {
                // 执行子任务
                executeSubTask(taskId, finalI);
                
                // 完成任务，计数器减1
                lockService.decreaseCountDownLatch(lockKey);
            }).start();
        }
    }
}
```

**5. 信号量（Semaphore）**

```java
@Service
public class ParkingLotService {
    
    @Autowired
    private RedissonLockService lockService;
    
    private static final String SEMAPHORE_KEY = "parking:lot:A";
    
    /**
     * 初始化停车场（3个车位）
     */
    @PostConstruct
    public void init() {
        lockService.semaphoreLock(SEMAPHORE_KEY, 3);
    }
    
    /**
     * 车辆进入
     */
    public boolean enterParking(String vehicleId) {
        try {
            // 获取许可（阻塞等待）
            lockService.decreaseSemaphoreLock(SEMAPHORE_KEY, 1);
            log.info("车辆 {} 进入停车场", vehicleId);
            return true;
        } catch (Exception e) {
            log.warn("停车场已满");
            return false;
        }
    }
    
    /**
     * 车辆离开
     */
    public void leaveParking(String vehicleId) {
        // 释放许可
        lockService.increaseSemaphoreLock(SEMAPHORE_KEY, 1);
        log.info("车辆 {} 离开停车场", vehicleId);
    }
}
```

**6. 限流注解（@CurrentLimiting）**

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    /**
     * 按URL限流：10秒内最多2次请求
     */
    @GetMapping("/data")
    @CurrentLimiting(time = 10, sum = 2)
    public R getData() {
        return R.ok(dataService.getData());
    }
    
    /**
     * 按用户ID限流：60秒内最多10次请求
     */
    @PostMapping("/submit")
    @CurrentLimiting(
        key = CurrentLimitingRulesEnum.USER_ID,
        time = 60,
        sum = 10
    )
    public R submit(@RequestBody Request request) {
        return R.ok();
    }
    
    /**
     * 按IP限流：带自定义错误信息
     */
    @GetMapping("/search")
    @CurrentLimiting(
        key = CurrentLimitingRulesEnum.IP,
        time = 1,
        sum = 5,
        waitingTimeErrorStr = "搜索太频繁，请稍后再试"
    )
    public R search(@RequestParam String keyword) {
        return R.ok(searchService.search(keyword));
    }
}
```

**限流规则枚举：**

| 枚举值 | 说明 |
|--------|------|
| `URL` | 按请求URL限流 |
| `USER_ID` | 按当前登录用户ID限流 |
| `IP` | 按客户端IP限流 |

**注意**：使用 `USER_ID` 限流时，需要从上下文中获取用户ID，确保已集成auth模块。

**7. 缓存注解（@RedisCache）**

```java
@RestController
@RequestMapping("/api")
public class DataController {
    
    /**
     * 基本缓存：缓存10秒，追加0-5秒随机时长（防雪崩）
     */
    @GetMapping("/user/{id}")
    @RedisCache(cacheTime = 10, appendRandomDuration = 5)
    public R getUser(@PathVariable String id) {
        // 首次请求：查数据库并缓存
        // 后续请求：直接返回缓存
        User user = userService.findById(id);
        return R.ok(user);
    }
    
    /**
     * 自定义缓存Key前缀
     */
    @GetMapping("/config")
    @RedisCache(head = "config:global", cacheTime = 3600)
    public R getConfig() {
        Config config = configService.getGlobalConfig();
        return R.ok(config);
    }
}
```

**缓存机制：**

```
首次请求
   ↓
查询Redis缓存
   ↓
未命中 → 获取分布式锁
   ↓
查询数据库
   ↓
存入Redis（cacheTime + 随机偏移）
   ↓
释放锁，返回结果

后续请求
   ↓
查询Redis缓存
   ↓
命中 → 直接返回

并发请求
   ↓
第一个请求获取锁，查库并缓存
   ↓
其他请求等待锁释放
   ↓
从缓存读取
```

**防雪崩机制：**
- ✅ **随机过期时间**：`appendRandomDuration` 在基础过期时间上增加0-N秒随机偏移
- ✅ **分布式锁**：防止缓存击穿，同一时刻只有一个请求查库
- ✅ **并发控制**：其他请求等待缓存更新完成

**缓存Key生成规则：**
- 默认：`请求URI + & + 参数哈希值`
- 自定义：`head参数 + & + 参数哈希值`
- 无参数：`请求URI + & + noParametersKey配置值`

---

[返回主文档](../README.md)

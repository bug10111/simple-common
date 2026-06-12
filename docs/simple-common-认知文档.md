# simple-common 框架认知文档

> 作者: qty | 完整 API 索引，含参数与调用示例

---

## 快速查找索引

| # | 模块 | 行号 | 一句话定位 |
|---|------|------|-----------|
| 1 | **core** 核心基础设施 | ~L60 | 统一响应 `R<T>`、异常体系、30+工具类、线程池/分布式锁/循环任务接口 |
| 2 | **mp** MyBatis Plus封装 | ~L390 | `PageBase` 分页基类、雪花ID、自动填充 `createTime/updateTime`、`@DataScopeTable` 数据权限 |
| 3 | **redis** 缓存/限流/分布式锁 | ~L470 | `@RedisCache` 声明式缓存、`@CurrentLimiting` 限流、`RedissonLockService` 分布式锁+闭锁+信号量 |
| 4 | **cache** 多级缓存 | ~L590 | Caffeine本地缓存+Redis分布式缓存两级方案，防击穿 |
| 5 | **rabbitmq** 消息队列 | ~L680 | `RabbitMqService` 即时/延迟消息、`@RabbitMqConsumption` 防重复消费 |
| 6 | **eventbus** 事件总线 | ~L780 | `@Event`+`@EventHandler` 注解驱动、同步/MQ异步/延迟三种模式 |
| 7 | **auth** 认证授权体系 | ~L880 | `@HasAuthority`权限、`@Sign`签名、`@CsrfDefense`防重复、`LoginManager`登录、`PermissionManageService`权限管理、`UnifiedSecretManager`密钥管理、`TokenManager`令牌、`PermissionAutoLoader`权限加载 |
| 8 | **logs** 日志系统 | ~L1360 | TCP+Protobuf高性能日志收集，`LogService`/`LogManager`/`BufferedLogManager`/`LogUserManager`/`LogDataEvent`/`AbsLogsSaveManager` |
| 9 | **websocket** 通信 | ~L1540 | Netty+`@WebSocketListening`注解分发、`ChannelMap`多级通道 |
| 10 | **excel** 导入导出 | ~L1630 | EasyExcel写入/读取 + POI复杂导出（自定义列宽分页） |
| 11 | **sms** 短信服务 | ~L1780 | 验证码发送校验、模板短信、IP/手机号防刷 |
| 12 | **annex** 附件管理 | ~L1860 | S3协议上传/下载/删除/生成URL，支持MinIO/OSS/AWS |
| 13 | **doc** 文档模板 | ~L2070 | Poi-Tl Word模板填充：文本/图片/表格/列表 |
| 14 | **xxljob** 任务调度 | ~L2170 | XXL-JOB任务CRUD HTTP封装 |
| 15 | **ai** 大模型集成 | ~L2260 | 阿里云百炼对话封装 |
| 16 | **alibaba** 阿里组件 | ~L2300 | Sentinel用户级两级限流+Feign配置 |
| 17 | POM依赖关系 | ~L1940 | 全模块依赖树 |

---

## 1. core 核心模块

**Maven**: `simple-common-core`
**包路径**: `com.simple.common.core`

### 1.1 统一响应 R\<T\>

**完整 API**：

```java
// 成功响应
R.ok();                    // 返回 code="200", message="success", data=null
R.ok(data);                // 返回 code="200", message="success", data=你的数据

// 失败响应
R.error();                 // 返回 code="500", message="error", data=null
R.error(data);             // 返回 code="500", message="error", data=你的数据
R.error("code", "msg");    // 自定义 code 和 message

// 构造器
new R<>(abstractException);               // 从异常枚举构造
new R<>(abstractException, data);         // 从异常枚举构造 + 数据
new R<>("500", "自定义错误消息");          // 自定义code+message
```

**Controller 实际调用示例**：

```java
@GetMapping("/user/{id}")
public R<SysUser> getUser(@PathVariable String id) {
    SysUser user = userService.findById(id);
    return R.ok(user);
}

@PostMapping("/user")
public R<String> createUser(@RequestBody CreateSysUserRequest req) {
    AssertUtils.notEmpty(req.getUsername(), "用户名不能为空");
    String id = userService.save(req);
    return R.ok(id);
}
```

---

### 1.2 AssertUtils 断言工具

**完整 API（所有重载）**：

```java
// ===== notEmpty — 校验对象非空（支持 CharSequence/Map/Iterable/Iterator/Array） =====
AssertUtils.notEmpty(obj, "错误消息");
AssertUtils.notEmpty(obj, "用户[{}]不存在", userId);                          // 支持 {} 占位符
AssertUtils.notEmpty(obj, YourExceptionEnum.XXX);                              // 自定义异常枚举
AssertUtils.notEmpty(obj, YourExceptionEnum.XXX, "重写错误消息");
AssertUtils.notEmpty(obj, YourExceptionEnum.XXX, "用户[{}]不存在", userId);    // 异常枚举 + 占位符

// ===== notNull — 校验非null =====
AssertUtils.notNull(obj, "对象不能为空");

// ===== isTrue — 断言布尔表达式 =====
AssertUtils.isTrue(age > 0, "年龄必须大于0");
AssertUtils.isTrue(status == 1, "状态[{}]不正确", status);
AssertUtils.isTrue(flag, YourExceptionEnum.XXX);
AssertUtils.isTrue(flag, YourExceptionEnum.XXX, "重写消息");
AssertUtils.isTrue(flag, YourExceptionEnum.XXX, "用户[{}]无权限", userId);

// ===== error — 直接抛出异常 =====
AssertUtils.error("错误消息");
AssertUtils.error(YourExceptionEnum.XXX);
AssertUtils.error(YourExceptionEnum.XXX, "重写消息");
AssertUtils.error(YourExceptionEnum.XXX, "用户[{}]无权限", userId);
```

**实际调用示例**：

```java
SysUser user = userRepository.findById(id);
AssertUtils.notEmpty(user, "主键为[{}]的用户不存在", id);

AssertUtils.isTrue(user.getStatus() == 1, "用户已被禁用，无法操作");
```

---

### 1.3 DefaultException 自定义异常

```java
// 构造方法
new DefaultException("错误消息");                          // code="500"
new DefaultException("CUSTOM_CODE", "错误消息");            // 自定义code
new DefaultException("CUSTOM_CODE", "错误消息", data);      // 附带数据
new DefaultException(abstractException);                    // 从异常枚举构造
new DefaultException(abstractException, "重写消息");         // 异常枚举 + 重写消息
```

---

### 1.4 工具类完整 API

#### IdUtils — 唯一ID生成

```java
IdUtils.getSnowflakeNextIdStr();      // 雪花ID（推荐，全局唯一递增）
IdUtils.getFastSimpleUUID();          // UUID去横线
IdUtils.getFastUUID();                // 标准UUID
IdUtils.randomNumbers();              // 9位随机数字
IdUtils.randomNumbers(6);             // 指定长度随机数字
```

#### JsonUtils — JSON转化

```java
JsonUtils.toJsonStr(obj);              // 对象 → JSON字符串
JsonUtils.toJsonPrettyStr(obj);        // 对象 → 格式化JSON字符串
JsonUtils.toJsonObj(jsonStr, UserDTO.class);  // JSON字符串 → 对象
JsonUtils.toList(jsonStr, UserDTO.class);     // JSON数组字符串 → List<对象>
```

#### ThreadUtils — 多线程工具

```java
// 异步执行（使用内置高性能线程池）
CompletableFuture<UserDTO> future = ThreadUtils.supplyAsync(() -> userService.findById(id));
CompletableFuture<Void> future = ThreadUtils.runAsync(() -> logService.save(log));

// 延时任务
ThreadUtils.schedule(() -> orderService.cancelTimeout(orderId), 30, TimeUnit.MINUTES);
ScheduledFuture<?> sf = ThreadUtils.scheduleWithFuture(task, 30, TimeUnit.SECONDS);

// 定时任务（任务完成后等固定间隔再执行）
ThreadUtils.scheduleWithFixedDelay(() -> syncService.sync(), 60, TimeUnit.SECONDS);
ThreadUtils.scheduleWithFixedDelay(() -> syncService.sync(), 0, 60, TimeUnit.SECONDS);
// 返回 ScheduledFuture 用于取消
ScheduledFuture<?> sf = ThreadUtils.scheduleWithFixedDelayFuture(task, 60, TimeUnit.SECONDS);

// 定时任务（安全版，自动捕获异常不中断调度）
ThreadUtils.scheduleWithFixedDelaySafe(() -> riskyTask(), 60, TimeUnit.SECONDS);

// 获取线程池
ScheduledThreadPoolExecutor pool = ThreadUtils.getScheduledThreadPoolExecutor();  // 轻量延迟池
ExecutorService pool = ThreadUtils.getAsyncExecutor();  // 高并发异步池

// 监控
int queueSize = ThreadUtils.getScheduledQueueSize();  // 调度队列大小
int activeCount = ThreadUtils.getAsyncActiveCount();  // 活跃线程数
int queueSize = ThreadUtils.getAsyncQueueSize();      // 异步队列大小
```

#### BeanUtils — 对象工具

```java
// 对象转Map（驼峰key）
Map<String, Object> map = BeanUtils.toMap(user);

// Map填充到对象
SysUser user = BeanUtils.fillBeanWithMap(map, SysUser.class);

// copyProperties（继承自 hutool BeanUtil）
BeanUtils.copyProperties(source, target);
```

#### SignUtils — 签名工具

```java
// 生成API签名字符串（排除敏感字段，按参数名ASCII排序）
String signStr = SignUtils.generateSignStr(requestDTO, "secret", "password");

// Web请求 HMAC-SHA256 签名（服务端间调用）
String signature = SignUtils.signWeb("待签名的消息体", "你的秘钥（建议使用无横线UUID）");
boolean ok = SignUtils.verifyWeb("原消息体", "收到的签名", "共享秘钥");

// RSA/SM2 签名（非对称）
byte[] sig = SignUtils.sign(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, privateKey, data);
boolean ok = SignUtils.verify(CryptoUtil.AsymmetricAlgorithmType.SM2, publicKey, data, sig);
```

#### CryptoUtil — 加解密工具

```java
// ==== 对称加密（AES/SM4/DES） ====
// 生成秘钥
byte[] key = CryptoUtil.generateSymmetricKey(CryptoUtil.SymmetricAlgorithmType.AES_GCM);
String keyBase64 = CryptoUtil.generateSymmetricKeyStr(CryptoUtil.SymmetricAlgorithmType.SM4_GCM);

// 加密/解密（字节数组）
byte[] encrypted = CryptoUtil.encrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, data);
byte[] decrypted = CryptoUtil.decrypt(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, encrypted);

// 加密/解密（Base64字符串，推荐日常使用）
String encStr = CryptoUtil.encryptStr(CryptoUtil.SymmetricAlgorithmType.AES_GCM, keyBase64, "明文");
String decStr = CryptoUtil.decryptStr(CryptoUtil.SymmetricAlgorithmType.AES_GCM, keyBase64, encStr);

// ==== 非对称加密（RSA/SM2） ====
// 生成密钥对
KeyPair keyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);

// PEM格式转换
String pubPem = CryptoUtil.getPublicKeyPem(keyPair.getPublic());
String priPem = CryptoUtil.getPrivateKeyPem(keyPair.getPrivate());

// 加密（公钥）/解密（私钥）
String enc = CryptoUtil.encryptStr(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, pubPem, "明文");
String dec = CryptoUtil.decryptStr(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, priPem, enc);

// ==== 哈希 ====
byte[] hash = CryptoUtil.hash(CryptoUtil.HashAlgorithmType.SHA256, data);
String hashStr = CryptoUtil.hashStr(CryptoUtil.HashAlgorithmType.SM3, "待哈希文本");

// ==== 密码哈希（BCrypt） ====
String hashed = CryptoUtil.hashPassword("用户密码");           // 存储到数据库
boolean match = CryptoUtil.verifyPassword("用户输入密码", hashed);  // 登录时校验
```

| 对称算法枚举 | 说明 | 安全性 |
|-------------|------|--------|
| `AES_GCM` | AES GCM模式 | ✅ 推荐 |
| `SM4_GCM` | 国密SM4 GCM模式 | ✅ 推荐 |
| `AES_CBC` | AES CBC模式 | ⚠️ 仅兼容旧系统 |
| `SM4_CBC` | SM4 CBC模式 | ⚠️ 仅兼容旧系统 |
| `DES_CBC` | DES CBC模式 | ⚠️ 仅兼容旧系统 |

| 非对称算法枚举 | 说明 | 安全性 |
|---------------|------|--------|
| `RSA_OAEP` | RSA OAEP填充 | ✅ 推荐 |
| `SM2` | 国密SM2 | ✅ 推荐 |
| `RSA_PSS` | RSA PSS签名 | ⚠️ 签名可用 |
| `RSA_PKCS1` | RSA PKCS1 | ⚠️ 仅兼容旧系统 |

#### FileUtils — 文件工具

```java
// 将输入流写入文件（自动关闭流）
FileUtils.write(inputStream, "/data/exports/report.xlsx");

// 创建空白文件（自动删除旧文件）
File f = FileUtils.createFile("/data/logs/app.log");

// 读取resources下文件
InputStream is = FileUtils.getResourcesFileInputStream("/templates/contract.docx");

// 获取resources目录路径
String path = FileUtils.getPath();           // 如 /app/classes/
String path = FileUtils.getPath("templates"); // 如 /app/classes/templates
```

#### Base64Utils

```java
String enc = Base64Utils.encode("文本");            // 文本/字节 → Base64字符串
byte[] dec = Base64Utils.decode("Base64字符串");     // Base64字符串 → 字节数组
String str = Base64Utils.decodeStr("Base64字符串");  // Base64字符串 → 原始字符串
```

#### IPUtils

```java
String ip = IPUtils.getIpAddr();        // 获取当前请求客户端IP
String ip = IPUtils.getIntranetIp();    // 获取服务器内网IP
```

#### ResponseUtils

```java
// Controller中直接写JSON到HTTP响应（用于下载场景）
ResponseUtils.writeResponse("文件名.docx", byteArrayOutputStream);
```

---

### 1.5 核心服务接口

#### LockService — 分布式锁

```java
// 注入
@Autowired
private LockService lockService;

// 可重入锁（无返回值）
lockService.lock("order:" + orderId, () -> {
    orderService.updateStatus(orderId, "PAID");
});

// 可重入锁（有返回值）
Integer newStock = (Integer) lockService.lockHaveValue("stock:" + productId, () -> {
    return stockService.decrease(productId, 5);
});

// 公平锁（FIFO，抢票/秒杀场景）
lockService.fairLock("ticket:" + ticketId, () -> {
    if (ticketService.hasRemaining(ticketId)) {
        ticketService.issue(userId, ticketId);
    }
});
```

**参数说明**：
| 参数 | 类型 | 说明 |
|------|------|------|
| `key` | `String` | 锁标识，相同key的请求排队执行 |
| `function` | `DefaultFunction` (Runnable) | 业务逻辑，无返回值 |
| `function` | `ReturnValueFunction` (Supplier) | 业务逻辑，有返回值 |

#### ThreadService — 线程池管理

```java
// 不直接使用，通过 ThreadUtils 静态方法调用（见1.4节）
// 如需自定义可注入实现
@Autowired
private ThreadService threadService;
```

#### CycleService\<T\> — 循环任务

**抽象类继承方式使用**：

```java
@Service
public class OrderStatusCheckService extends AbsCycleService<OrderQuery> {
    @Override
    public void execute(OrderQuery query) {
        Order order = orderService.findById(query.getOrderId());
        if (order.isPaid()) {
            complete(query);  // 完成，停止循环
        } else {
            throw new RuntimeException("订单未支付");  // 抛出异常触发重试
        }
    }

    @Override
    public void complete(OrderQuery query) {
        log.info("订单 {} 已支付", query.getOrderId());
    }
}

// 调用
orderStatusCheckService.runUniform(query, 5, 10);      // 最多5次，每次间隔10秒
orderStatusCheckService.runAccumulate(query, 5, 10);   // 累加延迟：10s, 20s, 30s, 40s
```

| 方法 | 参数 | 说明 |
|------|------|------|
| `runUniform(T body, Integer sum, Integer interval)` | body=数据, sum=最大次数, interval=间隔秒数 | 均匀延迟 |
| `runAccumulate(T body, Integer sum, Integer interval)` | 同上 | 累加延迟(interval×次数) |
| `run(T body, Integer sum, Integer interval, Boolean isAccumulate, Map params)` | isAccumulate=true累加/false均匀 | 完整参数版 |

---

### 1.6 core POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-core</artifactId>
</dependency>
```

---

## 2. mp MyBatis Plus封装

**Maven**: `simple-common-mp`
**包路径**: `com.simple.common.mp`

### 2.1 PageBase — 分页基类

**DTO 继承方式**：

```java
@Getter
@Setter
public class PageSysUserRequest extends PageBase {
    private String nickname;
    private Integer status;
}
```

**分页参数**：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `current` | `Integer` | `1` | 当前页 |
| `size` | `Integer` | `10` | 每页条数，最大1000 |
| `pageSort` | `String[]` | `{}` | 排序：`{"createTime-false"}`（字段名-true正序/false倒序） |

**Controller/Service 完整用法**：

```java
// Controller
@GetMapping("/user/page")
@HasAuthority({"sys:user:page"})
public R<IPage<PageSysUserResponse>> page(PageSysUserRequest req) {
    return R.ok(sysUserService.findAll(req));
}

// Service实现
@Override
public IPage<PageSysUserResponse> findAll(PageSysUserRequest req) {
    Page<?> page = req.getPage();                           // 获取MP分页对象
    List<PageSysUserResponse> list = repository.selectPageWithUserNames(page, req);
    return new Page<PageSysUserResponse>(page.getCurrent(), page.getSize(), page.getTotal()).setRecords(list);
}

// Repository
List<PageSysUserResponse> selectPageWithUserNames(
    @Param("page") Page<SysUser> page,
    @Param("pageRequest") PageSysUserRequest pageRequest);
```

### 2.2 自动功能

| 功能 | 说明 |
|------|------|
| 雪花ID | `CustomIdGenerator` 自动为所有 `String id` 主键生成雪花ID |
| 自动填充 | `MybatisPlusOperationHandler` 自动填充 `createTime` / `updateTime` |
| 数据权限 | 实体加 `@DataScopeTable` → 实现 `DataScopeSqlHandler` → 拦截器自动注入WHERE条件 |

### 2.3 mp POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-mp</artifactId>
</dependency>
```

---

## 3. Redis 缓存/限流/分布式锁

**Maven**: `simple-common-redis`
**包路径**: `com.simple.common.redis`

### 3.1 @RedisCache — 声明式缓存

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

### 3.2 @CurrentLimiting — 接口限流

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

### 3.3 RedissonLockService — 高级锁

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

### 3.4 Redis POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-redis</artifactId>
</dependency>
```

---

## 4. Cache 多级缓存

**Maven**: `simple-common-cache`
**包路径**: `com.simple.common.cache`

### 4.1 CacheUtils — 分布式缓存（Redis + DB，防击穿）

```java
// 获取数据：先Redis → 未命中 → 分布式锁 → 双重检查 → 查DB → 写回Redis
UserDTO user = CacheUtils.get(
    userId,                    // R request: 请求参数
    "user:cache:" + userId,    // String lockKey: 分布式锁key（必须与缓存key一致）
    new GetRedisFunction<UserDTO, String>() {
        @Override
        public UserDTO get(String key) {
            return (UserDTO) redisTemplate.opsForValue().get("user:cache:" + key);
        }
        @Override
        public void set(UserDTO value) {
            int expire = CacheUtils.getCacheTime(3600, 600);  // 1小时 + 随机10分钟防雪崩
            redisTemplate.opsForValue().set("user:cache:" + userId, value, expire, TimeUnit.SECONDS);
        }
    },
    new GetDBFunction<UserDTO, String>() {
        @Override
        public UserDTO get(String key) {
            return userRepository.findById(key);
        }
    }
);

// 删除缓存
CacheUtils.evict(userId, "user:cache:" + userId, key -> redisTemplate.delete(key));
```

### 4.2 CacheUtils 完整 API

**CacheUtils 是静态工具类，所有方法直接通过类名调用，无需注入。**

#### 分布式缓存（Redis + DB，防击穿）

```java
// 获取数据：先Redis → 未命中 → 分布式锁 → 双重检查 → 查DB → 写回Redis
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

// 删除缓存
CacheUtils.evict(userId, "user:cache:" + userId, key -> redisTemplate.delete(key));

// 计算缓存过期时间（基础时间 + 随机偏移，防雪崩）
int expireSeconds = CacheUtils.getCacheTime(3600, 600);  // 3600~4200秒
```

**完整 API 签名**：
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

**GetRedisFunction 接口（必须实现3个方法）**：

| 方法 | 说明 |
|------|------|
| `T get(R request)` | 从Redis获取缓存数据 |
| `void setHandler(T value)` | 将数据写入Redis（设置过期时间） |
| `T getNullable()` | 返回空值占位对象，**必须非null**，防缓存穿透 |

**GetDBFunction 接口（函数式接口，1个方法）**：

| 方法 | 说明 |
|------|------|
| `T get(R request)` | 从数据库查询数据，不存在返回null |

#### 本地缓存（Caffeine）

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

**本地缓存完整 API 签名**：
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

### 4.3 Cache POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-cache</artifactId>
</dependency>
```

---

## 5. RabbitMQ 消息队列

**Maven**: `simple-common-rabbitmq`
**包路径**: `com.simple.common.rabbitmq`

### 5.1 RabbitMqService — 消息发送

```java
@Autowired
private RabbitMqService rabbitMqService;

// 发送即时消息
DefaultMessage msg = new DefaultMessage();
msg.setData(orderInfo);                   // 业务数据对象（会被序列化为JSON存入msgData）
msg.setBizType("order");                  // 业务类型标识（存入properties header）
rabbitMqService.sendMsg(msg, "order.exchange", "order.created");

// 发送延迟消息（需安装 rabbitmq-delayed-message-exchange 插件）
rabbitMqService.sendMsg(msg, "order.delayed.exchange", "order.timeout", 30, TimeUnit.MINUTES);
```

**DefaultMessage 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `msgData` | `String` | 序列化后的业务数据JSON |
| `sendTime` | `String` | 发送时间（自动生成） |
| `id` | `String` | 消息唯一ID（自动生成雪花ID） |

**重要**：发送前必须调用 `msg.initialization()` 或手动设置 id。框架实现类会自动调用。

```java
// 标准发送流程
DefaultMessage msg = new DefaultMessage();
msg.initialization();    // 初始化 id 和 sendTime
msg.setBizType("order");
msg.setData(orderDTO);   // 业务数据
rabbitMqService.sendMsg(msg, "order.exchange", "order.routingKey");
```

### 5.2 @RabbitMqConsumption — 防重复消费

```java
@RabbitListener(queues = "order.queue")
public void onMessage(Message message, Channel channel) {
    // 处理消息...
}

// 在处理方法上加注解
@RabbitMqConsumption(businessTime = 30, effectiveTime = 1800, retryCount = 3)
public void handleOrder(OrderDTO order) {
    orderService.process(order);
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `businessTime()` | `int` | `10` | 业务执行时长（秒），此时间内重复消息被拦截 |
| `effectiveTime()` | `int` | `1800` | 消息有效时长（秒），用于判断重复消费的最大时间 |
| `timeUnit()` | `TimeUnit` | `SECONDS` | 时长单位 |
| `retryCount()` | `int` | `3` | 消费失败后最大重试次数 |

**前提**：必须设置手动ACK模式 `spring.rabbitmq.listener.simple.acknowledge-mode=manual`

### 5.3 RabbitMQ POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-rabbitmq</artifactId>
</dependency>
```

---

## 6. EventBus 事件总线

**Maven**: `simple-common-eventbus`
**包路径**: `com.simple.common.eventbus`

### 6.1 三步使用流程

**步骤一**：定义事件类

```java
@Event("userCreated")                    // value=事件名称，默认取短类名
public class UserCreatedEvent {
    private String userId;
    private String username;
    // getter/setter...
}
```

| `@Event` 属性 | 类型 | 默认值 | 说明 |
|--------------|------|--------|------|
| `value()` | `String` | `""` | 事件名称，不指定取短类名 |
| `targets()` | `String[]` | `THIS_MACHINE` | 目标系统名称，同步事件无效 |

**步骤二**：定义处理器

```java
@Component
public class UserEventHandler {

    @EventHandler("userCreated")
    public void handleUserCreated(UserCreatedEvent event) {
        // 发送欢迎邮件
        emailService.sendWelcome(event.getUserId());
        // 初始化用户数据
        userService.initData(event.getUserId());
    }
}
```

| `@EventHandler` 属性 | 类型 | 默认值 | 说明 |
|----------------------|------|--------|------|
| `value()` | `String` | `""` | 要处理的事件名称 |
| `applicationName()` | `String[]` | `THIS_MACHINE` | 接收哪些系统的消息 |

**步骤三**：发布事件

```java
@Autowired
private EventBusService eventBusService;

// 同步发布（默认 SyncEventBusService）
eventBusService.push(new UserCreatedEvent().setUserId("123").setUsername("张三"));

// 延迟发布
eventBusService.push(new OrderTimeoutEvent().setOrderId("ORDER123"), 30, TimeUnit.MINUTES);

// 指定事件类型发布（将Map转为事件对象）
Map<String, Object> data = new HashMap<>();
data.put("userId", "123");
eventBusService.push(data, UserCreatedEvent.class);

// 指定类型 + 延迟
eventBusService.push(data, UserActiveReminderEvent.class, 24, TimeUnit.HOURS);
```

### 6.2 EventBusService 完整 API

```java
void push(Object event);                                                      // 自动识别事件类型
void push(Object obj, Class<?> eventClass);                                   // 指定事件类型
void push(Object event, int time, TimeUnit timeUnit);                         // 延迟+自动识别
void push(Object obj, Class<?> eventClass, int time, TimeUnit timeUnit);      // 延迟+指定类型
```

| 实现类 | 模式 | 说明 |
|--------|------|------|
| `SyncEventBusService` | 同步 | 事件在同一线程/进程内处理 |
| `MqEventBusService` | 异步(MQ) | 事件通过RabbitMQ发送到其他服务 |

### 6.3 EventBus POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-eventbus</artifactId>
</dependency>
```

---

## 7. Auth 认证授权体系

**Maven**: `simple-common-auth-client`（客户端）/ `simple-common-auth-server`（服务端）
**包路径**: `com.simple.common.auth`

### 7.1 客户端注解

#### @HasAuthority — 权限校验

```java
@HasAuthority({"sys:user:page"})
@GetMapping("/page")
public R<IPage<PageSysUserResponse>> page(PageSysUserRequest req) {
    // 超级管理员自动放行，其他用户校验权限
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| `value()` | `String[]` | 权限标识数组，满足任意一个即可 |

**权限值命名规则**：`@RequestMapping路径:方法路径`，`/`→`:`，剔除PathVariable。示例：`@RequestMapping("sys/department")` + `@GetMapping("tree")` → `sys:department:tree`

#### @Sign — 接口签名

```java
@Sign(excludeFields = {"password", "secret"}, checkTimestamp = true, checkNonce = true)
@PostMapping("/sensitive/api")
public R<?> sensitiveApi(@RequestBody SensitiveRequest req) {
    // 自动校验：签名、时间戳有效期、nonce防重放
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled()` | `boolean` | `true` | 是否启用签名校验 |
| `excludeFields()` | `String[]` | `{}` | 参与签名字段中需排除的（如password、secret） |
| `checkTimestamp()` | `boolean` | `true` | 是否校验时间戳（防过期请求） |
| `checkNonce()` | `boolean` | `true` | 是否校验nonce（防重放攻击） |

#### @CsrfDefense — CSRF防御 + 防重复提交

```java
@CsrfDefense(consume = true)
@PostMapping("/order/submit")
public R<?> submitOrder(@RequestBody SubmitOrderRequest req) {
    // consume=true: token一次性，提交后立即删除
    // consume=false: token可复用直到过期
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `consume()` | `boolean` | `true` | 校验后是否立即删除token，true=一次性 |

### 7.2 LoginUserUtils — 获取当前登录用户

```java
UserTemporary user = LoginUserUtils.getUserTemporary();
String userId = user.getUserId();
String nickname = user.getNickname();
String clientId = user.getClientId();
String loginKey = user.getLoginKey();         // 登录标识（区分同一用户不同设备）
String jti = user.getJti();                   // JWT唯一标识
HashSet<String> scopes = user.getScopes();    // 权限作用域（all/write/read）
HashSet<String> loginRole = user.getLoginRole(); // 登录角色集合

// 数据权限相关（通过 DataPermission 对象获取）
DataPermission dp = user.getDataPermission();
String tenantId = dp.getTenantId();
DataScopeEnum scope = dp.getPermissionScope();
Set<String> departmentIds = dp.getDepartmentIds();  // 已展开的子部门ID
```

**UserTemporary 完整字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `String` | 用户ID |
| `nickname` | `String` | 用户昵称 |
| `loginKey` | `String` | 登录标识（区分同一用户不同登录设备） |
| `jti` | `String` | JWT唯一标识 |
| `path` | `String` | 请求路径 |
| `clientId` | `String` | 客户端ID |
| `clientName` | `String` | 客户端名称 |
| `appNames` | `String` | 可访问的微服务名称集合（逗号分隔） |
| `wxAppId` | `String` | 微信小程序AppID |
| `scopes` | `HashSet<String>` | 权限作用域 |
| `loginRole` | `HashSet<String>` | 登录角色集合 |
| `extension` | `Object` | 扩展信息 |
| `dataPermission` | `DataPermission` | 数据权限对象 |

**DataPermission 字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `tenantId` | `String` | 租户ID |
| `permissionScope` | `DataScopeEnum` | 数据权限类型 |
| `departmentIds` | `Set<String>` | 部门ID集合（已展开子部门） |

### 7.3 DataScopeEnum — 数据权限级别

| 枚举值 | code | 权限范围 | 说明 |
|--------|------|---------|------|
| `ALL` | 1 | 全部数据 | 仅受租户隔离约束 |
| `DEPT_AND_CHILD` | 2 | 部门及子部门 | departmentIds已在登录时展开 |
| `DEPT` | 3 | 本部门 | 仅能查看所属部门数据 |
| `SELF` | 4 | 仅本人 | 仅能查看自己创建的数据 |

**常用判断**：
```java
DataScopeEnum scope = user.getDataScope();
scope.isAll();                  // 是否全部权限
scope.isDeptScope();            // 是否需要部门过滤
scope.isSelf();                 // 是否仅本人
scope.greaterThanOrEqual(other); // 当前权限是否≥指定权限
```

### 7.4 服务端核心接口（完整 API）

#### LoginManager — 登录管理器（需自行实现）

```java
// 实现示例：密码登录
@Component
public class PwdLoginManager extends AbsLoginManager<PwdLoginRequest> {
    @Override
    protected AbsUserDetails doLogin(PwdLoginRequest request, ClientDetails clientDetails) {
        SysUser user = userService.findByUsername(request.getUsername());
        AssertUtils.notEmpty(user, "用户不存在");
        AssertUtils.isTrue(passwordEncoder.matches(request.getPassword(), user.getPassword()), "密码错误");
        return user;
    }
}
```

**LoginManager 接口方法**：

| 方法 | 签名 | 说明 |
|------|------|------|
| `support` | `boolean support(Object adapter)` | 判断是否支持该登录类型，如 `return adapter instanceof PwdLoginRequest` |
| `login` | `AbsUserDetails login(ClientDetails clientDetails, Object adapter)` | 执行登录认证，返回用户详情 |

**AbsLoginManager\<T\> 抽象类**：
- 继承后只需重写 `doLogin(T request, ClientDetails clientDetails)` 方法
- 框架自动处理参数校验、Token生成、事件发布等
- 泛型 `T` 为登录请求类型

#### LoginService — 登录服务

```java
@Autowired
private LoginService loginService;

// 用户登录
Map<String, String> tokens = loginService.login(
    new PwdLoginRequest("admin", "123456"),  // adapter: 登录请求对象
    LoginTypeAdapter.PASSWORD                // loginType: 登录类型枚举
);
// 返回：{accessToken, refreshToken, expiresIn, userId, ...}

// 刷新Token
Map<String, String> newTokens = loginService.refresh(refreshTokenStr);

// 登出（指定用户）
loginService.logout("userId123");

// 登出（当前用户）
loginService.logout();
```

**完整 API**：
```java
Map<String, String> login(Object adapter, LoginTypeAdapter loginType);
Map<String, String> refresh(String refreshTokenStr);
void logout(String userId);
void logout();
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `adapter` | `Object` | 登录请求对象，如 `PwdLoginRequest`、`SmsLoginRequest` |
| `loginType` | `LoginTypeAdapter` | 登录类型枚举，路由到对应 LoginManager |
| `refreshTokenStr` | `String` | 刷新Token字符串 |

**返回值 `Map<String, String>` 包含的key**：`accessToken`、`refreshToken`、`expiresIn`、`userId`、`username` 等

#### LoginTypeAdapter — 登录类型枚举

```java
// 框架内置类型（可在业务项目中扩展）
LoginTypeAdapter.PASSWORD    // 密码登录
LoginTypeAdapter.SMS         // 短信登录
LoginTypeAdapter.WECHAT      // 微信登录
```

**接口方法**：
```java
Class<? extends LoginManager> getAClass();  // 返回对应的LoginManager实现类
LoginManager getLoginManager();              // 通过Spring容器获取LoginManager实例
```

#### ClientManager — 客户端管理器

```java
@Autowired
private ClientManager clientManager;

// 加密客户端凭证为Token
String token = clientManager.encrypt("my-client-id", "my-client-secret");

// 解密Authorization头获取凭证
String authHeader = request.getHeader("Authorization");  // "Basic base64(...)"
Map<ClientAttribute, String> credentials = clientManager.decryptStr(authHeader);
String clientId = credentials.get(ClientAttribute.CLIENT_ID);
String clientSecret = credentials.get(ClientAttribute.CLIENT_SECRET);
```

**完整 API**：
```java
String encrypt(String clientId, String clientSecret);
Map<ClientAttribute, String> decryptStr(String header);
```

#### ClientDetailsService — 客户端详情服务（需继承 AbsClientDetailsService）

```java
// 实现示例
@Service
public class MyClientDetailsService extends AbsClientDetailsService {
    @Override
    protected ClientDetails getClientDetailsFromDB(String clientId) {
        SysClientDetails entity = clientRepository.findByClientId(clientId);
        AssertUtils.notEmpty(entity, "客户端不存在");
        return convertToClientDetails(entity);
    }
}

// 调用
ClientDetails client = clientDetailsService.getClientDetails(authHeader);
String clientToken = clientDetailsService.getClientToken("client-id", "client-secret");
```

**完整 API**：
```java
ClientDetails getClientDetails(String header);                            // 从Authorization头获取
ClientDetails getClientDetails(HttpServletRequest request);               // 从HttpServletRequest获取
String getClientToken(String clientId, String clientSecret);              // 生成客户端Token
```

#### PermissionManageService — 权限管理服务

```java
@Autowired
private PermissionManageService permissionManageService;

// 新增权限（按项目+角色，自动发布事件）
Map<String, String> perms = new HashMap<>();
perms.put("user:create", "创建用户");
perms.put("user:delete", "删除用户");
permissionManageService.addPermissions("xiaoyue-web", "admin", perms);

// 新增权限（不发布事件，用于登录预缓存）
permissionManageService.addPermissions("xiaoyue-web", "admin", perms, false);

// 全量替换角色权限
permissionManageService.updateRolePermission("xiaoyue-web", "editor", newPerms);

// 删除单个权限
permissionManageService.deletePermission("xiaoyue-web", "admin", "user:delete");

// 批量删除权限
permissionManageService.deletePermissions("xiaoyue-web", "admin", List.of("user:create", "user:delete"));
```

**完整 API**：
```java
void addPermissions(String projectCode, String roleKey, Map<String, String> permissions);
void addPermissions(String projectCode, String roleKey, Map<String, String> permissions, boolean publishEvent);
void updateRolePermission(String projectCode, String roleKey, Map<String, String> permissions);
void deletePermission(String projectCode, String roleKey, String permissionKey);
void deletePermissions(String projectCode, String roleKey, List<String> permissionKeys);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `projectCode` | `String` | 项目编码（clientName），如 `"xiaoyue-web"` |
| `roleKey` | `String` | 角色标识，如 `"admin"` |
| `permissions` | `Map<String,String>` | key=权限标识, value=权限描述 |
| `publishEvent` | `boolean` | 是否发布事件通知客户端，`true`=广播, `false`=仅存Redis |

#### UnifiedSecretManager — 统一密钥管理器（需自行实现）

```java
// 实现示例
@Component
public class DatabaseUnifiedSecretManager implements UnifiedSecretManager {
    @Override
    public Map<String, String> getSecrets(String projectCode) {
        ProjectClient client = repository.findByProjectCode(projectCode);
        if (client == null) {
            client = createDefaultClient(projectCode);
        }
        Map<String, String> secrets = new HashMap<>();
        secrets.put("jwt", client.getJwtSecret());   // JWT签名密钥
        secrets.put("sign", client.getSignSecret());  // API签名密钥
        return secrets;
    }
}
```

**接口方法**：
```java
Map<String, String> getSecrets(String projectCode);
// 返回：{"jwt": "64位随机字符串", "sign": "32位随机字符串"}
```

#### LoginUserOperationManager — 登录用户操作管理器

```java
@Autowired
private LoginUserOperationManager loginUserOperationManager;

// 登录时保存用户信息
loginUserOperationManager.saveUserInfo(tokenData, true);  // isLogin=true登录, false=刷新

// 退出登录（清除所有登录信息）
loginUserOperationManager.loginOut("userId123");

// 退出登录（仅退出当前设备）
loginUserOperationManager.loginOut("userId123", "jti-xxx");

// 退出登录（当前用户）
loginUserOperationManager.loginOut();
```

### 7.5 客户端核心接口（完整 API）

#### TokenManager — Token 管理器

```java
@Autowired
private TokenManager tokenManager;

// 创建Token
Map<String, Object> headers = Map.of("alg", "HS256", "typ", "JWT");
Map<String, Object> payload = Map.of("userId", "123", "username", "张三");
String token = tokenManager.create(headers, payload);

// 验证Token并解析载荷
Map<String, Object> payload = tokenManager.verify(token, isRefreshToken);
// isRefreshToken: false=AccessToken, true=RefreshToken
```

**完整 API**：
```java
String create(Map<String, Object> headers, Map<String, Object> payload);
Map<String, Object> verify(String token, boolean isRefreshToken);
```

#### LoginInfoManager — 登录用户信息管理器

```java
@Autowired
private LoginInfoManager loginInfoManager;

// 获取用户信息（按jti或userId）
Map<Object, Object> userInfo = loginInfoManager.getUserInfo("jti-xxx");

// 获取权限（按角色集合）
Set<String> roles = Set.of("admin", "editor");
Map<Object, Map<Object, Object>> authorities = loginInfoManager.getAuthorities(roles);

// 获取权限（按角色+项目维度）
Map<Object, Map<Object, Object>> perms = loginInfoManager.getAuthoritiesByProjectCode(roles, "xiaoyue-web");
```

#### SignManager — 签名管理器

```java
@Autowired
private SignManager signManager;

// 校验时效性（默认5分钟窗口）
signManager.checkTimestamp("1680000000000");

// 防重放校验
signManager.checkNonce("random-uuid-string");

// 生成签名
String signature = signManager.signWeb(message);

// 验证签名
boolean valid = signManager.verifyWeb(message, signature);
```

**完整 API**：
```java
void checkTimestamp(String timestamp);
void checkNonce(String nonce);
String signWeb(String message);
boolean verifyWeb(String message, String signature);
```

#### CsrfService — CSRF防御服务

```java
@Autowired
private CsrfService csrfService;

// 保存CSRF Token
csrfService.saveToken("userId123", "/order/submit", "csrf-token-value");

// 校验CSRF Token（consume=true一次性使用，false可复用）
csrfService.checkToken("userId123", "/order/submit", "csrf-token-value", true);
```

**完整 API**：
```java
void saveToken(String userId, String path, String token);
void checkToken(String userId, String path, String token, boolean consume);
```

#### PermissionAutoLoader — 权限自动加载器（SPI，需自行实现）

```java
@Component
public class SysRoleMenuPermissionLoader implements PermissionAutoLoader {
    @Autowired
    private SysRoleMenuView sysRoleMenuView;

    @Override
    public Map<String, String> loadPermissions(String roleKey, String projectCode) {
        List<String> perms = sysRoleMenuView.findPermsByRoleKeyAndProjectCode(roleKey, projectCode);
        if (perms == null || perms.isEmpty()) {
            return Map.of();
        }
        return perms.stream()
            .filter(p -> p != null && !p.isEmpty())
            .collect(Collectors.toMap(p -> p, p -> ""));
    }
}
```

**接口方法**：
```java
Map<String, String> loadPermissions(String roleKey, String projectCode);
// 返回：key=权限标识, value=权限描述，不可返回null
```

#### JwtUtils — JWT工具类（静态方法）

```java
// 生成新密钥
String secret = JwtUtils.createJWTSignerStr();  // 64位随机字符串

// 保存密钥
JwtUtils.saveSecret(secret);

// 获取签名器
JWTSigner signer = JwtUtils.getJWTSigner();

// 创建JWT
String token = JwtUtils.create(headers, payload);

// 验证JWT
JWT jwt = JwtUtils.verify(token);
```

**完整 API**：
```java
static String createJWTSignerStr();                              // 生成64位随机密钥
static void saveSecret(String JWTSigner);                        // 保存密钥
static JWTSigner getJWTSigner();                                 // 获取签名器
static String create(Map<String, Object> headers, Map<String, Object> payload);  // 创建JWT
static JWT verify(String token);                                 // 验证JWT
```

### 7.6 Auth 事件体系

| 事件类 | 说明 | 触发时机 |
|--------|------|---------|
| `PermissionChangeEvent` | 权限变更事件 | 管理员修改角色权限时 |
| `SecretEvent` | 密钥变更事件 | 密钥轮换时 |
| `SecretBroadcaster` | 密钥广播器 | 服务端向客户端广播新密钥 |

**客户端监听权限变更**：
```java
@Component
public class PermissionChangeEventHandler {
    @EventHandler("permissionChange")
    public void handle(PermissionChangeEvent event) {
        // 框架自动刷新本地权限缓存
        cacheManager.clearPermissionCache(event.getProjectCode());
    }
}
```

### 7.7 Auth POM依赖

```xml
<!-- 客户端（业务服务引入） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-auth-client</artifactId>
</dependency>

<!-- 服务端（认证服务引入） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-auth-server</artifactId>
</dependency>
```

---

## 8. Logs 日志系统

**Maven**: `simple-common-logs-client` / `simple-common-logs-server`
**架构**: TCP + Protobuf 高性能日志收集
**包路径**: `com.simple.common.logs`

### 8.1 架构概览

| 子模块 | 核心类 | 说明 |
|--------|--------|------|
| `logs-proto` | `LogDataEvent` | Protobuf协议定义，日志数据结构 |
| `logs-client` | `LogService` / `BufferedLogManager` / `LogProtobufTcpClient` | 日志发送客户端，自动拦截Controller请求 |
| `logs-server` | `LogProtobufTcpServer` / `AbsLogsSaveManager` | 日志接收+存储服务端 |

### 8.2 LogService — 日志服务接口

```java
@Autowired
private LogService logService;

// 发送日志（通常由拦截器自动调用，无需手动调用）
logService.send(request, response, handler, exception);

// 启动日志客户端（应用启动时自动调用）
logService.start();

// 停止日志客户端（应用关闭时自动调用）
logService.stop();
```

**完整 API**：
```java
void send(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex);
void start();
void stop();
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `request` | `HttpServletRequest` | HTTP 请求对象 |
| `response` | `HttpServletResponse` | HTTP 响应对象 |
| `handler` | `Object` | 处理器对象（Controller方法） |
| `ex` | `Exception` | 异常对象，无异常时传 null |

**自动拦截机制**：引入 `simple-common-logs-client` 后，框架自动通过 `LogFilter` 和 `LogInterceptor` 拦截所有 Controller 请求，记录操作日志并通过 TCP 发送到日志服务器，无需手动编码。

### 8.3 LogManager — 日志发送管理器

```java
@Autowired
private LogManager logManager;

// 构建日志数据
LogDataEvent logData = LogDataEvent.newBuilder()
    .setUserId("123")
    .setUserName("张三")
    .setOperation("创建订单")
    .setModule("订单管理")
    .setIp("192.168.1.100")
    .setCreateTime(System.currentTimeMillis())
    .build();

// 发送日志
boolean success = logManager.send(logData);
```

**完整 API**：
```java
boolean send(LogDataEvent logData);
void start();
void stop();
```

### 8.4 BufferedLogManager — 缓冲日志管理器（默认实现）

继承 `LogManager`，提供增强特性：

| 特性 | 说明 |
|------|------|
| 内存缓冲队列 | 后台线程批量发送，减少网络和编解码开销 |
| 失败降级 | 发送失败时自动写入本地文件，连接恢复后补发 |
| 优雅停机 | 应用关闭时刷新缓冲区，确保数据不丢 |
| 魔数校验 | 降级文件采用魔数（0xCAFEBABE）校验，损坏自动恢复 |

### 8.5 LogUserManager — 日志用户信息管理器（需自行实现）

```java
// 默认实现返回 null，需继承并重写
@Component
public class MyLogUserManager extends DefaultLogUserManager {
    @Override
    public String loginNickName() {
        UserTemporary user = LoginUserUtils.getUserTemporary();
        return user != null ? user.getNickname() : "anonymous";
    }

    @Override
    public String loginUserId() {
        UserTemporary user = LoginUserUtils.getUserTemporary();
        return user != null ? user.getUserId() : "unknown";
    }
}
```

**完整 API**：
```java
String loginNickName();   // 返回当前操作用户昵称
String loginUserId();     // 返回当前操作用户ID
```

### 8.6 LogProtobufTcpClient — TCP 日志客户端

```java
// 连接日志服务器
logProtobufTcpClient.connect("192.168.1.100", 9090);

// 发送日志数据
logProtobufTcpClient.send(logData);

// 添加发送失败监听器
logProtobufTcpClient.addSendFailureListener(new SendFailureListener() {
    @Override
    public void onSendFailure(LogDataEvent logData) {
        // 降级处理：写入本地文件
        fallbackWriter.write(logData);
    }
});
```

### 8.7 LogDataEvent — Protobuf 日志事件

**构建日志数据**：
```java
LogDataEvent logData = LogDataEvent.newBuilder()
    .setUserId("123")                    // 用户ID
    .setUserName("张三")                 // 用户名
    .setOperation("创建订单")            // 操作描述
    .setModule("订单管理")               // 操作模块
    .setIp("192.168.1.100")             // 客户端IP
    .setUrl("/api/order/create")        // 请求URL
    .setMethod("POST")                   // 请求方法
    .setParams("{\"amount\":100}")       // 请求参数JSON
    .setResult("success")                // 操作结果
    .setCostTime(150L)                   // 耗时（毫秒）
    .setCreateTime(System.currentTimeMillis())
    .build();
```

### 8.8 服务端 — AbsLogsSaveManager（需自行实现）

```java
@Component
public class MyLogsSaveManager extends AbsLogsSaveManager {
    @Override
    protected void save(List<LogDataEvent> logDataList) {
        // 批量保存日志到数据库
        logRepository.saveBatch(logDataList);
    }
}
```

### 8.9 Logs POM依赖

```xml
<!-- 客户端（业务服务引入） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-logs-client</artifactId>
</dependency>

<!-- 服务端（日志收集服务引入） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-logs-server</artifactId>
</dependency>
```

---

## 9. WebSocket 通信

**Maven**: `simple-common-websocket`
**基于**: Netty
**包路径**: `com.simple.common.websocket`

### 9.1 @WebSocketListening — 消息分发

```java
@Component
public class ChatWebSocketHandler {

    @WebSocketListening(type = "chat", cliKey = "default")
    public void handleChat(ChatMessage message, ChannelHandlerContext ctx) {
        // type="chat": socket类型，区分不同通讯
        // cliKey="default": 区分同类型下不同端的数据
        String reply = chatService.process(message);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(reply));
    }

    @WebSocketListening(type = "notification", cliKey = "mobile")
    public void handleNotification(NotificationMessage message, ChannelHandlerContext ctx) {
        notificationService.push(message);
    }
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type()` | `String` | 必填 | socket类型，一个类型在同端只能有一个数据通道 |
| `cliKey()` | `String` | `"default"` | 区分相同类型下不同端的数据 |

### 9.2 ChannelMap\<K, V, T\> — 多级通道存储

**泛型参数**：`K`=主键类型，`V`=子键类型，`T`=值类型（通常是 `Channel`）

```java
ChannelMap<String, String, Channel> channelMap = new ChannelMap<>();

// 添加通道
channelMap.add("userId:123", "mobile", channel);    // K=user, V=设备, T=Channel

// 获取用户所有通道（多端登录）
List<Channel> channels = channelMap.get("userId:123");

// 获取指定设备的通道
Channel channel = channelMap.get("userId:123", "mobile");

// 删除指定设备的通道
channelMap.del("userId:123", "mobile");

// 删除用户所有通道
channelMap.del("userId:123");

// 清理/统计
channelMap.clear();
int size = channelMap.size();   // 总通道数
int keySize = channelMap.keySize();  // 总用户数
```

### 9.3 WebSocket POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-websocket</artifactId>
</dependency>
```

---

## 10. Excel 导入导出

**Maven**: `simple-common-excel`
**包路径**: `com.simple.common.excel`

### 10.1 EasyExcelWriteService — 写入/导出（推荐）

```java
@Autowired
private EasyExcelWriteService writeService;

// 导出到浏览器下载（最常用）
@GetMapping("/export/users")
public void exportUsers(HttpServletResponse response) {
    List<UserExportDTO> list = userService.findAllForExport();
    writeService.writeResponse(UserExportDTO.class, list, "用户列表");
    // 浏览器下载 "用户列表.xlsx"
}

// 导出到输出流（用于上传OSS/邮件附件等）
ByteArrayOutputStream os = writeService.writeOutputStream(UserExportDTO.class, list);
ByteArrayInputStream is = writeService.writeInputStream(UserExportDTO.class, list);
```

**完整 API**：
```java
<T> ByteArrayOutputStream writeOutputStream(ByteArrayOutputStream outputStream, Class<T> clazz, List<T> data);
<T> void writeResponse(Class<T> clazz, List<T> data, String writeName);        // writeName不含扩展名
<T> ByteArrayOutputStream writeOutputStream(Class<T> clazz, List<T> data);     // 自动创建输出流
<T> ByteArrayInputStream writeInputStream(Class<T> clazz, List<T> data);       // 返回输入流
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `clazz` | `Class<T>` | 数据实体类，需用 `@ExcelProperty` 标注列 |
| `data` | `List<T>` | 待导出的数据集合 |
| `writeName` | `String` | 下载文件名（不含 `.xlsx` 扩展名，自动添加） |
| `outputStream` | `ByteArrayOutputStream` | 目标输出流，为null则自动创建 |

### 10.2 EasyExcelReadService — 读取/导入

```java
@Autowired
private EasyExcelReadService readService;

// 从MultipartFile读取（推荐，Web上传场景）
@PostMapping("/import/users")
public R<?> importUsers(@RequestParam("file") MultipartFile file) {
    List<UserImportDTO> list = new ArrayList<>();
    readService.read(file, 1, UserImportDTO.class, new ReadListener<UserImportDTO>() {
        @Override
        public void invoke(UserImportDTO data, AnalysisContext context) {
            list.add(data);
        }
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            userService.saveBatch(list);
        }
    });
    return R.ok("导入成功，共" + list.size() + "条");
}

// 从文件路径读取
readService.read("/data/import/users.xlsx", 1, UserImportDTO.class, listener);

// 从输入流读取
readService.read(inputStream, 1, OrderImportDTO.class, listener);
```

**完整 API**：
```java
<T> void read(String filePath, int headRowNumber, Class<T> head, ReadListener<T> readListener);
<T> void read(InputStream inputStream, int headRowNumber, Class<T> head, ReadListener<T> readListener);
<T> void read(MultipartFile file, int headRowNumber, Class<T> head, ReadListener<T> readListener);  // default方法
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `headRowNumber` | `int` | 表头行数，如1=第1行表头，从第2行读数据 |
| `head` | `Class<T>` | 数据实体类，属性顺序需与Excel列顺序一致（也可传 `Map.class` 动态映射） |
| `readListener` | `ReadListener<T>` | 数据读取监听器，逐行回调 `invoke()` |

### 10.3 PoiWriteService — POI复杂导出

**适用场景**：自定义列宽、合并单元格、分页导出等复杂需求。

```java
@Autowired
private PoiWriteService poiWriteService;

@GetMapping("/export/report")
public void exportReport(HttpServletResponse response) {
    List<ReportData> list = reportService.findAll();
    String[] headers = {"用户ID", "姓名", "年龄", "部门"};
    Integer[] widths = {20, 30, 15, 25};

    poiWriteService.exportResponse(
        (rowNum, data) -> new Object[]{data.getId(), data.getName(), data.getAge(), data.getDept()},
        list,
        headers,
        widths,
        1000,           // 每页1000行（每个Sheet最大行数，不超过1048576）
        "用户报表"       // 文件名
    );
}
```

**完整 API**：
```java
<T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num, ByteArrayOutputStream outputStream);
<T> void exportResponse(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num, String excelName);
<T> ByteArrayOutputStream writeOutputStream(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num);  // 自动创建输出流
<T> ByteArrayInputStream writeInputStream(PoiExportFunction<T> function, List<T> list,
    String[] head, Integer[] width, Integer num);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `function` | `PoiExportFunction<T>` | 数据填充函数 `(rowNum, data) -> Object[]` |
| `head` | `String[]` | 表头名称数组 |
| `width` | `Integer[]` | 列宽数组（字符单位） |
| `num` | `Integer` | 每个Sheet最大行数，不超过1048576 |
| `excelName` | `String` | 下载文件名（不含扩展名） |

### 10.4 Excel POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-excel</artifactId>
</dependency>
```

---

## 11. SMS 短信服务

**Maven**: `simple-common-sms`
**包路径**: `com.simple.common.sms`
**默认实现**: `AliSmsService`（阿里云短信）

### 11.1 SmsService API

```java
@Autowired
private SmsService smsService;

// 发送验证码
smsService.sendCode("13800138000", "123456", "LOGIN");
// 参数：mobile=手机号, code=验证码, sendType=短信类型配置标识

// 发送模板短信（自动获取客户端IP）
smsService.sendTemplateParam("13800138000", "ORDER_NOTIFY", "{\"orderNo\":\"20240101001\"}");
// 参数：mobile=手机号, sendType=类型标识, templateParam=模板参数JSON

// 发送模板短信（手动指定IP）
smsService.sendTemplateParam("13800138000", "ORDER_NOTIFY", "{\"orderNo\":\"20240101001\"}", "192.168.1.1");

// 校验验证码
smsService.checkSms("13800138000", "123456", "LOGIN");
// 校验失败直接抛出异常
```

| 方法 | 参数 | 说明 |
|------|------|------|
| `sendCode` | `String mobile, String code, String sendType` | 发送验证码 |
| `sendTemplateParam` | `String mobile, String sendType, String templateParam, String ip` | 发送模板短信（指定IP） |
| `sendTemplateParam` | `String mobile, String sendType, String templateParam` | 发送模板短信（自动获取IP） |
| `checkSms` | `String mobile, String code, String sendType` | 校验验证码 |

### 11.2 SMS POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-sms</artifactId>
</dependency>
```

---

## 12. Annex 附件管理

**Maven**: `simple-common-annex`
**包路径**: `com.simple.common.annex`
**存储后端**: S3协议（支持 MinIO / 阿里云OSS / AWS S3）

### 12.1 AnnexService API

```java
@Autowired
private AnnexService annexService;

// ==== 上传 ====

// 上传MultipartFile（基础版，使用默认包路径）
UploadResponse resp = annexService.upload(file, "user-service", ShareType.PUBLIC);

// 上传MultipartFile（带包名，按业务模块分类）
UploadResponse resp = annexService.upload(avatarFile, "user-service", "avatars", ShareType.PUBLIC);
// 文件路径: user-service/avatars/2024/01/15/avatar_xxx.jpg

// 通过输入流上传（从URL下载后上传等场景）
InputStream is = url.openStream();
UploadResponse resp = annexService.upload(
    "avatar.jpg",            // fileName: 含扩展名
    "user-service",          // applicationName: 应用名
    "avatars",               // packageName: 业务包名（可为null）
    ShareType.PUBLIC,        // shareType: PUBLIC公开/PRIVATE私有
    is,                      // inputStream: 文件流
    existingUrl -> existingUrl == null  // UploadFunction: 是否需要重新上传，null=总是上传
);

// 简化版输入流上传（不传UploadFunction，总是上传）
UploadResponse resp = annexService.upload("report.pdf", "report-service", ShareType.PRIVATE, inputStream);

// ==== 下载 ====

// 下载到浏览器（Controller中使用）
@GetMapping("/download/{objectKey}")
public void download(@PathVariable String objectKey, HttpServletResponse response) {
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=\"file.pdf\"");
    annexService.writeGetObjectResponse(objectKey);
}

// ==== 生成临时访问URL ====
String url = annexService.generateUrl(objectKey);
// 返回带签名的临时URL，过期后无法访问（有效期由配置决定）

// ==== 删除 ====
annexService.delete(objectKey);
// 删除不可恢复，建议业务层先软删除再定时清理
```

### 12.2 UploadResponse 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | `String` | 文件名 |
| `totalSize` | `long` | 文件大小（字节） |
| `algorithmValue` | `String` | 摘要算法值（验证文件一致性） |
| `algorithmType` | `Algorithm` | 摘要算法类型 |
| `suffix` | `String` | 文件扩展名（不带点） |
| `saveUrl` | `String` | 文件完整URL（objectKey） |
| `shareType` | `ShareType` | 附件权限类型 |
| `applicationName` | `String` | 系统名称 |
| `isTrue` | `Boolean` | 有判断方法时返回，表示文件是否存在 |
| `extension` | `String` | 扩展字段 |

### 12.3 ShareType 枚举

| 枚举 | code | 说明 |
|------|------|------|
| `PUBLIC` | 1 | 公开访问（无需签名） |
| `PRIVATE` | 2 | 私有访问（需签名URL） |

### 12.4 Annex POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-annex</artifactId>
</dependency>
```

---

## 13. Doc 文档模板替换

**Maven**: `simple-common-doc`
**包路径**: `com.simple.common.doc`
**基于**: Poi-Tl（Word .docx 模板填充）

### 13.1 模板语法

| 占位符格式 | 用途 | 示例 |
|-----------|------|------|
| `{{code}}` | 普通文本 | `{{username}}` |
| `{{@code}}` | 图片 | `{{@seal}}` |
| `{{#code}}` | 表格 | `{{#orderTable}}` |
| `{{*code}}` | 列表 | `{{*itemList}}` |

### 13.2 Docs Builder — 构建模板数据

```java
Map<String, Object> data = Docs.builder()
    // 普通文本
    .addStr("contractNo", "HT-2024-001")
    // 带链接文本
    .addStrLink("companyName", "XX科技有限公司", "https://www.example.com")
    // 带颜色文本
    .addStrColor("warnText", "请仔细阅读", "#FF0000")
    // 本地图片
    .addImgLocal("seal", "/templates/seal.png", 100, 100)
    // 图片流（数据库存储的图片）
    .addImgInputStream("sign", inputStream, 80, 80)
    // 网络图片
    .addImgInputUrl("logo", "https://cdn.example.com/logo.png", 120, 60)
    // 表格
    .addTable("orderTable", "90%",
        new String[]{"序号", "商品", "数量", "单价", "金额"},
        orderItems,
        (item) -> new String[]{item.getSeq(), item.getName(), item.getQty(), item.getPrice(), item.getAmount()}
    )
    // 表格（指定字体大小）
    .addTable("detailTable", "100%", 12,
        new String[]{"名称", "规格", "备注"},
        details,
        (d) -> new String[]{d.getName(), d.getSpec(), d.getRemark()}
    )
    // 列表（默认圆点）
    .addList("terms", Arrays.asList("条款一：xxx", "条款二：yyy", "条款三：zzz"))
    // 列表（指定编号格式）
    .addList("steps", NumberingFormat.DECIMAL, Arrays.asList("第一步", "第二步", "第三步"))
    .create();  // 返回 Map<String, Object>
```

**Docs Builder API**：

| 方法 | 模板格式 | 参数 |
|------|---------|------|
| `addStr(key, value)` | `{{key}}` | key=模板key, value=文本内容 |
| `addStrLink(key, value, link)` | `{{key}}` | 带超链接的文本 |
| `addStrColor(key, value, color)` | `{{key}}` | 带颜色的文本，color=如"#FF0000" |
| `addImgLocal(key, url, width, height)` | `{{@key}}` | 本地图片路径 |
| `addImgInputStream(key, is, width, height)` | `{{@key}}` | 图片输入流 |
| `addImgInputUrl(key, url, width, height)` | `{{@key}}` | 网络图片URL |
| `addTable(key, percentWidth, head, list, function)` | `{{#key}}` | 表格（默认字体15） |
| `addTable(key, percentWidth, size, head, list, function)` | `{{#key}}` | 表格（指定字体大小） |
| `addList(key, list)` | `{{*key}}` | 列表（默认圆点） |
| `addList(key, format, list)` | `{{*key}}` | 列表（指定格式） |

### 13.3 DocReplaceService — 替换并输出

```java
@Autowired
private DocReplaceService docReplaceService;

// ==== 最常用：从resources模板生成 + 浏览器下载 ====
@GetMapping("/contract/download/{orderId}")
public void downloadContract(@PathVariable String orderId, HttpServletResponse response) {
    Order order = orderService.findById(orderId);
    Map<String, Object> data = Docs.builder()
        .addStr("orderNo", order.getOrderNo())
        .addStr("customerName", order.getCustomerName())
        .addStr("amount", order.getAmount().toString())
        .create();

    // 一行完成：加载模板 → 替换 → 写入HTTP响应
    docReplaceService.replaceResponse("合同_" + order.getOrderNo(),
                                       "/templates/contract.docx", data);
}

// ==== 从流加载模板 + 浏览器下载 ====
try (InputStream templateStream = getClass().getResourceAsStream("/templates/contract.docx")) {
    docReplaceService.replaceResponse("合同_2024001", templateStream, data);
}

// ==== 获取输出流（上传OSS等场景） ====
ByteArrayOutputStream os = docReplaceService.replaceAndGetOutputStream("/templates/report.docx", data);
ByteArrayInputStream is = docReplaceService.replaceAndGetInputStream("/templates/report.docx", data);

// ==== 基础方法：指定输入输出流 ====
docReplaceService.replace(inputStream, outputStream, data);
```

**完整 API**：
```java
void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);
void replaceResponse(String name, InputStream inputStream, Map<String, Object> values);       // 流模板+下载
void replaceResponse(String name, String templatePath, Map<String, Object> values);           // resources模板+下载
ByteArrayInputStream replaceAndGetInputStream(String templatePath, Map<String, Object> values);
ByteArrayOutputStream replaceAndGetOutputStream(String templatePath, Map<String, Object> values);
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `inputStream` | `InputStream` | 模板文件输入流 |
| `outputStream` | `OutputStream` | 替换后文档输出流 |
| `values` | `Map<String, Object>` | 模板参数（通过 `Docs.builder().create()` 构建） |
| `name` | `String` | 下载文件名（不含 `.docx` 扩展名） |
| `templatePath` | `String` | resources下模板路径，如 `"/templates/contract.docx"` |

### 13.4 Doc POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-doc</artifactId>
</dependency>
```

---

## 14. XxlJob 分布式任务调度

**Maven**: `simple-common-xxljob`
**包路径**: `com.simple.common.xxljob`

### 14.1 XxlJobManager API

```java
@Autowired
private XxlJobManager xxlJobManager;

// 创建任务
CreateXxlJobTaskRequest req = new CreateXxlJobTaskRequest()
    .setJobGroup(1)                                     // 执行器ID
    .setJobDesc("订单超时取消任务")                       // 任务描述
    .setAuthor("system")                                 // 负责人
    .setScheduleConf("0 0/5 * * * ?")                    // Cron表达式
    .setExecutorHandler("orderTimeoutHandler")            // JobHandler名称
    .setExecutorParam("{\"timeoutMinutes\":30}")          // 任务参数JSON
    .setExecutorTimeout(30)                               // 超时秒数
    .setExecutorFailRetryCount(3);                        // 失败重试次数
String jobId = xxlJobManager.create(req);

// 启动任务
xxlJobManager.start(Integer.parseInt(jobId));

// 立即触发一次（测试用）
xxlJobManager.trigger(Integer.parseInt(jobId));

// 修改任务
UpdateXxlJobTaskRequest updateReq = new UpdateXxlJobTaskRequest()
    .setId(123)
    .setScheduleConf("0 0/10 * * * ?")  // 改为每10分钟
    .setJobDesc("订单超时取消任务(已优化)");
xxlJobManager.update(updateReq);

// 停止/删除
xxlJobManager.end(jobId);    // 停止
xxlJobManager.delete(jobId); // 删除
```

### 14.2 CreateXxlJobTaskRequest 字段

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `jobGroup` | `Integer` | ✅ | - | 执行器主键ID |
| `scheduleConf` | `String` | ✅ | - | 调度配置（Cron表达式） |
| `executorHandler` | `String` | ✅ | - | JobHandler名称，需和 `@XxlJob` 注解一致 |
| `author` | `String` | ✅ | - | 负责人 |
| `jobDesc` | `String` | ✅ | - | 任务描述 |
| `scheduleType` | `String` | ❌ | `"CRON"` | 调度类型 |
| `glueType` | `String` | ❌ | `"BEAN"` | 运行模式 |
| `executorRouteStrategy` | `String` | ❌ | `"FIRST"` | 路由策略 |
| `misfireStrategy` | `String` | ❌ | `"DO_NOTHING"` | 调度过期策略 |
| `executorBlockStrategy` | `String` | ❌ | `"SERIAL_EXECUTION"` | 阻塞处理策略 |
| `alarmEmail` | `String` | ❌ | - | 报警邮件 |
| `executorParam` | `String` | ❌ | - | 任务参数（JSON格式） |
| `childJobId` | `String` | ❌ | - | 子任务ID |
| `executorTimeout` | `int` | ❌ | `30` | 超时时间（秒） |
| `executorFailRetryCount` | `int` | ❌ | `3` | 失败重试次数 |

### 14.3 UpdateXxlJobTaskRequest

继承 `CreateXxlJobTaskRequest`，额外字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `Integer` | ✅ | 任务主键ID |

### 14.4 XxlJob POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-xxljob</artifactId>
</dependency>
```

---

## 15. AI 大模型集成

**Maven**: `simple-common-ai`
**包路径**: `com.simple.common.ai`
**后端**: 阿里云百炼（DashScope）

### 15.1 AiAliBuilder\<T\> API

```java
// 发送对话消息
AiAliBuilder<MyResponse>.AiResult result = AiAliBuilder.<MyResponse>builder()
    .sendMsg("session-001", "请帮我分析这份数据...");

// 获取AI回复文本
String aiReply = result.getMsg();

// 判断返回是否为结构化JSON
boolean isJson = result.isStructured();

// 将JSON回复转为Java对象
if (result.isStructured()) {
    MyResponse parsed = result.toObj(MyResponse.class);
}
```

**配置类 `AiAliProperties`**（需要配置）：
```yaml
simple:
  ai:
    ali:
      api-key: your-dashscope-api-key
      app-id: your-app-id
      enable-thinking: true   # 是否启用深度思考
```

### 15.2 AiResult 内部类

| 字段/方法 | 类型 | 说明 |
|-----------|------|------|
| `structured` | `boolean` | 返回内容是否为结构化JSON |
| `msg` | `String` | AI回复文本内容 |
| `toObj(Class<T>)` | `T` | 将回复JSON解析为指定类型对象 |

### 15.3 AI POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-ai</artifactId>
</dependency>
```

---

## 16. Alibaba 阿里组件

**Maven**: `simple-common-alibaba`
**包路径**: `com.come.on.alibaba`

### 16.1 UserRateLimitAspect — Sentinel两级限流

自动拦截所有 Controller 方法，按当前登录用户 `userId` 做两级限流：

| 级别 | 资源名 | 说明 |
|------|--------|------|
| 接口级 | `Controller类名:方法名` | 针对每个接口单独限流 |
| 全局级 | `user:rate:limit` | 用户总请求量限制 |

**注意**：POM中已包含 Sentinel 依赖，无需额外引入。

### 16.2 FeignConfig — Feign配置

统一提供 Feign 客户端的基础配置（请求拦截器、编码器等）。

### 16.3 Alibaba POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-alibaba</artifactId>
</dependency>
```

---

## 17. POM 依赖关系总览

```
simple-common (parent)
├── core          ← 所有模块的基础（R、断言、工具类、锁/线程/循环接口）
├── mp            ← 依赖core（分页、雪花ID、自动填充、数据权限）
├── redis         ← 依赖core（缓存注解、限流、分布式锁/闭锁/信号量）
├── cache         ← 依赖redis+core（Caffeine本地+Redis分布式两级缓存）
├── rabbitmq      ← 依赖core（消息发送、防重复消费）
├── eventbus      ← 依赖rabbitmq+core（注解驱动事件总线）
├── auth-client   ← 依赖core（权限/签名/CSRF注解、登录用户工具）
├── auth-server   ← 依赖core（登录管理、权限管理、秘钥管理）
├── logs-*        ← 独立（TCP+Protobuf日志收集三模块）
├── websocket     ← 依赖auth-client+core（Netty WebSocket）
├── excel         ← 依赖core（EasyExcel+POI 导入导出）
├── sms           ← 依赖core（阿里云短信）
├── annex         ← 依赖core（S3附件管理）
├── doc           ← 依赖core（Word模板替换）
├── xxljob        ← 依赖core（XXL-JOB任务管理）
├── ai            ← 依赖core（阿里云百炼AI）
├── alibaba       ← 依赖auth-client+core（Sentinel限流+Feign）
└── test          ← 测试模块
```

**引入规则**：只引入业务需要的模块，core 会自动传递引入。
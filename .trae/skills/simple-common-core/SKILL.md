---
name: "simple-common-core"
description: "框架核心基础模块。提供 R 统一响应、AssertUtils 断言、IdUtils 雪花ID/UUID、JsonUtils JSON转化、ThreadUtils 线程池/定时任务、BeanUtils/Base64Utils/IPUtils 工具类、CryptoUtil 加解密（AES/SM4/RSA/SM2/BCrypt）、LockService 分布式锁、CycleService 循环任务。所有模块的基础依赖。"
---

# simple-common-core 认知文档

**Maven**: `simple-common-core`
**包路径**: `com.simple.common.core`

## 1.1 统一响应 R\<T\>

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

## 1.2 AssertUtils 断言工具

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

## 1.3 DefaultException 自定义异常

```java
// 构造方法
new DefaultException("错误消息");                          // code="500"
new DefaultException("CUSTOM_CODE", "错误消息");            // 自定义code
new DefaultException("CUSTOM_CODE", "错误消息", data);      // 附带数据
new DefaultException(abstractException);                    // 从异常枚举构造
new DefaultException(abstractException, "重写消息");         // 异常枚举 + 重写消息
```

---

## 1.4 工具类完整 API

### IdUtils — 唯一ID生成

```java
IdUtils.getSnowflakeNextIdStr();      // 雪花ID（推荐，全局唯一递增）
IdUtils.getFastSimpleUUID();          // UUID去横线
IdUtils.getFastUUID();                // 标准UUID
IdUtils.randomNumbers();              // 9位随机数字
IdUtils.randomNumbers(6);             // 指定长度随机数字
```

### JsonUtils — JSON转化

```java
JsonUtils.toJsonStr(obj);              // 对象 → JSON字符串
JsonUtils.toJsonPrettyStr(obj);        // 对象 → 格式化JSON字符串
JsonUtils.toJsonObj(jsonStr, UserDTO.class);  // JSON字符串 → 对象
JsonUtils.toList(jsonStr, UserDTO.class);     // JSON数组字符串 → List<对象>
```

### ThreadUtils — 多线程工具

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

### BeanUtils — 对象工具

```java
// 对象转Map（驼峰key）
Map<String, Object> map = BeanUtils.toMap(user);

// Map填充到对象
SysUser user = BeanUtils.fillBeanWithMap(map, SysUser.class);

// copyProperties（继承自 hutool BeanUtil）
BeanUtils.copyProperties(source, target);
```

### SignUtils — 签名工具

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

### CryptoUtil — 加解密工具

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

### FileUtils — 文件工具

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

### Base64Utils

```java
String enc = Base64Utils.encode("文本");            // 文本/字节 → Base64字符串
byte[] dec = Base64Utils.decode("Base64字符串");     // Base64字符串 → 字节数组
String str = Base64Utils.decodeStr("Base64字符串");  // Base64字符串 → 原始字符串
```

### IPUtils

```java
String ip = IPUtils.getIpAddr();        // 获取当前请求客户端IP
String ip = IPUtils.getIntranetIp();    // 获取服务器内网IP
```

### ResponseUtils

```java
// Controller中直接写JSON到HTTP响应（用于下载场景）
ResponseUtils.writeResponse("文件名.docx", byteArrayOutputStream);
```

---

## 1.5 核心服务接口

### LockService — 分布式锁

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

### ThreadService — 线程池管理

```java
// 不直接使用，通过 ThreadUtils 静态方法调用（见1.4节）
// 如需自定义可注入实现
@Autowired
private ThreadService threadService;
```

### CycleService\<T\> — 循环任务

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

## 1.6 core POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-core</artifactId>
</dependency>
```

---

## 1.7 全模块 POM 依赖关系总览

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
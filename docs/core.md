# simple-common-core

核心工具模块，提供通用工具类、异常处理、响应封装等基础功能。

## 核心功能

| 功能分类 | 类/接口 | 说明 |
|---------|--------|------|
| 断言工具 | `AssertUtils` | 参数校验和异常抛出 |
| HTTP工具 | `HttpServletUtils` | HttpServletRequest 获取 |
| 加密工具 | `CryptoUtil` | 通过枚举自动切换加密方式（AES/SM4/RSA/SM2/国密） |
| Base64工具 | `Base64Utils` | Base64编码解码 |
| IP工具 | `IPUtils` | IP地址获取和处理 |
| JSON工具 | `JsonUtils` | JSON序列化/反序列化 |
| 响应封装 | `R<T>` | 统一响应格式 |
| 异常定义 | `DefaultException` | 默认业务异常 |
| 线程池工具 | `ThreadUtils` | 异步任务执行、延迟调度、定时任务 |
| 循环调度服务 | `CycleService` | 基于事件的循环任务调度（均匀/累加延迟） |

## 集成方式

### 步骤1：添加依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-core</artifactId>
    <version>${version}</version>
</dependency>
```

**重要说明：**
- core模块是所有其他模块的基础依赖，会自动引入
- 提供统一的异常处理、响应封装、工具类等基础功能
- CryptoUtil支持AES/SM4/RSA/SM2等国密算法，通过枚举切换

## 使用示例

### 1. 断言工具

```java
@Service
public class UserService {
    
    public User findById(String id) {
        // 参数校验
        AssertUtils.notBlank(id, "用户ID不能为空");
        
        User user = userMapper.selectById(id);
        AssertUtils.notNull(user, "用户不存在");
        
        return user;
    }
}
```

### 2. 加密工具（通过枚举自动切换加密方式）

CryptoUtil支持通过枚举参数自动切换加密算法，包括对称加密、非对称加密和哈希算法。

**支持的加密算法枚举：**

| 枚举类型 | 枚举值 | 说明 | 安全性 |
|---------|--------|------|--------|
| **对称加密** | `AES_GCM` | AES-GCM模式（推荐） | ✅ 安全 |
| | `SM4_GCM` | SM4-GCM模式（国密推荐） | ✅ 安全 |
| | `AES_CBC` | AES-CBC模式（旧系统兼容） | ⚠️ 不安全 |
| | `SM4_CBC` | SM4-CBC模式（旧系统兼容） | ⚠️ 不安全 |
| | `DES_CBC` | DES-CBC模式（旧系统兼容） | ⚠️ 不安全 |
| **非对称加密** | `RSA_OAEP` | RSA-OAEP模式（推荐） | ✅ 安全 |
| | `SM2` | SM2国密算法 | ✅ 安全 |
| | `RSA_PKCS1` | RSA-PKCS1模式（旧系统兼容） | ⚠️ 不安全 |
| **哈希算法** | `SHA256` | SHA-256哈希 | ✅ 安全 |
| | `SM3` | SM3国密哈希 | ✅ 安全 |
| | `MD5` | MD5哈希（旧系统兼容） | ⚠️ 不安全 |

**使用示例：**

```java
@Service
public class CryptoService {
    
    /**
     * 对称加密示例（通过枚举切换算法）
     */
    public String symmetricEncryptExample() {
        // 1. 使用AES-GCM（推荐）
        SymmetricAlgorithmType algo1 = SymmetricAlgorithmType.AES_GCM;
        String key1 = CryptoUtil.generateSymmetricKeyStr(algo1);
        String encrypted1 = CryptoUtil.encryptStr(algo1, key1, "敏感数据");
        String decrypted1 = CryptoUtil.decryptStr(algo1, key1, encrypted1);
        
        // 2. 使用SM4-GCM（国密标准）
        SymmetricAlgorithmType algo2 = SymmetricAlgorithmType.SM4_GCM;
        String key2 = CryptoUtil.generateSymmetricKeyStr(algo2);
        String encrypted2 = CryptoUtil.encryptStr(algo2, key2, "敏感数据");
        String decrypted2 = CryptoUtil.decryptStr(algo2, key2, encrypted2);
        
        return decrypted1; // "敏感数据"
    }
    
    /**
     * 非对称加密示例（通过枚举切换算法）
     */
    public String asymmetricEncryptExample() {
        // 1. 使用RSA-OAEP（推荐）
        AsymmetricAlgorithmType algo1 = AsymmetricAlgorithmType.RSA_OAEP;
        KeyPair keyPair1 = CryptoUtil.generateKeyPair(algo1);
        String publicKey1 = CryptoUtil.getPublicKeyPem(keyPair1.getPublic());
        String privateKey1 = CryptoUtil.getPrivateKeyPem(keyPair1.getPrivate());
        
        String encrypted1 = CryptoUtil.encryptStr(algo1, publicKey1, "机密信息");
        String decrypted1 = CryptoUtil.decryptStr(algo1, privateKey1, encrypted1);
        
        // 2. 使用SM2（国密标准）
        AsymmetricAlgorithmType algo2 = AsymmetricAlgorithmType.SM2;
        KeyPair keyPair2 = CryptoUtil.generateKeyPair(algo2);
        String publicKey2 = CryptoUtil.getPublicKeyPem(keyPair2.getPublic());
        String privateKey2 = CryptoUtil.getPrivateKeyPem(keyPair2.getPrivate());
        
        String encrypted2 = CryptoUtil.encryptStr(algo2, publicKey2, "机密信息");
        String decrypted2 = CryptoUtil.decryptStr(algo2, privateKey2, encrypted2);
        
        return decrypted1; // "机密信息"
    }
    
    /**
     * 哈希算法示例（通过枚举切换算法）
     */
    public String hashExample() {
        String data = "需要哈希的数据";
        
        // 1. 使用SHA256（推荐）
        String hash1 = CryptoUtil.hashStr(HashAlgorithmType.SHA256, data);
        
        // 2. 使用SM3（国密标准）
        String hash2 = CryptoUtil.hashStr(HashAlgorithmType.SM3, data);
        
        // 3. 使用MD5（不推荐，仅用于旧系统兼容）
        String hash3 = CryptoUtil.hashStr(HashAlgorithmType.MD5, data);
        
        return hash1;
    }
    
    /**
     * 密码哈希示例（推荐使用BCrypt）
     */
    public void passwordHashExample() {
        String password = "user_password_123";
        
        // 哈希密码（自动生成盐值）
        String hashedPassword = CryptoUtil.hashPassword(password);
        
        // 验证密码
        boolean isMatch = CryptoUtil.checkPassword(password, hashedPassword);
        
        log.info("密码验证结果: {}", isMatch); // true
    }
}
```

**重要说明：**
- **推荐使用**：`AES_GCM`、`SM4_GCM`（对称加密），`RSA_OAEP`、`SM2`（非对称加密），`SHA256`、`SM3`（哈希）
- **旧系统兼容**：`AES_CBC`、`DES_CBC`、`SM4_CBC`、`RSA_PKCS1`、`MD5`会打印警告日志，建议尽快迁移
- **自动切换**：只需更改枚举参数即可切换加密算法，无需修改其他代码
- **密钥管理**：对称加密密钥建议使用Base64编码存储，非对称加密密钥建议使用PEM格式

### 3. 统一响应格式

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @GetMapping("/data")
    public R<List<Data>> getData() {
        List<Data> data = dataService.list();
        return R.ok(data);
    }
    
    @PostMapping("/create")
    public R<Void> create(@RequestBody CreateRequest request) {
        dataService.create(request);
        return R.ok();
    }
    
    @GetMapping("/error")
    public R<Void> error() {
        return R.error("操作失败");
    }
}
```

### 4. 线程池工具（异步任务）

```java
@Service
public class AsyncService {
    
    /**
     * 异步执行有返回值的任务
     */
    public CompletableFuture<String> asyncTask() {
        return ThreadUtils.supplyAsync(() -> {
            // 模拟耗时操作
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "异步任务完成";
        });
    }
    
    /**
     * 异步执行无返回值的任务
     */
    public void asyncRun() {
        ThreadUtils.runAsync(() -> {
            // 异步执行，不阻塞主线程
            log.info("异步任务执行中...");
            doSomething();
        });
    }
    
    /**
     * 延迟执行任务
     */
    public void scheduleTask() {
        ThreadUtils.schedule(() -> {
            log.info("延迟5秒后执行");
        }, 5, TimeUnit.SECONDS);
    }
    
    /**
     * 定时执行任务（固定频率）
     */
    public void scheduleAtFixedRate() {
        ThreadUtils.scheduleAtFixedRate(() -> {
            log.info("每10秒执行一次");
        }, 0, 10, TimeUnit.SECONDS);
    }
    
    /**
     * 等待异步任务完成
     */
    public String waitForResult() throws Exception {
        CompletableFuture<String> future = asyncTask();
        // join()：阻塞等待，异常以CompletionException抛出
        return future.join();
        
        // 或者使用get()：阻塞等待，声明式抛出checked异常
        // return future.get();
    }
}
```

### 5. 循环调度服务（均匀延迟）

```java
@Service
public class OrderQueryService extends AbsCycleService<OrderQuery> {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Override
    public Class<OrderQuery> getEventClass() {
        return OrderQuery.class;
    }
    
    @Override
    protected boolean execute(OrderQuery query, int num, Map<String, Object> parameters) {
        log.info("第{}次查询订单: {}", num, query.getOrderId());
        
        Order order = orderMapper.selectById(query.getOrderId());
        if (order != null && order.getStatus() == 1) {
            log.info("订单已支付，停止轮询");
            return true; // 返回true表示任务完成，停止轮询
        }
        
        return false; // 返回false表示继续轮询
    }
}

// 使用示例
@Service
public class OrderService {
    
    @Autowired
    private OrderQueryService orderQueryService;
    
    public void checkOrderPayment(String orderId) {
        OrderQuery query = new OrderQuery();
        query.setOrderId(orderId);
        
        // 最多查询10次，每次间隔5秒（均匀延迟）
        // 第1次：立即执行
        // 第2次：5秒后
        // 第3次：10秒后
        // ...
        orderQueryService.runUniform(query, 10, 5);
    }
}
```

### 6. 循环调度服务（累加延迟）

```java
@Service
public class RetryService extends AbsCycleService<RetryTask> {
    
    @Override
    public Class<RetryTask> getEventClass() {
        return RetryTask.class;
    }
    
    @Override
    protected boolean execute(RetryTask task, int num, Map<String, Object> parameters) {
        log.info("第{}次重试任务: {}", num, task.getTaskId());
        
        try {
            // 执行任务
            executeTask(task);
            log.info("任务执行成功");
            return true;
        } catch (Exception e) {
            log.warn("第{}次重试失败: {}", num, e.getMessage());
            return false;
        }
    }
}

// 使用示例
@Service
public class TaskService {
    
    @Autowired
    private RetryService retryService;
    
    public void executeWithRetry(String taskId) {
        RetryTask task = new RetryTask();
        task.setTaskId(taskId);
        
        // 最多重试5次，基础间隔10秒，累加延迟
        // 第1次：立即执行
        // 第2次：10秒后（10×1）
        // 第3次：30秒后（10×2 + 10×1）
        // 第4次：60秒后（10×3 + 10×2 + 10×1）
        // 第5次：100秒后（10×4 + 10×3 + 10×2 + 10×1）
        retryService.runAccumulate(task, 5, 10);
    }
}
```

---

[返回主文档](../README.md)

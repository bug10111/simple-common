# simple-common-core 核心基础模块

> @author qty
> 版本：1.0.1

## 1. 模块介绍

`simple-common-core` 是 simple-common 框架的核心基础模块，所有其他模块（mp、redis、cache、rabbitmq、eventbus、auth、websocket、excel、sms、annex、doc、xxljob、ai、alibaba 等）均直接或间接依赖此模块。

该模块提供以下核心能力：

- **统一响应封装**：`R<T>` 统一返回格式，`AbstractException` 异常体系
- **断言工具**：`AssertUtils` 提供丰富的断言方法，支持占位符格式化
- **唯一ID生成**：`IdUtils` 基于本机IP的雪花算法 + UUID
- **JSON转化**：`JsonUtils` 对象与JSON字符串互转
- **多线程工具**：`ThreadUtils` 异步执行、延时任务、定时任务，内置高性能线程池
- **对象操作**：`BeanUtils` 属性复制、对象转Map、Map填充对象
- **加解密**：`CryptoUtil` 对称加密（AES/SM4/DES）、非对称加密（RSA/SM2）、哈希（MD5/SHA256/SHA512/SM3）、BCrypt密码哈希
- **签名工具**：`SignUtils` API签名、HMAC-SHA256签名、RSA/SM2数字签名
- **分布式锁接口**：`LockService` 可重入锁、公平锁
- **循环任务接口**：`CycleService` / `AbsCycleService` 自动重试、延迟执行
- **线程池管理接口**：`ThreadService` / `DefaultThreadService` 双线程池（定时调度 + 高并发异步）
- **其他工具类**：Base64编解码、IP地址获取、二维码生成、文件操作、ZIP压缩、XML转换、树形结构构建、URL路由匹配、XSS防护等

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-core</artifactId>
    <version>1.0.1</version>
</dependency>
```

该模块会自动传递引入以下依赖：
- `hutool-all`：核心工具库
- `fastjson2`：JSON处理
- `bcpkix-jdk15on`：国密算法支持（BouncyCastle）
- `knife4j-openapi3-jakarta-spring-boot-starter`：Swagger/OpenAPI 文档
- `spring-boot-starter-validation`：参数校验
- `spring-boot-starter-aop`：AOP 支持
- `aviator`：表达式引擎
- `zxing (core + javase)`：二维码生成

## 3. 核心功能

| 类名 | 功能描述 | 关键特性 |
|------|---------|---------|
| [`R`](simple-common-core/src/main/java/com/simple/common/core/response/R.java:14) | 统一响应封装 | 泛型支持、成功/失败静态工厂方法、异常枚举构造 |
| [`AssertUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/AssertUtils.java:16) | 断言工具 | notEmpty/notNull/isTrue/error，支持占位符格式化、自定义异常枚举 |
| [`IdUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/IdUtils.java:13) | 唯一ID生成 | 基于本机IP的雪花算法（分布式唯一）、UUID、随机数字 |
| [`JsonUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/JsonUtils.java:17) | JSON转化 | 对象转JSON字符串、JSON字符串转对象、JSON数组转List |
| [`ThreadUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/ThreadUtils.java:33) | 多线程工具 | 异步执行（CompletableFuture）、延时任务、定时任务、线程池监控 |
| [`BeanUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/BeanUtils.java:16) | 对象操作 | 继承hutool BeanUtil，对象转Map、Map填充对象、copyProperties |
| [`Base64Utils`](simple-common-core/src/main/java/com/simple/common/core/utils/Base64Utils.java:13) | Base64编解码 | 字符串/字节数组编码、解码为字符串/字节数组 |
| [`IPUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/IPUtils.java:22) | IP地址工具 | 客户端IP获取（多级代理穿透）、内网IP、公网IP |
| [`CryptoUtil`](simple-common-core/src/main/java/com/simple/common/core/utils/CryptoUtil.java:37) | 加解密工具 | 对称（AES/SM4/DES）、非对称（RSA/SM2）、哈希（MD5/SHA256/SHA512/SM3）、BCrypt |
| [`SignUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/SignUtils.java:20) | 签名工具 | API签名串生成、HMAC-SHA256签名、RSA/SM2数字签名与验签 |
| [`LockService`](simple-common-core/src/main/java/com/simple/common/core/common/service/lock/LockService.java:21) | 分布式锁接口 | 可重入锁（有/无返回值）、公平锁 |
| [`AbsLockService`](simple-common-core/src/main/java/com/simple/common/core/common/service/lock/AbsLockService.java:11) | 锁服务抽象类 | 统一key前缀管理 |
| [`CycleService`](simple-common-core/src/main/java/com/simple/common/core/common/service/cycle/CycleService.java:48) | 循环任务接口 | 均匀延迟、累加延迟、扩展参数 |
| [`AbsCycleService`](simple-common-core/src/main/java/com/simple/common/core/common/service/cycle/AbsCycleService.java:19) | 循环任务抽象类 | 自动重试、成功/失败/超限/异常回调 |
| [`ThreadService`](simple-common-core/src/main/java/com/simple/common/core/common/service/thread/ThreadService.java:25) | 线程池管理接口 | 定时调度池、高并发异步池、监控方法 |
| [`DefaultThreadService`](simple-common-core/src/main/java/com/simple/common/core/service/thread/DefaultThreadService.java:22) | 线程池默认实现 | 双线程池、守护/非守护线程、优雅停机、异常捕获 |
| [`ThreadProperties`](simple-common-core/src/main/java/com/simple/common/core/common/properties/ThreadProperties.java:20) | 线程池配置 | 可配置核心/最大线程数、队列容量、存活时间、停机等待 |
| [`DefaultExceptionHandler`](simple-common-core/src/main/java/com/simple/common/core/exception/DefaultExceptionHandler.java:22) | 全局异常处理 | 统一异常捕获与R响应 |
| [`QRCodeUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/QRCodeUtils.java:31) | 二维码工具 | 单个/批量生成、Logo叠加、底部文字、异步批量ZIP下载 |
| [`RecursiveUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/RecursiveUtils.java:21) | 树形结构工具 | 自动识别顶级节点、多根森林构建、固定根节点构建 |
| [`UrlRulesUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/UrlRulesUtils.java:23) | URL路由匹配 | AntPathMatcher模式匹配、批量匹配、交集判断 |

## 4. 配置说明

### 4.1 线程池配置（`simple.thread-pool`）

配置类：[`ThreadProperties`](simple-common-core/src/main/java/com/simple/common/core/common/properties/ThreadProperties.java:20)

| 属性 | 配置键 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| `asyncCorePoolSize` | `simple.thread-pool.async-core-pool-size` | `int` | `CPU核心数 * 2 + 1` | 异步线程池核心线程数（I/O密集型） |
| `asyncMaxPoolSize` | `simple.thread-pool.async-max-pool-size` | `int` | `asyncCorePoolSize * 2` | 异步线程池最大线程数（设为0时自动计算） |
| `asyncQueueCapacity` | `simple.thread-pool.async-queue-capacity` | `int` | `2000` | 异步线程池队列容量 |
| `asyncKeepAliveSeconds` | `simple.thread-pool.async-keep-alive-seconds` | `long` | `60` | 异步线程池空闲线程存活时间（秒） |
| `scheduledCorePoolSize` | `simple.thread-pool.scheduled-core-pool-size` | `int` | `min(4, max(2, CPU核心数/2))` | 定时调度线程池核心线程数 |
| `scheduledKeepAliveSeconds` | `simple.thread-pool.scheduled-keep-alive-seconds` | `long` | `60` | 定时调度线程池空闲线程存活时间（秒） |
| `scheduledAllowCoreThreadTimeOut` | `simple.thread-pool.scheduled-allow-core-thread-time-out` | `boolean` | `true` | 是否允许定时调度核心线程超时回收 |
| `shutdownAwaitTerminationSeconds` | `simple.thread-pool.shutdown-await-termination-seconds` | `long` | `120` | 线程池关闭时等待任务完成的最大时间（秒） |
| `shutdownNowAwaitTerminationSeconds` | `simple.thread-pool.shutdown-now-await-termination-seconds` | `long` | `5` | 线程池强制关闭后的二次等待时间（秒） |

**配置示例**：

```yaml
simple:
  thread-pool:
    async-core-pool-size: 17
    async-max-pool-size: 34
    async-queue-capacity: 2000
    async-keep-alive-seconds: 60
    scheduled-core-pool-size: 4
    scheduled-keep-alive-seconds: 60
    scheduled-allow-core-thread-time-out: true
    shutdown-await-termination-seconds: 120
    shutdown-now-await-termination-seconds: 5
```

### 4.2 锁配置（`simple.lock`）

配置类：[`LockProperties`](simple-common-core/src/main/java/com/simple/common/core/common/properties/LockProperties.java:18)

| 属性 | 配置键 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| `bag` | `simple.lock.bag` | `boolean` | `true` | 是否创建锁key前缀包 |
| `defaultBag` | `simple.lock.default-bag` | `String` | `"lock"` | 锁key默认前缀名称 |

### 4.3 应用配置（`spring.application`）

配置类：[`ApplicationProperties`](simple-common-core/src/main/java/com/simple/common/core/common/properties/ApplicationProperties.java:18)

| 属性 | 配置键 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| `name` | `spring.application.name` | `String` | `null` | 服务名称 |

### 4.4 阿里云配置（`simple.ali`）

配置类：[`AlibabaProperties`](simple-common-core/src/main/java/com/simple/common/core/common/properties/AlibabaProperties.java:18)

| 属性 | 配置键 | 类型 | 默认值 | 说明 |
|------|--------|------|--------|------|
| `accessKeyId` | `simple.ali.access-key-id` | `String` | `null` | 阿里云AccessKey ID |
| `accessKeySecret` | `simple.ali.access-key-secret` | `String` | `null` | 阿里云AccessKey Secret |

### 4.5 Aviator 表达式引擎配置

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `simple.aviator.open` | `String` | `null` | 是否开启Aviator自定义函数（设为`"true"`时开启） |

### 4.6 自动配置

模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册 [`DefaultWebMvcConfigurer`](simple-common-core/src/main/java/com/simple/common/core/common/config/DefaultWebMvcConfigurer.java:18)，提供以下功能：
- `@EnableSpringUtil`：启用 Hutool 的 `SpringUtil`，支持静态获取 Spring Bean
- `@ComponentScan(basePackages = {"com.simple.common.core"})`：自动扫描 core 模块组件
- CORS 跨域配置：允许所有源、所有方法、携带凭证

## 5. 核心类与接口详细说明

### 5.1 R\<T\> 统一响应封装

**类路径**：`com.simple.common.core.response.R`

**字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | `String` | 状态码（"200"成功，"500"失败） |
| `message` | `String` | 响应消息 |
| `data` | `T` | 数据载体 |

**构造方法**：

| 方法签名 | 说明 |
|---------|------|
| `R()` | 无参构造 |
| `R(AbstractException abstractException)` | 从异常枚举构造 |
| `R(AbstractException abstractException, T data)` | 从异常枚举构造 + 数据 |
| `R(String code, String message)` | 自定义code + message |

**静态工厂方法**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `R.ok()` | `R<Object>` | 成功响应，code="200" |
| `R.ok(T t)` | `R<T>` | 成功响应 + 数据 |
| `R.error()` | `R<T>` | 失败响应，code="500" |
| `R.error(T t)` | `R<T>` | 失败响应 + 数据 |
| `R.error(String code, String message)` | `R<T>` | 自定义code + message的失败响应 |

### 5.2 AssertUtils 断言工具

**类路径**：`com.simple.common.core.utils.AssertUtils`

**方法清单**：

| 方法签名 | 说明 |
|---------|------|
| `notEmpty(Object object, String errorMsg)` | 断言对象非空（支持CharSequence/Map/Iterable/Iterator/Array） |
| `notEmpty(Object object, String errorMsg, Object... params)` | 断言非空 + 占位符格式化 |
| `notEmpty(Object object, AbstractException exception)` | 断言非空 + 自定义异常枚举 |
| `notEmpty(Object object, AbstractException exception, String errorMsg)` | 断言非空 + 异常枚举 + 重写消息 |
| `notEmpty(Object object, AbstractException exception, String errorMsg, Object... params)` | 断言非空 + 异常枚举 + 重写消息 + 占位符 |
| `notNull(Object object, String errorMsg)` | 断言非null |
| `isTrue(boolean expression, String errorStr)` | 断言布尔为true |
| `isTrue(boolean expression, String errorStr, Object... params)` | 断言布尔为true + 占位符 |
| `isTrue(boolean expression, AbstractException abstractException)` | 断言布尔为true + 异常枚举 |
| `isTrue(boolean expression, AbstractException abstractException, String errorStr)` | 断言布尔为true + 异常枚举 + 重写消息 |
| `isTrue(boolean expression, AbstractException abstractException, String errorStr, Object... params)` | 断言布尔为true + 异常枚举 + 重写消息 + 占位符 |
| `error(AbstractException abstractException)` | 直接抛出异常 |
| `error(AbstractException abstractException, String errorStr)` | 抛出异常 + 重写消息 |
| `error(AbstractException abstractException, String errorStr, Object... params)` | 抛出异常 + 重写消息 + 占位符 |
| `error(String error)` | 抛出DefaultException（code="500"） |
| `error(String error, Object... params)` | 抛出异常 + 占位符 |
| `error(String error, Object Data)` | 抛出异常 + 附带数据 |

**占位符格式**：使用 `{}` 作为占位符，如 `"用户[{}]不存在"` + `userId`。

### 5.3 IdUtils 唯一ID生成

**类路径**：`com.simple.common.core.utils.IdUtils`

**雪花算法原理**：基于本机IP完整哈希自动计算 `workerId`（0-31）和 `datacenterId`（0-31），将完整IP字符串哈希到0-1023槽位（32x32），避免同网段IP碰撞。

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getSnowflakeNextIdStr()` | `String` | 雪花ID（基于本机IP保证分布式唯一） |
| `getFastSimpleUUID()` | `String` | UUID去掉横线 |
| `getFastUUID()` | `String` | 标准UUID |
| `randomNumbers()` | `String` | 9位随机数字 |
| `randomNumbers(int length)` | `String` | 指定长度随机数字 |

### 5.4 JsonUtils JSON转化

**类路径**：`com.simple.common.core.utils.JsonUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `toJsonStr(Object obj)` | `String` | 对象转JSON字符串（基于hutool JSONUtil） |
| `toJsonPrettyStr(Object obj)` | `String` | 对象转格式化JSON字符串 |
| `toJsonObj(String json, Class<T> c)` | `T` | JSON字符串转对象（基于hutool JSONUtil） |
| `toList(String json, Class<T> c)` | `List<T>` | JSON数组字符串转List（基于fastjson2） |

### 5.5 ThreadUtils 多线程工具

**类路径**：`com.simple.common.core.utils.ThreadUtils`

**异步执行**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `supplyAsync(Supplier<U> supplier)` | `CompletableFuture<U>` | 异步执行有返回值任务（使用高性能线程池） |
| `runAsync(Runnable runnable)` | `CompletableFuture<Void>` | 异步执行无返回值任务 |

**延时任务**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `schedule(Runnable runnable, long time, TimeUnit timeUnit)` | `void` | 延时执行任务 |
| `scheduleWithFuture(Runnable runnable, long time, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 延时执行任务，返回Future用于取消 |

**定时任务**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `scheduleWithFixedDelay(Runnable runnable, long initialTime, long fixedTime, TimeUnit timeUnit)` | `void` | 固定延迟定时任务 |
| `scheduleWithFixedDelayFuture(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 固定延迟定时任务，返回Future |
| `scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit timeUnit)` | `void` | 无初始延迟的固定延迟定时任务 |
| `scheduleWithFixedDelayFuture(Runnable runnable, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 无初始延迟，返回Future |
| `scheduleWithFixedDelaySafe(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 安全版定时任务（自动捕获异常，保证调度不中断） |
| `scheduleWithFixedDelaySafe(Runnable runnable, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 无初始延迟的安全版定时任务 |

**线程池获取**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getScheduledThreadPoolExecutor()` | `ScheduledThreadPoolExecutor` | 获取定时调度线程池（少量并发和延迟任务） |
| `getAsyncExecutor()` | `ExecutorService` | 获取异步线程池（高并发高性能场景） |

**监控方法**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getScheduledQueueSize()` | `int` | 定时调度线程池队列大小 |
| `getAsyncActiveCount()` | `int` | 异步线程池活跃线程数 |
| `getAsyncQueueSize()` | `int` | 异步线程池队列大小 |

### 5.6 BeanUtils 对象操作

**类路径**：`com.simple.common.core.utils.BeanUtils`（继承 `cn.hutool.core.bean.BeanUtil`）

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `toMap(Object obj)` | `Map<String, Object>` | 对象转Map（驼峰key，忽略null） |
| `fillBeanWithMap(Map<?, ?> map, Class<T> tClass)` | `T` | Map填充到新对象 |
| `copyProperties(Object source, Object target)` | `void` | 属性复制（继承自hutool BeanUtil） |
| `copyProperties(Object source, Class<T> tClass)` | `T` | 属性复制到新对象（继承自hutool BeanUtil） |

### 5.7 Base64Utils

**类路径**：`com.simple.common.core.utils.Base64Utils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `encode(String text)` | `String` | 字符串进行Base64编码 |
| `encode(byte[] text)` | `String` | 字节数组进行Base64编码 |
| `decodeStr(String text)` | `String` | Base64解码为字符串 |
| `decode(String text)` | `byte[]` | Base64解码为字节数组 |

### 5.8 IPUtils IP地址工具

**类路径**：`com.simple.common.core.utils.IPUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getIpAddr()` | `String` | 获取当前请求客户端IP（从HttpServletRequest获取） |
| `getIpAddr(HttpServletRequest request)` | `String` | 获取指定请求的客户端IP（支持多级反向代理穿透） |
| `getIntranetIp()` | `String` | 获取本机内网IP（IPv4） |
| `getPublicNetworkIp()` | `String` | 获取本机公网IP |

**客户端IP获取顺序**：`x-forwarded-for` → `Proxy-Client-IP` → `X-Forwarded-For` → `WL-Proxy-Client-IP` → `X-Real-IP` → `request.getRemoteAddr()`

### 5.9 CryptoUtil 加解密工具

**类路径**：`com.simple.common.core.utils.CryptoUtil`

#### 对称加密

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `generateSymmetricKey(SymmetricAlgorithmType algorithm)` | `byte[]` | 生成对称加密密钥 |
| `generateSymmetricKeyStr(SymmetricAlgorithmType algorithm)` | `String` | 生成对称加密密钥（Base64字符串） |
| `encrypt(SymmetricAlgorithmType algorithm, byte[] key, byte[] data)` | `byte[]` | 对称加密（字节数组） |
| `encryptStr(SymmetricAlgorithmType algorithm, String key, String data)` | `String` | 对称加密（Base64字符串） |
| `decrypt(SymmetricAlgorithmType algorithm, byte[] key, byte[] encryptedData)` | `byte[]` | 对称解密（字节数组） |
| `decryptStr(SymmetricAlgorithmType algorithm, String key, String encryptedDataStr)` | `String` | 对称解密（Base64字符串） |

**对称算法枚举 `SymmetricAlgorithmType`**：

| 枚举值 | 算法 | 模式 | 填充 | IV长度 | 安全性 |
|--------|------|------|------|--------|--------|
| `AES_CBC` | AES | CBC | PKCS5Padding | 16字节 | 不安全（仅兼容旧系统） |
| `DES_CBC` | DES | CBC | PKCS5Padding | 8字节 | 不安全（仅兼容旧系统） |
| `SM4_CBC` | SM4 | CBC | PKCS5Padding | 16字节 | 不安全（仅兼容旧系统） |
| `AES_GCM` | AES | GCM | NoPadding | 12字节 | 安全（AEAD认证加密） |
| `SM4_GCM` | SM4 | GCM | NoPadding | 12字节 | 安全（AEAD认证加密） |

#### 非对称加密

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `generateKeyPair(AsymmetricAlgorithmType algorithm)` | `KeyPair` | 生成非对称密钥对 |
| `encrypt(AsymmetricAlgorithmType algorithm, PublicKey publicKey, byte[] data)` | `byte[]` | 非对称加密（公钥加密） |
| `encryptStr(AsymmetricAlgorithmType algorithm, String publicKeyPem, String data)` | `String` | 非对称加密（PEM公钥） |
| `decrypt(AsymmetricAlgorithmType algorithm, PrivateKey privateKey, byte[] encryptedData)` | `byte[]` | 非对称解密（私钥解密） |
| `decryptStr(AsymmetricAlgorithmType algorithm, String privateKeyPem, String encryptedDataStr)` | `String` | 非对称解密（PEM私钥） |

**非对称算法枚举 `AsymmetricAlgorithmType`**：

| 枚举值 | 算法 | 密钥长度 | 签名算法 | 安全性 |
|--------|------|---------|---------|--------|
| `RSA_PKCS1` | RSA/ECB/PKCS1Padding | 2048 | SHA256withRSA | 不安全（仅兼容旧系统） |
| `RSA_PSS` | RSA/ECB/PKCS1Padding | 2048 | SHA256withRSA_PSS | 签名安全/加密不安全 |
| `RSA_OAEP` | RSA/ECB/OAEPWithSHA-256AndMGF1Padding | 2048 | SHA256withRSA | 安全（推荐） |
| `SM2` | SM2 | 256 | null | 安全（国密推荐） |

#### 哈希

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `hash(HashAlgorithmType algorithm, byte[] data)` | `byte[]` | 哈希计算（字节数组） |
| `hashStr(HashAlgorithmType algorithm, String data)` | `String` | 哈希计算（字符串，返回Base64） |

**哈希算法枚举 `HashAlgorithmType`**：

| 枚举值 | 算法 | 安全性 |
|--------|------|--------|
| `MD5` | MD5 | 不安全（仅兼容旧系统） |
| `SHA256` | SHA-256 | 安全 |
| `SHA512` | SHA-512 | 安全 |
| `SM3` | SM3 | 安全（国密推荐） |

#### 密码哈希（BCrypt）

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `hashPassword(String password)` | `String` | BCrypt加密密码 |
| `checkPassword(String password, String hashedPassword)` | `boolean` | BCrypt验证密码 |

#### 密钥PEM转换

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `restoreKeyPair(String publicKeyPem, String privateKeyPem)` | `KeyPair` | 从PEM恢复密钥对 |
| `getPublicKeyPem(PublicKey publicKey)` | `String` | 公钥转PEM格式 |
| `getPrivateKeyPem(PrivateKey privateKey)` | `String` | 私钥转PEM格式 |
| `getPublicKeyFromPem(String pem)` | `PublicKey` | PEM转公钥对象 |
| `getPrivateKeyFromPem(String pem)` | `PrivateKey` | PEM转私钥对象 |

### 5.10 SignUtils 签名工具

**类路径**：`com.simple.common.core.utils.SignUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `generateSignStr(T t, String... excludeFields)` | `String` | 生成API签名字符串（按参数名ASCII排序，排除指定字段） |
| `sign(AsymmetricAlgorithmType algorithm, PrivateKey privateKey, byte[] data)` | `byte[]` | RSA/SM2数字签名 |
| `verify(AsymmetricAlgorithmType algorithm, PublicKey publicKey, byte[] data, byte[] signData)` | `boolean` | RSA/SM2验签 |
| `signWeb(String message, String secretKey)` | `String` | HMAC-SHA256签名（Web请求场景） |
| `verifyWeb(String message, String signature, String secretKey)` | `boolean` | HMAC-SHA256验签 |

### 5.11 LockService 分布式锁接口

**类路径**：`com.simple.common.core.common.service.lock.LockService`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `lock(String key, DefaultFunction function)` | `void` | 可重入锁（无返回值） |
| `lockHaveValue(String key, ReturnValueFunction function)` | `Object` | 可重入锁（有返回值） |
| `fairLock(String key, DefaultFunction function)` | `void` | 公平锁（FIFO，无返回值） |

**函数式接口**：

| 接口 | 方法 | 说明 |
|------|------|------|
| [`DefaultFunction`](simple-common-core/src/main/java/com/simple/common/core/function/DefaultFunction.java:10) | `void handler() throws Throwable` | 无参数无返回值 |
| [`ReturnValueFunction`](simple-common-core/src/main/java/com/simple/common/core/function/ReturnValueFunction.java:10) | `Object handler() throws Throwable` | 无参数有返回值 |

**抽象类 [`AbsLockService`](simple-common-core/src/main/java/com/simple/common/core/common/service/lock/AbsLockService.java:11)**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getKey(String methodName, String key)` | `String` | 获取锁key（格式：`defaultBag:methodName + key`） |

### 5.12 CycleService 循环任务接口

**类路径**：`com.simple.common.core.common.service.cycle.CycleService<T>`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `run(T runBody, Integer sum, Integer timeInterval, Boolean isAccumulate, Map<String, Object> parameters)` | `void` | 完整参数执行 |
| `runUniform(T runBody, Integer sum, Integer timeInterval)` | `void` | 均匀延迟（每次间隔固定） |
| `runUniform(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters)` | `void` | 均匀延迟 + 扩展参数 |
| `runAccumulate(T runBody, Integer sum, Integer timeInterval)` | `void` | 累加延迟（interval x 当前次数） |
| `runAccumulate(T runBody, Integer sum, Integer timeInterval, Map<String, Object> parameters)` | `void` | 累加延迟 + 扩展参数 |

**抽象类 [`AbsCycleService`](simple-common-core/src/main/java/com/simple/common/core/common/service/cycle/AbsCycleService.java:19)** 需实现的抽象方法：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getEventClass()` | `Class<T>` | 返回任务数据类型Class |
| `handler(T runBody, Map<String, Object> parameters)` | `Boolean` | 执行业务逻辑，返回是否成功 |
| `ok(T runBody, Map<String, Object> parameters)` | `void` | 任务成功回调 |
| `more(T runBody, Map<String, Object> parameters)` | `void` | 超过最大次数回调 |
| `error(T runBody, Map<String, Object> parameters)` | `void` | 执行异常回调 |

**可选覆盖方法**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `addCounter(T runBody, Map<String, Object> parameters, int count)` | `void` | 增加计数器（默认空实现） |

### 5.13 ThreadService 线程池管理接口

**类路径**：`com.simple.common.core.common.service.thread.ThreadService`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getExecutor()` | `ScheduledThreadPoolExecutor` | 获取定时调度线程池 |
| `getAsyncExecutor()` | `ExecutorService` | 获取异步线程池 |
| `schedule(Runnable runnable, int time, TimeUnit timeUnit)` | `void` | 延时执行任务 |
| `schedule(Runnable runnable, long time, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 延时执行任务，返回Future |
| `scheduleWithFixedDelay(Runnable runnable, int InitialTime, int fixedTime, TimeUnit timeUnit)` | `void` | 固定延迟定时任务 |
| `scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 固定延迟定时任务，返回Future |
| `scheduleWithFixedDelay(Runnable runnable, int delay, TimeUnit timeUnit)` | `void` | 无初始延迟的固定延迟定时任务 |
| `scheduleWithFixedDelay(Runnable runnable, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 无初始延迟，返回Future |
| `scheduleWithFixedDelaySafe(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit)` | `ScheduledFuture<?>` | 安全版定时任务（自动捕获异常） |
| `getScheduledQueueSize()` | `int` | 定时调度线程池队列大小 |
| `getAsyncActiveCount()` | `int` | 异步线程池活跃线程数 |
| `getAsyncQueueSize()` | `int` | 异步线程池队列大小 |

### 5.14 DefaultThreadService 线程池默认实现

**类路径**：`com.simple.common.core.service.thread.DefaultThreadService`

**实现特性**：
- 实现 `ThreadService` 和 `InitializingBean` 接口
- **定时调度线程池**：守护线程（`daemon=true`），`CallerRunsPolicy` 拒绝策略，线程名前缀 `simple-scheduled-`
- **异步线程池**：非守护线程（`daemon=false`），`CallerRunsPolicy` 拒绝策略，线程名前缀 `simple-async-`
- 两个线程池均通过 `afterExecute` 钩子自动捕获并记录任务异常
- `@PreDestroy` 优雅停机：先 `shutdown()` 等待，超时后 `shutdownNow()` 强制关闭
- `scheduleWithFixedDelaySafe` 覆盖实现：包装 try-catch，记录异常日志但不中断调度

### 5.15 异常体系

#### AbstractException 接口

**类路径**：`com.simple.common.core.exception.AbstractException`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getCode()` | `String` | 获取异常状态码 |
| `getMessage()` | `String` | 获取异常提示信息 |
| `getCodeInt()` | `int` | 获取异常状态码（int类型） |

#### DefaultExceptionEnum 默认异常枚举

**类路径**：`com.simple.common.core.exception.DefaultExceptionEnum`

| 枚举值 | code | message |
|--------|------|---------|
| `OK` | `"200"` | `"请求成功"` |
| `ERROR` | `"500"` | `"请求失败"` |

#### DefaultException 自定义异常

**类路径**：`com.simple.common.core.exception.DefaultException`（继承 `RuntimeException`）

| 构造方法 | 说明 |
|---------|------|
| `DefaultException(String code, String message)` | 自定义code + message |
| `DefaultException(String code, String message, Object data)` | 自定义code + message + 数据 |
| `DefaultException(AbstractException abstractException)` | 从异常枚举构造 |
| `DefaultException(AbstractException abstractException, String message)` | 异常枚举 + 重写消息 |
| `DefaultException(String message)` | 仅message（code默认"500"） |

#### DefaultExceptionHandler 全局异常处理

**类路径**：`com.simple.common.core.exception.DefaultExceptionHandler`

通过 `@ControllerAdvice` 全局捕获异常，统一返回 `R<Object>` 响应：

| 异常类型 | HTTP状态码 | 处理逻辑 |
|---------|-----------|---------|
| `HttpRequestMethodNotSupportedException` | 405 | 返回不支持的方法提示 |
| `IllegalArgumentException` | 400 | 返回异常message |
| `IllegalStateException` | 400 | 返回异常message |
| `MethodArgumentNotValidException` | 500 | 返回第一个校验错误message |
| `DefaultException` | 500 | 返回异常code和message |
| 其他 `Exception` | 500 | 返回"系统繁忙，请稍后再试" |

### 5.16 其他工具类

#### QRCodeUtils 二维码工具

**类路径**：`com.simple.common.core.utils.QRCodeUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `createZip(String logoBase64, int size)` | `void` | 批量生成二维码并ZIP下载（默认300x300） |
| `createZip(int width, int height, String logoBase64, int size)` | `void` | 批量生成二维码并ZIP下载（自定义尺寸） |
| `create(String content, String logoBase64, String bottomText)` | `void` | 生成单个二维码并输出到浏览器 |
| `create(int width, int height, String content, String logoBase64, String bottomText)` | `byte[]` | 生成单个二维码PNG字节数组 |

#### RecursiveUtils 树形结构工具

**类路径**：`com.simple.common.core.utils.RecursiveUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `get(List<T> list)` | `List<Tree<String>>` | 自动递归（从最高层级构建树，默认按serial排序） |
| `get(List<T> list, String childrenKey)` | `List<Tree<String>>` | 自动递归（自定义排序字段） |
| `getFromRoot(List<T> list)` | `List<Tree<String>>` | 从固定根节点构建树 |
| `getFromRoot(List<T> list, String childrenKey, String rootId)` | `List<Tree<String>>` | 从指定根节点构建树 |
| `getTopLevelNodes(List<T> list)` | `List<T>` | 获取所有顶级节点 |

**树节点配置**：id字段名=`id`，父id字段名=`parentId`，子集合名=`list`，默认排序字段=`serial`，默认根节点ID=`"0000000000"`

#### UrlRulesUtils URL路由匹配

**类路径**：`com.simple.common.core.utils.UrlRulesUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `matches(String urlPath, Collection<String> urls)` | `boolean` | 判断URL是否匹配集合中任意一个 |
| `matches(String urlPath, String[] urls)` | `boolean` | 判断URL是否匹配数组中任意一个 |
| `isMatch(String url, String urlPath)` | `boolean` | 判断URL是否匹配单个规则 |
| `hasIntersection(Set<?> set1, Set<?> set2)` | `boolean` | 判断两个集合是否有交集 |
| `findMatch(String path, Set<String> patterns)` | `String` | 查找匹配的模式 |
| `isPattern(String path)` | `boolean` | 判断表达式是否合法 |

**匹配规则**（AntPathMatcher）：`?` 单字符，`*` 多字符，`**` 多层路径

#### 其他工具类一览

| 类名 | 类路径 | 核心方法 |
|------|--------|---------|
| [`FileUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/FileUtils.java:17) | `com.simple.common.core.utils.FileUtils` | `write()`, `createFile()`, `getResourcesFileInputStream()`, `getPath()` |
| [`ZipUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/ZipUtils.java:20) | `com.simple.common.core.utils.ZipUtils` | `downloadZip()`, `write()` |
| [`ResponseUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/ResponseUtils.java:19) | `com.simple.common.core.utils.ResponseUtils` | `renderText()`, `renderJson()`, `writeResponse()`, `getResponseOutputStream()` |
| [`DateUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/DateUtils.java:15) | `com.simple.common.core.utils.DateUtils` | `getNetworkDate()`（继承hutool DateUtil全部方法） |
| [`SerializeUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/SerializeUtils.java:13) | `com.simple.common.core.utils.SerializeUtils` | `serialize()`, `deserialize()`, `readerFor()` |
| [`ClassUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/ClassUtils.java:15) | `com.simple.common.core.utils.ClassUtils` | `createInstance()`, `getField()`, `getFieldVar()`, `getFieldNames()` |
| [`CollectionUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/CollectionUtils.java:15) | `com.simple.common.core.utils.CollectionUtils` | `matches(String, Collection)`, `matches(Collection, Collection)` |
| [`HttpServletUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/HttpServletUtils.java:17) | `com.simple.common.core.utils.HttpServletUtils` | `getRequest()`, `getResponse()` |
| [`HttpRequestUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/HttpRequestUtils.java:17) | `com.simple.common.core.utils.HttpRequestUtils` | `post()`, `get()` |
| [`XmlUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/XmlUtils.java:18) | `com.simple.common.core.utils.XmlUtils` | `convertToXml()`, `convertToObject()` |
| [`AviatorUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/AviatorUtils.java:16) | `com.simple.common.core.utils.AviatorUtils` | `run()` |
| [`SpringElUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/SpringElUtils.java:11) | `com.simple.common.core.utils.SpringElUtils` | `getValue()` |
| [`XssUtils`](simple-common-core/src/main/java/com/simple/common/core/common/xss/XssUtils.java:13) | `com.simple.common.core.common.xss.XssUtils` | `clean()` |
| [`UserIdUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/UserIdUtils.java:12) | `com.simple.common.core.utils.UserIdUtils` | `getUserId()` |
| [`PageRequestUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/PageRequestUtils.java:17) | `com.simple.common.core.utils.PageRequestUtils` | `fetchAll()` |

#### 已废弃工具类（@Deprecated）

| 类名 | 说明 | 替代方案 |
|------|------|---------|
| [`RsaUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/RsaUtils.java:26) | RSA非对称加密（基于hutool RSA） | 使用 `CryptoUtil`（AsymmetricAlgorithmType.RSA_OAEP） |
| [`AlgorithmUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/AlgorithmUtils.java:22) | MD5/SHA/BCrypt摘要算法 | 使用 `CryptoUtil`（HashAlgorithmType / hashPassword） |
| [`HttpUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/HttpUtils.java:23) | HTTP请求工具 | 使用 `HttpRequestUtils` |

### 5.17 核心服务接口

#### CoreLoginUserService 登录用户服务

**类路径**：`com.simple.common.core.common.service.jwt.CoreLoginUserService`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getUserId()` | `String` | 获取当前登录用户ID |

#### BasProcessService 责任链处理服务

**类路径**：`com.simple.common.core.common.service.process.BasProcessService`（继承 `Ordered`）

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getProcess()` | `DefaultKindProcess` | 获取流程类型 |
| `getOrder()` | `int` | 获取执行顺序（默认从 `getProcess().getOrdered()` 获取） |

#### DefaultKindProcess 责任链处理器类型

**类路径**：`com.simple.common.core.common.enums.process.DefaultKindProcess`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `isExecute()` | `boolean` | 流程是否执行 |
| `getOrdered()` | `Integer` | 流程执行顺序（越小越靠前） |
| `getMsg()` | `String` | 说明 |
| `getCode()` | `String` | 编码（默认返回 `getMsg()`） |

### 5.18 XSS 防护

#### @XssSafe 注解

**类路径**：`com.simple.common.core.common.aspect.XssSafe`

用于字段或参数上，配合 `@Validated` 触发 XSS 校验：

```java
@XssSafe
private String content;
```

#### XssValidator 校验器

**类路径**：`com.simple.common.core.common.xss.XssValidator`

实现 `ConstraintValidator<XssSafe, String>`，正则匹配 `<.*?>|&[a-zA-Z]+;|[<>"'&]`，匹配到则校验失败。

#### XssUtils 工具类

**类路径**：`com.simple.common.core.common.xss.XssUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `clean(String htmlContent)` | `String` | 过滤HTML文本，防止XSS攻击（基于hutool HtmlUtil.filter） |

### 5.19 Aviator 表达式引擎

#### DefAviatorFunction 自定义函数基类

**类路径**：`com.simple.common.core.common.service.aviator.DefAviatorFunction`（继承 `AbstractFunction`）

继承此类实现自定义 Aviator 函数，通过 `@Component` 注册，在 `AviatorInit` 中自动加载。

#### AviatorInit 初始化器

**类路径**：`com.simple.common.core.init.AviatorInit`

在 `ApplicationReadyEvent` 时自动将所有 `DefAviatorFunction` Bean 注册到 Aviator 引擎。需配置 `simple.aviator.open=true` 开启。

#### AviatorUtils 表达式执行

**类路径**：`com.simple.common.core.utils.AviatorUtils`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `run(String expression, Map<String, Object> vars)` | `Object` | 执行Aviator表达式 |

## 6. 使用示例

### 6.1 统一响应 R\<T\>

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

### 6.2 AssertUtils 断言

```java
// 基本断言
AssertUtils.notEmpty(user, "主键为[{}]的用户不存在", id);
AssertUtils.notNull(obj, "对象不能为空");
AssertUtils.isTrue(user.getStatus() == 1, "用户已被禁用，无法操作");

// 自定义异常枚举
AssertUtils.notEmpty(token, AuthExceptionEnum.TOKEN_INVALID);
AssertUtils.isTrue(hasPermission, AuthExceptionEnum.NO_PERMISSION, "用户[{}]无权限", userId);

// 直接抛出异常
AssertUtils.error("系统异常");
AssertUtils.error("用户[{}]不存在", userId);
```

### 6.3 IdUtils 唯一ID生成

```java
// 雪花ID（推荐，全局唯一递增）
String id = IdUtils.getSnowflakeNextIdStr();

// UUID
String uuid = IdUtils.getFastSimpleUUID();

// 随机数字
String code = IdUtils.randomNumbers(6);
```

### 6.4 JsonUtils JSON转化

```java
// 对象转JSON
String json = JsonUtils.toJsonStr(user);

// JSON转对象
UserDTO user = JsonUtils.toJsonObj(json, UserDTO.class);

// JSON数组转List
List<UserDTO> list = JsonUtils.toList(jsonArray, UserDTO.class);
```

### 6.5 ThreadUtils 多线程

> 示例来源：[`QRCodeAndThreadController`](simple-common-test/src/main/java/com/simple/common/test/controller/QRCodeAndThreadController.java:29)

```java
// 异步执行（使用内置高性能线程池）
CompletableFuture<UserDTO> future = ThreadUtils.supplyAsync(() -> userService.findById(id));

// 批量异步生成二维码（来自 QRCodeAndThreadController）
QRCodeUtils.createZip(null, size);

// 延时任务
ThreadUtils.schedule(() -> orderService.cancelTimeout(orderId), 30, TimeUnit.MINUTES);

// 定时任务（安全版，自动捕获异常不中断调度）
ThreadUtils.scheduleWithFixedDelaySafe(() -> syncService.sync(), 60, TimeUnit.SECONDS);

// 获取线程池监控
int queueSize = ThreadUtils.getAsyncQueueSize();
int activeCount = ThreadUtils.getAsyncActiveCount();
```

### 6.6 BeanUtils 对象操作

```java
// 属性复制（两个对象3个以上相同属性推荐使用）
BeanUtils.copyProperties(source, target);

// 对象转Map
Map<String, Object> map = BeanUtils.toMap(user);

// Map填充到对象
SysUser user = BeanUtils.fillBeanWithMap(map, SysUser.class);
```

### 6.7 CryptoUtil 加解密

```java
// ==== 对称加密（AES/GCM - 推荐安全版本） ====
String key = CryptoUtil.generateSymmetricKeyStr(CryptoUtil.SymmetricAlgorithmType.AES_GCM);
String encrypted = CryptoUtil.encryptStr(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, "明文数据");
String decrypted = CryptoUtil.decryptStr(CryptoUtil.SymmetricAlgorithmType.AES_GCM, key, encrypted);

// ==== 非对称加密（RSA OAEP - 推荐） ====
KeyPair keyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP);
String pubPem = CryptoUtil.getPublicKeyPem(keyPair.getPublic());
String priPem = CryptoUtil.getPrivateKeyPem(keyPair.getPrivate());
String enc = CryptoUtil.encryptStr(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, pubPem, "明文");
String dec = CryptoUtil.decryptStr(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, priPem, enc);

// ==== SM2 国密非对称加密 ====
KeyPair sm2KeyPair = CryptoUtil.generateKeyPair(CryptoUtil.AsymmetricAlgorithmType.SM2);
byte[] sm2Enc = CryptoUtil.encrypt(CryptoUtil.AsymmetricAlgorithmType.SM2, sm2KeyPair.getPublic(), "明文".getBytes());

// ==== 哈希 ====
String sha256Hash = CryptoUtil.hashStr(CryptoUtil.HashAlgorithmType.SHA256, "Hello World");
String sm3Hash = CryptoUtil.hashStr(CryptoUtil.HashAlgorithmType.SM3, "Hello World");

// ==== BCrypt 密码哈希 ====
String hashed = CryptoUtil.hashPassword("用户密码");
boolean match = CryptoUtil.checkPassword("用户输入密码", hashed);
```

### 6.8 SignUtils 签名

```java
// API签名串生成（排除敏感字段，按参数名ASCII排序）
String signStr = SignUtils.generateSignStr(requestDTO, "secret", "password");

// HMAC-SHA256 Web签名
String signature = SignUtils.signWeb("待签名消息体", "秘钥");
boolean valid = SignUtils.verifyWeb("原消息体", signature, "秘钥");

// RSA/SM2 数字签名
byte[] sig = SignUtils.sign(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, privateKey, data);
boolean ok = SignUtils.verify(CryptoUtil.AsymmetricAlgorithmType.RSA_OAEP, publicKey, data, sig);
```

### 6.9 LockService 分布式锁

> 示例来源：[`RedissonLockController`](simple-common-test/src/main/java/com/simple/common/test/controller/RedissonLockController.java:23)

```java
@Autowired
private RedissonLockService lockService;

// 可重入锁 - 扣库存（来自 RedissonLockController）
@PostMapping("/send1")
public R<Object> sendSms1(String key) {
    DefaultFunction function = () -> {
        String str = stringRedisTemplate.opsForValue().get("stock");
        AssertUtils.notEmpty(str, "库存不存在");
        int stock = Integer.parseInt(str);
        if (stock > 0) {
            int realStock = stock - 1;
            stringRedisTemplate.opsForValue().set("stock", realStock + "");
            log.info("扣减库存成功！，剩余库存：{}", realStock);
        } else {
            log.info("扣减失败！库存不足！");
        }
    };
    lockService.lock(key, function);
    return R.ok();
}

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

### 6.10 CycleService 循环任务

> 示例来源：[`DefaultCycleService`](simple-common-test/src/main/java/com/simple/common/test/service/DefaultCycleService.java:17) 和 [`CycleController`](simple-common-test/src/main/java/com/simple/common/test/controller/CycleController.java:23)

```java
// 1. 定义循环任务服务（继承 AbsCycleService 或其子类）
@Slf4j
@Component
public class DefaultCycleService extends AbsEventCycleService<DataDemo> {

    @Override
    public Class<DataDemo> getEventClass() {
        return DataDemo.class;
    }

    @Override
    protected Boolean handler(DataDemo runBody, Map<String, Object> parameters) {
        // 模拟http查单请求
        log.debug("查询中。。。。");
        return false; // 返回false触发下次重试
    }

    @Override
    protected void ok(DataDemo runBody, Map<String, Object> parameters) {
        log.debug("正在执行查询成功逻辑");
    }

    @Override
    protected void more(DataDemo runBody, Map<String, Object> parameters) {
        log.debug("查询已达最大次数");
    }

    @Override
    protected void error(DataDemo runBody, Map<String, Object> parameters) {
        log.debug("业务异常，查询失败");
    }
}

// 2. 调用循环任务（来自 CycleController）
@PostMapping("select")
public R<Object> select() {
    DataDemo dataDemo = new DataDemo().setDemoName2("测试数据1").setDemoName2("测试数据2");
    // 累加延迟：最多5次，基础间隔1秒
    // 第1次：立即，第2次：1秒后，第3次：2秒后，第4次：3秒后，第5次：4秒后
    defaultCycleService.runAccumulate(dataDemo, 5, 1);
    return R.ok();
}
```

### 6.11 IPUtils IP地址获取

```java
// 获取客户端IP
String clientIp = IPUtils.getIpAddr();

// 获取本机内网IP
String intranetIp = IPUtils.getIntranetIp();
```

### 6.12 Base64Utils

```java
// 编码
String encoded = Base64Utils.encode("文本内容");

// 解码
String decoded = Base64Utils.decodeStr(encoded);
byte[] bytes = Base64Utils.decode(encoded);
```

### 6.13 QRCodeUtils 二维码

> 示例来源：[`QRCodeAndThreadController`](simple-common-test/src/main/java/com/simple/common/test/controller/QRCodeAndThreadController.java:29)

```java
// 单个生成（输出到浏览器）
QRCodeUtils.create(text, null, text);

// 批量生成并ZIP下载
QRCodeUtils.createZip(null, size);

// 获取二维码字节数组
byte[] pngBytes = QRCodeUtils.create(300, 300, content, logoBase64, bottomText);
```

### 6.14 RecursiveUtils 树形结构

```java
// 自动识别顶级节点构建树
List<Tree<String>> tree = RecursiveUtils.get(departmentList);

// 从固定根节点构建树
List<Tree<String>> tree = RecursiveUtils.getFromRoot(menuList);

// 从指定根节点构建树
List<Tree<String>> tree = RecursiveUtils.getFromRoot(menuList, "serial", "0");
```

### 6.15 Aviator 表达式引擎

```java
// 执行表达式
Map<String, Object> vars = new HashMap<>();
vars.put("var1", 1);
vars.put("var2", 2);
vars.put("var3", 2);
vars.put("sum", 10);
Object result = AviatorUtils.run("(var1 + var2 - var3) * sum", vars);
// 结果：10
```

## 7. 扩展点与自定义方式

### 7.1 自定义线程池配置

通过 `application.yaml` 配置 `simple.thread-pool` 前缀属性即可覆盖默认值：

```yaml
simple:
  thread-pool:
    async-core-pool-size: 32
    async-max-pool-size: 64
    async-queue-capacity: 5000
    scheduled-core-pool-size: 8
```

如需完全自定义线程池实现，实现 `ThreadService` 接口并注册为 Spring Bean（覆盖 `DefaultThreadService`）：

```java
@Service
public class MyThreadService implements ThreadService, InitializingBean {
    // 实现 getExecutor()、getAsyncExecutor() 等方法
}
```

### 7.2 自定义 CycleService 实现

继承 `AbsCycleService<T>` 并实现抽象方法：

```java
@Service
public class OrderStatusCheckService extends AbsCycleService<OrderQuery> {

    @Override
    public Class<OrderQuery> getEventClass() {
        return OrderQuery.class;
    }

    @Override
    protected Boolean handler(OrderQuery runBody, Map<String, Object> parameters) {
        Order order = orderService.findById(runBody.getOrderId());
        if (order.isPaid()) {
            return true;  // 成功，停止循环
        }
        return false;  // 失败，触发下次重试
    }

    @Override
    protected void ok(OrderQuery runBody, Map<String, Object> parameters) {
        log.info("订单 {} 已支付", runBody.getOrderId());
    }

    @Override
    protected void more(OrderQuery runBody, Map<String, Object> parameters) {
        log.warn("订单 {} 查询已达最大次数", runBody.getOrderId());
    }

    @Override
    protected void error(OrderQuery runBody, Map<String, Object> parameters) {
        log.error("订单 {} 查询异常", runBody.getOrderId());
    }
}
```

### 7.3 自定义 LockService 实现

实现 `LockService` 接口（或继承 `AbsLockService`），框架默认在 `simple-common-redis` 模块提供 `RedissonLockService` 实现。如需自定义：

```java
@Service
public class MyLockService extends AbsLockService {

    @Override
    public void lock(String key, DefaultFunction function) {
        String lockKey = getKey("myMethod", key);
        // 自定义锁实现
    }

    @Override
    public Object lockHaveValue(String key, ReturnValueFunction function) {
        String lockKey = getKey("myMethod", key);
        // 自定义锁实现
        return null;
    }

    @Override
    public void fairLock(String key, DefaultFunction function) {
        String lockKey = getKey("myMethod", key);
        // 自定义公平锁实现
    }
}
```

### 7.4 自定义 CoreLoginUserService 实现

实现 `CoreLoginUserService` 接口，提供当前登录用户ID获取逻辑：

```java
@Component
public class MyLoginUserService implements CoreLoginUserService {
    @Override
    public String getUserId() {
        // 从 SecurityContext 或 ThreadLocal 中获取用户ID
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

### 7.5 自定义 Aviator 函数

继承 `DefAviatorFunction` 并注册为 `@Component`，配置 `simple.aviator.open=true` 后自动加载：

```java
@Component
public class MyAviatorFunction extends DefAviatorFunction {

    @Override
    public String getName() {
        return "myFunc";
    }

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1, AviatorObject arg2) {
        Number a = FunctionUtils.getNumberValue(arg1, env);
        Number b = FunctionUtils.getNumberValue(arg2, env);
        return AviatorDecimal.valueOf(a.doubleValue() + b.doubleValue());
    }
}
```

在表达式中使用：`AviatorUtils.run("myFunc(var1, var2)", vars)`

### 7.6 自定义异常枚举

实现 `AbstractException` 接口，定义业务异常码：

```java
@Getter
@AllArgsConstructor
public enum BusinessExceptionEnum implements AbstractException {
    USER_NOT_FOUND("1001", "用户不存在"),
    TOKEN_INVALID("1002", "Token无效"),
    NO_PERMISSION("1003", "权限不足");

    private final String code;
    private final String message;
}
```

配合 `AssertUtils` 使用：

```java
AssertUtils.notEmpty(user, BusinessExceptionEnum.USER_NOT_FOUND);
AssertUtils.notEmpty(user, BusinessExceptionEnum.USER_NOT_FOUND, "用户ID[{}]不存在", userId);
```

### 7.7 自定义责任链处理器

1. 定义处理器接口继承 `BasProcessService`
2. 创建枚举类实现 `DefaultKindProcess`
3. 实现具体处理器类

```java
// 1. 枚举定义
public enum AuthKindProcess implements DefaultKindProcess {
    CHECK_TOKEN(1, "Token校验"),
    CHECK_ROLE(2, "角色校验");

    private final int order;
    private final String msg;

    @Override
    public boolean isExecute() { return true; }
    @Override
    public Integer getOrdered() { return order; }
    @Override
    public String getMsg() { return msg; }
}

// 2. 处理器实现
@Component
public class CheckTokenProcess implements BasProcessService {
    @Override
    public DefaultKindProcess getProcess() {
        return AuthKindProcess.CHECK_TOKEN;
    }
}
```

## 8. 注意事项

### 8.1 线程安全

- **`IdUtils` 雪花算法**：`Snowflake` 实例为 `static final`，线程安全。基于本机IP计算 `workerId` 和 `datacenterId`，同网段不同IP不会碰撞。
- **`ThreadUtils`**：每次调用均从 Spring 容器获取最新 `ThreadService` Bean，不进行静态缓存，避免容器刷新后引用失效。
- **`DefaultThreadService`**：使用 `AtomicInteger` 生成线程名后缀，线程安全。两个线程池均设置 `UncaughtExceptionHandler`，异常不会静默吞掉。
- **`RsaUtils`**（已废弃）：内部使用 `ConcurrentHashMap` 缓存 RSA 实例，线程安全。
- **`BeanUtils.copyProperties`**：基于 hutool BeanUtil，非线程安全场景下使用局部变量无问题。注意空值会覆盖目标对象的非空值。

### 8.2 性能建议

- **线程池配置**：I/O 密集型任务建议 `asyncCorePoolSize = CPU核心数 * 2 + 1`；CPU 密集型任务建议 `asyncCorePoolSize = CPU核心数 + 1`。
- **定时任务**：使用 `scheduleWithFixedDelaySafe` 替代 `scheduleWithFixedDelay`，避免单次异常导致调度中断。
- **JSON 转换**：`JsonUtils.toList` 使用 fastjson2，性能优于 hutool；`toJsonStr` 和 `toJsonObj` 使用 hutool JSONUtil。
- **二维码批量生成**：`QRCodeUtils.createZip` 内部使用 `ThreadUtils.supplyAsync` 异步并行生成，批量场景性能良好。
- **Aviator 表达式**：`AviatorEvaluator.compile` 内部有缓存，重复执行相同表达式性能高。

### 8.3 安全注意

- **加解密算法选择**：推荐使用 `AES_GCM`、`SM4_GCM`（对称）、`RSA_OAEP`、`SM2`（非对称）、`SHA256`、`SHA512`、`SM3`（哈希）。不安全算法（`AES_CBC`、`DES_CBC`、`SM4_CBC`、`RSA_PKCS1`、`MD5`）仅用于旧系统兼容，使用时会打印警告日志。
- **密码存储**：使用 `CryptoUtil.hashPassword`（BCrypt）加密密码，禁止使用 MD5 存储密码。
- **XSS 防护**：对外部输入使用 `@XssSafe` 注解校验，或使用 `XssUtils.clean` 过滤 HTML 内容。
- **签名验证**：Web 请求使用 `SignUtils.signWeb`（HMAC-SHA256），服务端间通信使用 `SignUtils.sign`（RSA/SM2 数字签名）。
- **敏感信息日志**：密码、token、密钥等敏感信息禁止在 `info`/`warn`/`error` 级别日志中打印，使用 `debug` 级别。

### 8.4 常见问题

1. **`ThreadService 未加载` 异常**：`ThreadUtils` 在 Spring 容器初始化完成前调用会抛出此异常。确保在 Spring Bean 初始化完成后使用（如 `@PostConstruct`、`InitializingBean.afterPropertiesSet` 中慎用）。

2. **`CycleService` 任务不执行**：检查 `handler` 方法返回值。返回 `false` 会触发下次重试，返回 `true` 才会调用 `ok` 回调并停止循环。

3. **`BeanUtils.copyProperties` 空值覆盖**：源对象的 null 值会覆盖目标对象的非空值。如需忽略空值，需手动处理或使用其他方式。

4. **`CryptoUtil` IV 向量**：对称加密的 GCM 模式需要 12 字节 IV，CBC 模式需要 16 字节（AES/SM4）或 8 字节（DES）IV。IV 通常附加在密文前部传输。

5. **`DefaultExceptionHandler` 返回格式**：所有异常统一返回 `R<Object>` 格式，`DefaultException` 返回其 code 和 message，其他异常返回 500 + "系统繁忙，请稍后再试"。

6. **`AviatorInit` 不生效**：需在 `application.yaml` 中配置 `simple.aviator.open: "true"`，且自定义函数需继承 `DefAviatorFunction` 并注册为 `@Component`。

7. **`RecursiveUtils` 字段名要求**：被递归的对象必须包含 `id` 和 `parentId` 字段，子集合字段名为 `list`，排序字段默认为 `serial`（可通过参数自定义）。

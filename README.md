# Simple-Common 企业级开发框架

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![License](https://img.shields.io/badge/License-Apache--2.0-blue)

> **开源协议**：本项目采用 Apache License 2.0 开源协议，可自由使用、修改和分发。
>
> **项目性质**：这是一个完全开源的通用技术框架，适用于各类 Java 项目开发。

## 🚀 项目简介

**Simple-Common** 是一个基于 **Spring Boot 3.3 + Java 17** 构建的模块化企业级开发框架，提供认证授权、分布式缓存、消息队列、事件总线、实时通信等完整的企业级能力。

### ✨ 核心特性

#### 🔐 安全与认证
- **统一加密工具 CryptoUtil** - 枚举切换AES/SM4/RSA/SM2等国密算法，自动安全提示，BCrypt密码哈希
- **OAuth2认证授权** - 完整的OAuth2协议实现，支持JWT Token、角色权限、白名单机制
- **灵活缓存策略** - Auth模块Redis/Local一键切换，零代码修改适配不同部署环境

#### 💾 缓存与性能
- **两级缓存架构** - Caffeine(本地) + Redis(分布式)，智能读写策略优化
- **分布式锁服务** - Redisson实现可重入锁/公平锁/闭锁/信号量，支持锁续期
- **线程池管理** - 异步执行、定时任务、延迟调度，统一线程资源管理

#### 📨 消息与事件
- **消息队列防重消费** - RabbitMQ + Redisson分布式锁，支持锁续期、僵尸锁恢复、完成校验
- **事件总线双模式** - 同步(立即执行) / 异步(RabbitMQ分发)，支持跨系统事件、延迟事件
- **责任链模式实践** - Auth/RabbitMQ/SMS/Logs多模块采用，轻松扩展自定义逻辑

#### 🌐 通信与集成
- **WebSocket实时通信** - 静态工具类WebSocketUtils，支持点对点/广播消息推送
- **日志中心** - TCP + Protobuf高性能传输，client/proto/server三模块架构，Fire-and-Forget模式，追求极致的性能
- **短信服务** - 阿里云SMS集成，多层防刷机制(IP限流+频次控制+黑名单)

#### 📊 数据与文档
- **Excel处理** - EasyExcel + Apache POI双引擎，支持大数据量导出、模板填充
- **Word文档生成** - poi-tl引擎，支持动态模板替换、表格生成
- **附件管理** - MinIO/AWS S3对象存储，S3协议兼容
- **API文档** - SpringDoc OpenAPI 3.0，自动生成Swagger UI

#### ⚙️ 运维与监控
- **定时任务** - XXL-JOB集成，支持分布式任务调度、失败重试
- **AI集成** - Spring AI Alibaba，支持通义千问等大模型接入
- **MyBatis-Plus增强** - 通用CRUD、分页插件、自动填充

### 🛠️ 技术栈

| 类别 | 技术选型 |
|------|----------|
| 基础框架 | Spring Boot 3.3 + Java 17 |
| ORM框架 | MyBatis-Plus 3.5.7 |
| 缓存 | Redis (Redisson 3.27.0) + Caffeine |
| 消息队列 | RabbitMQ (Spring AMQP 3.1.6) |
| 实时通信 | WebSocket (Spring WebSocket) |
| 认证授权 | OAuth2 + JWT |
| 日志中心 | Netty + Protobuf (TCP协议) |
| 文档生成 | SpringDoc OpenAPI 2.5.0 |
| Excel处理 | EasyExcel 3.3.4 + Apache POI 5.2.5 |
| Word生成 | poi-tl 1.12.2 |
| 对象存储 | MinIO / AWS S3 |
| 定时任务 | XXL-JOB 2.4.1 |
| AI集成 | Spring AI Alibaba 1.0.0.2 |
| 工具类库 | Hutool 5.8.27 |

### 🎯 设计理念

- **模块化设计** - 各功能模块独立封装，按需引入，减少依赖冗余
- **高度可扩展** - 继承/接口/责任链轻松扩展，默认实现前缀`Default`
- **无侵入集成** - 现有项目直接引入使用，无需修改原有代码
- **统一规范** - 命名/service/manager/Process后缀标准化，异常/响应统一封装
- **性能优化** - 本地缓存/线程池/异步处理/防重消费等多维度优化

### 📦 适用场景

- **新项目快速启动** - 直接使用框架提供的功能模块，快速搭建项目
- **旧项目功能增强** - 按需引入特定模块，无侵入式增强项目功能
- **微服务架构** - 模块化设计支持微服务拆分，单体应用可平滑过渡到微服务
- **政府/国企项目** - 完整国密算法支持(SM2/SM3/SM4)，符合安全合规要求

---

## 命名规范说明

框架遵循统一的命名规范，便于理解和使用：

- **service后缀**：表示为本模块对外提供的核心功能接口。默认实现前缀加 `Default`。
  - 示例：`LoginService` → `DefaultLoginService`、`SmsService` → `AliSmsService`
  
- **manager后缀**：表示为本模块内部提供支撑的接口。默认实现前缀加 `Default`。
  - 示例：`TokenManager` → `DefaultTokenManager`、`WhiteManager` → `DefaultWhiteManager`
  
- **Process后缀**：责任链处理接口，配合枚举使用。
  - 示例：`AuthProcess`、`LoginErrorProcess`、`CheckSmsProcess`
  
- **Abs前缀**：抽象基类，提供通用实现逻辑。
  - 示例：`AbsLoginManager`、`AbsSmsService`、`AbsLogsSaveManager`

---

## 模块功能介绍

### 1. simple-common-core（核心模块）

核心基础模块，提供工具类、异常处理、响应封装等基础功能，是所有其他模块的基础依赖。

#### 核心功能

| 功能分类 | 工具类/组件 | 说明 |
|---------|-----------|------|
| 线程管理 | `ThreadUtils` | 线程池管理、异步执行、定时任务 |
| JSON处理 | `JsonUtils` | JSON序列化/反序列化 |
| 对象转换 | `BeanUtils` | 对象属性复制、转换 |
| 日期处理 | `DateUtils` | 日期格式化、计算 |
| ID生成 | `IdUtils` | UUID、雪花算法ID生成 |
| 断言工具 | `AssertUtils` | 参数校验、业务断言 |
| 表达式引擎 | `AviatorUtils` | Aviator表达式引擎封装 |
| **加密工具** | **`CryptoUtil`** | **统一加密工具，支持多种算法枚举切换** |
| 异常处理 | `DefaultException`、`DefaultExceptionHandler` | 统一异常体系 |
| 响应封装 | `R` | 统一响应对象 |
| 分布式锁 | `LockService`、`AbsLockService` | 分布式锁服务接口 |
| 循环任务 | `CycleService`、`AbsCycleService` | 循环任务处理服务 |
| 责任链基础 | `BasProcessService`、`DefaultKindProcess` | 责任链模式基础接口 |

#### 🌟 亮点特性：统一加密工具 CryptoUtil

**CryptoUtil 是框架的核心亮点之一！**通过枚举直接切换加密方式，无需修改代码逻辑。

**支持的加密算法：**

| 算法类型 | 枚举值 | 安全性 | 适用场景 |
|---------|--------|--------|----------|
| **对称加密** | `AES_GCM` | ✅ 安全 | **推荐使用**，GCM模式自带认证 |
| | `SM4_GCM` | ✅ 安全 | **国密标准**，政府项目推荐 |
| | `AES_CBC` | ⚠️ 不安全 | 仅旧系统兼容，会打印警告 |
| | `DES_CBC` | ⚠️ 不安全 | 已废弃，不建议使用 |
| | `SM4_CBC` | ⚠️ 不安全 | 仅旧系统兼容 |
| **非对称加密** | `RSA_OAEP` | ✅ 安全 | **推荐使用**，OAEP填充更安全 |
| | `SM2` | ✅ 安全 | **国密标准**，政府项目推荐 |
| | `RSA_PKCS1` | ⚠️ 不安全 | 仅旧系统兼容 |
| **哈希算法** | `SHA256` | ✅ 安全 | 通用哈希 |
| | `SHA512` | ✅ 安全 | 更高安全性 |
| | `SM3` | ✅ 安全 | **国密标准** |
| | `MD5` | ⚠️ 不安全 | 仅用于校验，不用于安全场景 |
| **密码哈希** | `BCrypt` | ✅ 安全 | **专门用于密码存储**，自带盐值 |

**使用示例：**

```java
// 1. 对称加密（通过枚举切换算法）
SymmetricAlgorithmType algorithm = SymmetricAlgorithmType.AES_GCM; // 或 SM4_GCM

// 生成密钥
byte[] key = CryptoUtil.generateSymmetricKey(algorithm);
String keyStr = CryptoUtil.generateSymmetricKeyStr(algorithm); // Base64编码

// 加密
String plainText = "Hello, World!";
String encrypted = CryptoUtil.encryptStr(algorithm, keyStr, plainText);

// 解密
String decrypted = CryptoUtil.decryptStr(algorithm, keyStr, encrypted);

// 2. 非对称加密（RSA/SM2）
AsymmetricAlgorithmType asymAlgo = AsymmetricAlgorithmType.RSA_OAEP; // 或 SM2

// 生成密钥对
KeyPair keyPair = CryptoUtil.generateKeyPair(asymAlgo);
PublicKey publicKey = keyPair.getPublic();
PrivateKey privateKey = keyPair.getPrivate();

// 公钥加密
String encrypted = CryptoUtil.encryptStr(asymAlgo, publicKeyPem, plainText);

// 私钥解密
String decrypted = CryptoUtil.decryptStr(asymAlgo, privateKeyPem, encrypted);

// 3. 哈希计算
HashAlgorithmType hashAlgo = HashAlgorithmType.SHA256; // 或 SM3、SHA512
byte[] hash = CryptoUtil.hash(hashAlgo, data.getBytes());

// 4. 密码哈希（BCrypt - 推荐）
String password = "user_password";
String hashedPassword = CryptoUtil.hashPassword(password); // 自动加盐
boolean matches = CryptoUtil.checkPassword(password, hashedPassword); // 验证
```

**优势：**

- ✅ **枚举切换**：只需更改枚举值即可切换算法，无需修改业务代码
- ✅ **安全提示**：使用不安全算法时自动打印警告日志，提醒迁移
- ✅ **国密支持**：完整支持SM2、SM3、SM4国密算法
- ✅ **统一API**：所有算法使用统一的接口，降低学习成本
- ✅ **旧系统兼容**：保留不安全算法但标记警告，平滑迁移

#### ThreadUtils 使用示例

```java
// 异步执行任务(无返回值)
ThreadUtils.runAsync(() -> {
    // 业务逻辑
});

// 异步执行任务(有返回值)
CompletableFuture<String> future = ThreadUtils.supplyAsync(() -> {
    // 业务逻辑
    return "result";
});

// 延迟执行任务（5秒后执行）
ThreadUtils.schedule(() -> {
    // 业务逻辑
}, 5, TimeUnit.SECONDS);

// 定时执行任务（每60秒执行一次，固定延迟）
ThreadUtils.scheduleWithFixedDelay(() -> {
    // 业务逻辑
}, 0, 60, TimeUnit.SECONDS);

// 定时执行任务（安全版，自动捕获异常）
ThreadUtils.scheduleWithFixedDelaySafe(() -> {
    // 业务逻辑，即使抛出异常也不会中断后续调度
}, 0, 60, TimeUnit.SECONDS);
```

#### AssertUtils 使用示例

```java
// 参数校验
AssertUtils.isTrue(user != null, "用户不存在");
AssertUtils.isTrue(password.equals(dbPassword), LoginException.PASSWORD_ERROR);

// 带参数的异常信息
AssertUtils.isTrue(hasPermission, LoginException.INSUFFICIENT_PERMISSIONS, "用户ID: {}", userId);
```

#### R 响应封装使用示例

```java
// 成功响应（带数据）
return R.ok(data);

// 成功响应（无数据）
return R.ok();

// 失败响应
return R.error("操作失败");
```

---

### 2. simple-common-auth（认证授权模块）

认证授权模块，分为客户端和服务端两个子模块，提供完整的认证授权解决方案。

#### 2.1 simple-common-auth-client（客户端模块）

客户端认证模块，用于资源服务端的认证拦截和权限校验。

##### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| Token管理 | `TokenManager` | Token解析、验证、创建 |
| 白名单管理 | `WhiteManager` | URL白名单配置 |
| 登录信息管理 | `LoginInfoManager` | 获取当前登录用户信息 |
| 签名校验 | `SignManager` | 接口签名校验 |
| **签名密钥管理** | **`SignSecretManager`** | **签名密钥统一管理，支持动态更新** |
| 缓存管理 | `CacheManager` | 认证相关缓存管理 |
| CSRF防御 | `CsrfService` | CSRF Token生成和验证 |
| 责任链处理 | `AuthProcess` | 认证拦截责任链处理接口 |

##### 架构设计

**认证拦截流程：**

```
请求到达
   ↓
OPTIONS请求？→ 是 → 直接放行
   ↓ 否
获取IP和请求路径
   ↓
全局登录开关关闭？→ 是 → 直接放行
   ↓ 否
IP白名单校验
   ↓
URL白名单匹配？→ 是 → 直接放行
   ↓ 否
提取Token（从Header或Cookie）
   ↓
Token为空？→ 是 → 抛出未登录异常
   ↓ 否
执行认证责任链
   ├─ CheckTokenAuthProcess: Token合法性校验
   ├─ CheckRoleAuthProcess: 角色权限校验
   └─ CheckScopeAuthProcess: 授权范围校验
   ↓
所有校验通过
   ↓
将用户信息存入ThreadLocal
   ↓
放行请求
   ↓
请求完成后清理ThreadLocal
```

**重要说明：**

- ✅ **责任链模式**：通过`AuthProcess`接口实现可扩展的认证流程
- ✅ **白名单机制**：支持URL白名单和IP白名单
- ✅ **流式API**：通过`ClientAuthInfo`提供流畅的配置方式
- ✅ **ThreadLocal管理**：自动管理用户上下文生命周期

#### 🌟 亮点特性：Redis与本地缓存自由切换

**Auth模块支持通过配置项一键切换缓存类型！**无需修改代码，只需调整配置文件。

**配置方式：**

```yaml
simple:
  auth:
    # 缓存类型：REDIS（默认）或 LOCAL
    cache-type: REDIS  # 或 LOCAL
```

**CacheTypeEnum 枚举定义：**

```java
public enum CacheTypeEnum {
    REDIS,  // Redis缓存（分布式环境推荐）
    LOCAL   // 本地缓存（单机环境推荐）
}
```

**使用场景：**

| 缓存类型 | 适用场景       | 优势 | 劣势           |
|---------|------------|------|--------------|
| **REDIS** | 分布式部署、多实例  | 数据共享、一致性高 | 依赖Redis，性能略低 |
| **LOCAL** | 分布式部署、单机部署 | 速度快 | 依赖于event同步数据 |

**切换示例：**

```yaml
# 开发环境：使用本地缓存，提高响应速度
simple:
  auth:
    cache-type: LOCAL

# 生产环境：使用Redis缓存，支持分布式
simple:
  auth:
    cache-type: REDIS
```

**优势：**

- ✅ **零代码修改**：只需修改配置文件即可切换
- ✅ **灵活适配**：根据部署环境选择最优方案
- ✅ **性能优化**：一般生产环境用Redis，极致性能使用LOCAL
- ✅ **平滑迁移**：从单机到分布式无需重构代码

##### 扩展指南

**自定义认证处理逻辑**：实现 `AuthProcess` 接口

```java
@Component
public class CustomAuthProcess implements AuthProcess {
    
    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_TOKEN; // 或其他自定义枚举
    }
    
    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, 
                       String token, String path, String ipAddr) {
        // 自定义认证逻辑
    }
}
```

**自定义白名单配置**：继承 `AbsClientAuthConfig`

```java
@Configuration
public class MyClientAuthConfig extends AbsClientAuthConfig {
    
    @Override
    protected void configure(ClientAuthInfo clientAuthInfo) {
        
        // ========== 全局开关 ==========
        // 开启所有请求需要登录
        clientAuthInfo.openLogin();
        
        // 开启所有请求需要鉴权（验证Scope）
        clientAuthInfo.openAuthentication();
        
        // 开启单设备登录（同一账号只能在一处登录）
        clientAuthInfo.openOneLogin();
        
        // 开启IP白名单校验
        clientAuthInfo.openIPWhitelist();
        
        // 标记为客户端应用
        clientAuthInfo.anyClient();
        
        // 添加访问本项目需要的Scope范围
        clientAuthInfo.addScope("user:read", "order:manage");
        
        // ========== URL权限配置（流式API）==========
        
        // 1. 放行公开接口（无需登录）
        clientAuthInfo.antMatchers("/api/public/**").permitAll()
                      .antMatchers("/api/login").permitAll()
                      .antMatchers("/doc.html").permitAll();
        
        // 2. IP白名单限制（只允许指定IP访问）
        clientAuthInfo.antMatchers("/api/internal/**")
                      .onlyIp("192.168.1.100", "10.0.0.1");
        
        // 3. 角色限制（只有指定角色可访问）
        clientAuthInfo.antMatchers("/api/admin/**")
                      .onlyRole("admin", "super_admin");
        
        // 4. 反向角色限制（排除指定角色，其他角色可访问）
        clientAuthInfo.antMatchers("/api/user/**")
                      .roleByRestricted("tourists");
    }
}
```

**ClientAuthInfo 核心方法说明：**

| 方法 | 说明 | 使用场景 |
|------|------|----------|
| `openLogin()` | 开启登录校验 | 所有接口都需要用户登录 |
| `openAuthentication()` | 开启Scope鉴权 | 微服务间调用需要验证授权范围 |
| `openOneLogin()` | 开启单设备登录 | 防止账号共享，提高安全性 |
| `openIPWhitelist()` | 开启IP白名单 | 内网接口只允许特定IP访问 |
| `anyClient()` | 标记为客户端应用 | 区分服务端和客户端应用 |
| `addScope(...)` | 添加所需Scope | 声明本服务需要的权限范围 |
| `antMatchers(url).permitAll()` | 放行URL | 公开接口无需认证 |
| `antMatchers(url).onlyIp(...)` | IP白名单 | 限制特定IP访问 |
| `antMatchers(url).onlyRole(...)` | 角色限制 | 只允许指定角色访问 |
| `antMatchers(url).roleByRestricted(...)` | 反向角色限制 | 排除某些角色，其他可访问 |

**获取当前登录用户**：使用 `LoginUserUtils`

```java
// 获取当前登录用户临时信息（推荐）
UserTemporary user = LoginUserUtils.getUserTemporary();
String userId = user.getUserId();
String username = user.getUsername();
Set<String> roles = user.getRoles();
Set<String> scopes = user.getScopes();

// 获取扩展信息(在登录的时候构建AbsUserDetails里面设置)
Object extension = user.getExtension();
```

#### 🌟 亮点特性：签名密钥统一管理

**SignSecretManager 提供签名密钥的统一管理！**与 JWT 密钥管理保持一致的设计模式。

**核心优势：**
- ✅ **统一存储** - 密钥存储在 CacheManager 中，支持 Redis/Local 切换
- ✅ **自动初始化** - 应用启动时自动生成初始密钥
- ✅ **动态更新** - 运行时可动态更新密钥，无需重启
- ✅ **职责分离** - 密钥管理与签名逻辑分离

**服务端使用示例：**

```java
@RestController
@RequestMapping("/admin/security")
public class SignSecurityAdminController {
    
    @Autowired
    private SignSecretManager signSecretManager;
    
    /**
     * 生成新的签名密钥
     * POST /admin/security/sign/generate
     */
    @PostMapping("/sign/generate")
    public R<String> generateSignSecret() {
        // 生成新密钥（建议使用 UUID 或随机字符串）
        String newSecret = UUID.randomUUID().toString().replace("-", "");
        
        // 添加并广播到所有客户端
        signSecretManager.addSecret(newSecret);
        
        log.info("签名密钥已生成并同步到所有客户端");
        return R.ok(newSecret);
    }
    
    /**
     * 手动更新签名密钥
     * POST /admin/security/sign/update
     */
    @PostMapping("/sign/update")
    public R<Void> updateSignSecret(@RequestBody String newSecret) {
        // 校验密钥非空
        if (newSecret == null || newSecret.isEmpty()) {
            return R.error("密钥不能为空");
        }
        
        // 更新密钥并广播
        signSecretManager.addSecret(newSecret);
        
        log.info("签名密钥已手动更新");
        return R.ok();
    }
    
    /**
     * 获取当前签名密钥（用于调试）
     * GET /admin/security/sign/current
     */
    @GetMapping("/sign/current")
    public R<String> getCurrentSignSecret() {
        String currentSecret = signSecretManager.getCurrentSecret();
        return R.ok(currentSecret);
    }
}
```

**客户端使用示例：**

```java
@Service
public class ApiSecurityService {
    
    @Autowired
    private SignManager signManager;
    
    /**
     * 对请求参数进行签名
     */
    public String signRequest(Object requestData) {
        // 从 SignManager 自动获取当前密钥
        String key = signManager.getKey();
        String message = SignUtils.generateSignStr(requestData, new String[]{});
        return SignUtils.signWeb(message, key);
    }
    
    /**
     * 验证请求签名
     */
    public boolean verifyRequest(Object requestData, String signature) {
        String key = signManager.getKey();
        String message = SignUtils.generateSignStr(requestData, new String[]{});
        return SignUtils.verifyWeb(message, signature, key);
    }
}
```

**工作流程：**

```
1. 应用启动
   ↓
2. DefaultSignSecretManager.afterPropertiesSet()
   ↓
3. 检查密钥是否存在 → 不存在则自动生成
   ↓
4. 密钥存储到 CacheManager (auth:sign:secret:current)
   ↓
5. SignManager 签名/验证时从 SignSecretManager 获取密钥
   ↓
6. 管理员调用 addSecret() 更新密钥
   ↓
7. 发布 SecretEvent 事件（如果配置了 EventBus）
   ↓
8. 所有客户端接收事件并更新本地密钥缓存
```

**架构对比：**

| 组件 | 旧方案 | 新方案 |
|------|--------|--------|
| **密钥存储** | 内存变量 (currentKey) | CacheManager (Redis/Local) |
| **密钥生成** | DefaultSignManager.generated() | SignSecretManager 自动初始化 |
| **密钥更新** | putKey() 方法 | addSecret() 方法 |
| **密钥获取** | getKey() 方法 | getCurrentSecret() 方法 |
| **持久化** | ❌ 重启丢失 | ✅ 根据配置持久化 |
| **分布式同步** | ❌ 不支持 | ✅ 通过事件总线同步 |

**安全建议：**

- 🔒 **定期轮换**：建议每 90 天轮换一次签名密钥
- 🔒 **密钥长度**：推荐使用至少 32 位的随机字符串
- 🔒 **传输加密**：生产环境确保事件总线通信加密
- 🔒 **访问控制**：密钥更新接口需要管理员权限
- 🔒 **密钥格式**：避免使用特殊字符，建议使用 Base64 或十六进制字符串

**配置要求：**

如需实现密钥自动同步，确保项目中已引入事件总线模块：

```yaml
simple:
  event:
    type: mq  # 或 sync，使用 RabbitMQ 或同步事件
```

**客户端启动行为：**

- ✅ **强制获取** - 客户端模式下，应用启动时必须从授权中心获取签名密钥
- ⚠️ **启动失败** - 如果获取失败（网络错误、返回空密钥、组件未加载等），应用将**终止启动**并抛出 `IllegalStateException`
- 🔒 **安全保证** - 确保所有客户端在启动时都拥有有效的签名密钥，避免接口签名验证失败
- 💡 **开发建议** - 本地开发时可暂时关闭客户端模式，或确保授权中心可用

**错误示例：**

```
2024-04-24 02:00:00 [main] ERROR c.s.c.a.c.i.SignSecretInitializer - 从授权中心获取签名密钥失败: 连接超时
2024-04-24 02:00:00 [main] ERROR o.s.boot.SpringApplication - Application run failed
java.lang.IllegalStateException: 从授权中心获取签名密钥失败: 连接超时
    at com.simple.common.auth.client.init.SignSecretInitializer.run(SignSecretInitializer.java:68)
    ...
```

**注意事项：**

- ⚠️ 密钥更新后，旧的签名将立即失效，需要客户端重新签名
- ⚠️ 建议在低峰期进行密钥轮换，避免影响业务
- ⚠️ 如果没有配置事件总线，需要重启所有客户端才能生效
- ⚠️ 签名密钥与 JWT 密钥是独立的，可以分别管理

---

#### 🌟 亮点特性：JWT 密钥管理

**JwtSecretManager 提供 JWT 密钥的统一管理！**支持服务端集中管理，客户端自动同步。

**核心优势：**
- ✅ **集中管理** - 服务端统一生成和管理 JWT 密钥
- ✅ **自动同步** - 通过事件总线自动同步到所有客户端
- ✅ **安全更新** - 支持运行时密钥轮换，无需重启服务
- ✅ **长度校验** - 强制要求密钥长度至少 64 位，保证安全性

**服务端使用示例：**

```java
@RestController
@RequestMapping("/admin/security")
public class SecurityAdminController {
    
    @Autowired
    private JwtSecretManager jwtSecretManager;
    
    /**
     * 生成新的 JWT 密钥
     * POST /admin/security/jwt/generate
     */
    @PostMapping("/jwt/generate")
    public R<String> generateJwtSecret() {
        // 生成符合安全要求的密钥（至少 64 位）
        String newSecret = jwtSecretManager.generateSecret();
        
        // 添加并广播到所有客户端
        jwtSecretManager.addSecret(newSecret);
        
        log.info("JWT 密钥已生成并同步到所有客户端");
        return R.ok(newSecret);
    }
    
    /**
     * 手动更新 JWT 密钥
     * POST /admin/security/jwt/update
     */
    @PostMapping("/jwt/update")
    public R<Void> updateJwtSecret(@RequestBody String newSecret) {
        // 校验密钥长度
        if (newSecret.length() < 64) {
            return R.error("密钥长度至少为 64 位");
        }
        
        // 更新密钥并广播
        jwtSecretManager.addSecret(newSecret);
        
        log.info("JWT 密钥已手动更新");
        return R.ok();
    }
}
```

**客户端自动同步机制：**

客户端通过监听 `SecretEvent` 事件自动接收密钥更新：

```java
@Component
public class SecretEventHandler {
    
    @Autowired
    private ClientAuthInfo clientAuthInfo;
    
    @EventListener
    public void onSecretChange(SecretEvent event) {
        String secret = event.getSecret();
        switch (event.getOperation()) {
            case ADD, UPDATE -> {
                if (clientAuthInfo.getClient()) {
                    // 更新 JWT 密钥
                    JJwtUtils.saveSecret(secret);
                    JwtUtils.saveSecret(secret);
                    log.debug("全局 JWT 密钥已更新");
                }
            }
        }
    }
}
```

**密钥更新流程：**

```
1. 管理员调用服务端 JwtSecretManager.addSecret()
   ↓
2. 服务端验证密钥长度（≥64 位）
   ↓
3. 服务端缓存密钥到本地
   ↓
4. 发布 SecretEvent 事件（携带新密钥）
   ↓
5. 所有客户端接收事件
   ↓
6. 客户端更新本地 JWT 密钥缓存
   ↓
7. ✅ 所有服务使用新密钥签发/验证 Token
```

**安全建议：**

- 🔒 **定期轮换**：建议每 90 天轮换一次 JWT 密钥
- 🔒 **密钥长度**：强制要求至少 64 位，推荐使用 128 位
- 🔒 **传输加密**：生产环境确保事件总线通信加密
- 🔒 **访问控制**：密钥更新接口需要管理员权限

**配置要求：**

确保项目中已引入事件总线模块以实现密钥同步：

```yaml
simple:
  event:
    type: mq  # 或 sync，使用 RabbitMQ 或同步事件
```

**客户端启动行为：**

- ✅ **强制获取** - 客户端模式下，应用启动时必须从授权中心获取 JWT 密钥
- ⚠️ **启动失败** - 如果获取失败（网络错误、返回空密钥等），应用将**终止启动**并抛出 `IllegalStateException`
- 🔒 **安全保证** - 确保所有客户端在启动时都拥有有效的 JWT 密钥，避免运行时认证失败
- 💡 **开发建议** - 本地开发时可暂时关闭客户端模式，或确保授权中心可用

**错误示例：**

```
2024-04-24 02:00:00 [main] ERROR c.s.c.a.c.i.JwtSecretInitializer - 从授权中心获取JWT密钥失败: 连接超时
2024-04-24 02:00:00 [main] ERROR o.s.boot.SpringApplication - Application run failed
java.lang.IllegalStateException: 从授权中心获取JWT密钥失败: 连接超时
    at com.simple.common.auth.client.init.JwtSecretInitializer.run(JwtSecretInitializer.java:63)
    ...
```

---

---

#### 🌟 亮点特性：Token 刷新并发优化

**DefaultLoginService 采用优化的 Token 刷新策略！**避免并发场景下的认证失败问题。

**核心优势：**
- ✅ **先加后删** - 先生成新 Token 并保存，再删除旧 Token
- ✅ **无时间窗口** - 消除旧 Token 删除与新 Token 保存之间的竞态条件
- ✅ **平滑过渡** - 确保用户在刷新过程中不会遇到认证失败

**Token 刷新流程（优化后）：**

```
1. 客户端携带 RefreshToken 请求刷新
   ↓
2. 服务端校验 RefreshToken 合法性
   ↓
3. 从缓存获取用户内省数据
   ↓
4. 构建新的 TokenData（生成新 jti/ati）
   ↓
5. 生成新的 AccessToken 和 RefreshToken
   ↓
6. 【关键】先保存新 Token 信息到缓存
   ↓
7. 【关键】再删除旧 Token 信息
   ↓
8. 返回新 Token 给客户端
   ↓
9. ✅ 刷新完成，无并发风险
```

**代码实现：**

```java
@Override
public Map<String, String> refresh(String refreshTokenStr) {
    // 1. 校验 RefreshToken
    Map<String, Object> payload = tokenManager.check(refreshTokenStr, true);
    
    // 2. 获取用户内省数据
    Map<Object, Object> userInfo = loginUserOperationManager.getUserInfo(jti);
    
    // 3. 构建新 Token 数据
    TokenData tokenData = new TokenData();
    tokenData.refresh(userInfo, jti, ati);
    
    // 4. 生成新 Token
    String accessToken = tokenManager.create(tokenData.getAccessTokenMap());
    String refreshToken = tokenManager.create(tokenData.getRefreshTokenMap());
    
    // 5. 【优化点】先保存新 Token，再删除旧 Token
    loginUserOperationManager.saveUserInfo(tokenData, false);  // 先保存
    loginUserOperationManager.loginOut(userId, jti);           // 后删除
    
    // 6. 返回新 Token
    return loginReturn;
}
```

**并发场景对比：**

| 场景 | 旧方案（先删后加） | 新方案（先加后删） |
|------|------------------|------------------|
| **刷新期间访问** | ❌ 可能认证失败 | ✅ 正常访问 |
| **多设备同时刷新** | ⚠️ 可能冲突 | ✅ 安全处理 |
| **网络延迟场景** | ❌ 高风险 | ✅ 低风险 |
| **用户体验** | ⚠️ 偶发失败 | ✅ 流畅无感 |

---

#### 🌟 亮点特性：Cookie 安全增强

**CookieProcessingHandlerInterceptor 采用响应头方式设置 Cookie 属性！**确保跨容器兼容性和安全性。

**核心优势：**
- ✅ **标准兼容** - 使用 Set-Cookie 响应头，符合 HTTP 标准
- ✅ **SameSite 支持** - 正确设置 SameSite=Strict，防止 CSRF 攻击
- ✅ **环境自适应** - 生产环境自动启用 Secure 标志
- ✅ **HttpOnly 保护** - 防止 JavaScript 访问 Cookie，抵御 XSS 攻击

**Cookie 安全配置：**

```java
@Component
public class CookieProcessingHandlerInterceptor implements HandlerInterceptor {
    
    @Autowired
    private ClientAuthInfo clientAuthInfo;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        handleCookie(request, response);
        return true;
    }
    
    protected void handleCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                // 通过响应头设置 Cookie 属性
                StringBuilder cookieHeader = new StringBuilder();
                cookieHeader.append(cookie.getName()).append("=").append(cookie.getValue());
                cookieHeader.append("; Path=").append(cookie.getPath() != null ? cookie.getPath() : "/");
                
                if (cookie.getMaxAge() >= 0) {
                    cookieHeader.append("; Max-Age=").append(cookie.getMaxAge());
                }
                
                cookieHeader.append("; HttpOnly");  // 防止 XSS
                
                if ("produce".equals(clientAuthInfo.getProduce())) {
                    cookieHeader.append("; Secure");  // 仅 HTTPS
                }
                
                cookieHeader.append("; SameSite=Strict");  // 防止 CSRF
                
                response.addHeader("Set-Cookie", cookieHeader.toString());
            }
        }
    }
}
```

**Cookie 安全属性说明：**

| 属性 | 值 | 作用 | 适用环境 |
|------|-----|------|----------|
| **HttpOnly** | - | 禁止 JavaScript 访问，防止 XSS 窃取 Cookie | 所有环境 |
| **Secure** | - | 仅通过 HTTPS 传输，防止中间人攻击 | 生产环境 |
| **SameSite** | Strict | 严格模式，禁止第三方网站携带 Cookie | 所有环境 |
| **Path** | / | Cookie 生效路径 | 所有环境 |
| **Max-Age** | 动态 | Cookie 有效期（秒） | 所有环境 |

**安全防护效果：**

```
攻击场景                      防护机制                     结果
─────────────────────────────────────────────────────────────
XSS 窃取 Cookie          →  HttpOnly                  →  ✅ 阻止
CSRF 跨站请求伪造         →  SameSite=Strict           →  ✅ 阻止
中间人窃听               →  Secure (生产环境)          →  ✅ 阻止
Cookie 劫持              →  三重防护                   →  ✅ 阻止
```

**配置建议：**

```yaml
# application.yaml
simple:
  auth:
    # 标记当前环境，生产环境自动启用 Secure
    produce: produce  # 或 dev/test
```

---

---#### 2.2 simple-common-auth-server（服务端模块）

认证服务器模块，负责用户认证、Token颁发、OAuth2授权。

##### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 登录服务 | `LoginService` | 用户登录、登出 |
| Token管理 | `TokenManager` | Token创建、刷新、验证 |
| 客户端管理 | `ClientManager` | OAuth2客户端信息管理 |
| 登录错误处理 | `LoginErrorProcess` | 登录失败次数限制 |
| 登录成功处理 | `LoginSucProcess` | 登录成功后处理 |
| JWT密钥管理 | `JwtSecretManager` | JWT密钥生成和管理 |
| **权限管理** | **`PermissionManageService`** | **角色权限管理，支持事件驱动实时同步** |

##### 责任链模式

**登录错误处理责任链**：

```java
// 责任链枚举定义
public enum LoginErrorKindProcess implements DefaultKindProcess {
    ACCOUNT_ERROR("基于账号的登录失败计数", true, 1),
    IP_ERROR("基于IP的登录失败计数", true, 2);
}
```

**自定义登录错误处理**：继承 `AbsLoginErrorProcess`

```java
@Component
public class CustomLoginErrorProcess extends AbsLoginErrorProcess {
    
    /**
     * 返回对应的处理器类型枚举
     */
    @Override
    public LoginErrorKindProcess getProcess() {
        return LoginErrorKindProcess.ACCOUNT_ERROR;
    }
    
    /**
     * 获取登录标识key（如账号、IP 等）
     */
    @Override
    protected String getLoginKey(ClientDetails clientDetails, Object adapter, String ip) {
        LoginRequest request = (LoginRequest) adapter;
        return request.getUsername(); // 按用户名统计失败次数
    }
    
    /**
     * 获取Redis key前缀
     */
    @Override
    protected String getKeyPrefix() {
        return authProperties.getLoginErrorKey();
    }
}
```

**核心方法说明：**

| 方法 | 说明 | 是否需实现 | 参数 |
|------|------|-----------|------|
| `getProcess()` | 返回处理器类型枚举 | **必须实现** | - |
| `checkErrorNum()` | 检查失败次数是否超限 | 父类已实现 | clientDetails, adapter, ip |
| `recordError()` | 记录登录失败 | 父类已实现 | clientDetails, adapter, ip |
| `clearError()` | 清除失败记录（登录成功后调用） | 父类已实现 | clientDetails, adapter, ip |
| `getLoginKey()` | 获取登录标识key | **必须实现** | clientDetails, adapter, ip |
| `getKeyPrefix()` | 获取Redis key前缀 | **必须实现** | - |

**登录成功处理责任链**：

```java
// 责任链枚举定义
public enum LoginSucKindProcess implements DefaultKindProcess {
    SAVE_INFO("保存用户信息", true, 1),
    LOGIN_LOG("登陆日志", true, 2);
}
```

**自定义登录成功处理**：实现 `LoginSucProcess` 接口

```java
@Component
public class CustomLoginSucProcess implements LoginSucProcess {
    
    @Override
    public DefaultKindProcess getProcess() {
        return LoginSucKindProcess.SAVE_INFO;
    }
    
    @Override
    public void execute(TokenData tokenData) {
        // 登录成功后的自定义处理逻辑
        log.info("用户 {} 登录成功", tokenData.getUserId());
    }
}
```

#### 🌟 亮点特性：权限管理服务与事件同步

**PermissionManageService 提供完整的权限管理能力！**支持角色权限的增删改查，并通过事件总线实时同步到所有客户端。

**核心优势：**
- ✅ **事件携带完整数据** - 零HTTP请求，直接同步
- ✅ **避免并发雪崩** - 不产生额外的服务端压力
- ✅ **实时性高** - 事件到达即完成同步
- ✅ **职责清晰** - 独立的权限管理服务

**使用示例：**

```java
@Service
public class UserRoleService {
    
    @Autowired
    private PermissionManageService permissionManageService;
    
    /**
     * 更新角色权限
     * 自动完成：更新缓存 + 发布事件 + 所有客户端同步
     */
    public void updateAdminPermissions() {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("user:create", "创建用户");
        permissions.put("user:delete", "删除用户");
        permissions.put("user:update", "更新用户");
        
        // 一行代码，自动触发全流程
        permissionManageService.updateRolePermission("admin", permissions);
    }
    
    /**
     * 删除角色的某个权限
     */
    public void removePermission() {
        permissionManageService.deletePermission("admin", "user:delete");
    }
    
    /**
     * 批量刷新多个角色的权限
     */
    public void batchRefresh() {
        List<String> roles = List.of("admin", "editor", "viewer");
        permissionManageService.batchRefreshPermissions(roles);
    }
}
```

**工作流程：**

```
1. 管理员调用 PermissionManageService.updateRolePermission()
   ↓
2. 服务端更新 Redis 缓存
   ↓
3. 发布 PermissionChangeEvent 事件（携带完整权限数据）
   ↓
4. 所有客户端接收事件
   ↓
5. 客户端直接从事件中获取权限数据并更新 CacheManager 缓存
   ↓
6. ✅ 同步完成（零HTTP请求）
```

**事件类定义：**

```java
@Data
public class PermissionChangeEvent {
    private String roleKey;                    // 角色标识
    private Map<String, String> permissions;   // 完整权限数据
    private String changeType;                 // UPDATE/DELETE
    private Long timestamp;                    // 时间戳
}
```

**客户端监听器（自动处理）：**

```java
@Component
public class PermissionChangeEventHandler {
    
    @Autowired
    private CacheManager cacheManager;
    
    @EventHandler
    public void onPermissionChange(PermissionChangeEvent event) {
        String roleKey = event.getRoleKey();
        Map<String, String> permissions = event.getPermissions();
        
        // 直接更新缓存，无需HTTP请求
        String authKey = TokenConstant.getAuthKey(roleKey);
        cacheManager.hashPutAll(authKey, new HashMap<>(permissions));
        cacheManager.expire(authKey, 30 * 60);
    }
}
```

**配置要求：**

确保项目中已引入事件总线模块：

```yaml
simple:
  event:
    type: mq  # 或 sync，使用 RabbitMQ 或同步事件
```

---

### 3. simple-common-redis（Redis模块）

Redis缓存模块，提供Redisson分布式锁、限流、缓存注解等高级功能封装。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 分布式锁服务 | `RedissonLockService` | 可重入锁、公平锁、闭锁、信号量 |
| 限流管理 | `CurrentLimitingManager` | 基于令牌桶的限流 |
| 限流注解 | `@CurrentLimiting` | 方法级限流注解 |
| 缓存注解 | `@RedisCache` | 方法级缓存注解 |
| 缓存切面管理 | `RedisCacheAspectManager` | 缓存Key生成策略 |

#### 3.1 分布式锁（RedissonLockService）

**支持的锁类型：**

| 锁类型 | 方法 | 说明 |
|--------|------|------|
| 可重入锁 | `lock(key, function)` | 自动续期，30秒刷新一次 |
| 带返回值锁 | `lockHaveValue(key, function)` | 支持返回值的可重入锁 |
| 公平锁 | `fairLock(key, function)` | 按请求顺序获取锁 |
| 闭锁 | `countDownLatch(key, count)` | 等待多个任务完成 |
| 信号量 | `semaphoreLock(key, permits)` | 控制并发访问数量 |

**使用示例：**

```java
@Autowired
private RedissonLockService lockService;

// 1. 可重入锁（无返回值）
lockService.lock("order:create:" + orderId, () -> {
    // 业务逻辑
    orderService.create(order);
});

// 2. 可重入锁（有返回值）
Object result = lockService.lockHaveValue("order:query:" + orderId, () -> {
    return orderService.findById(orderId);
});

// 3. 公平锁
lockService.fairLock("resource:access", () -> {
    // 按请求顺序执行
});

// 4. 闭锁（等待多个任务完成）
lockService.countDownLatch("task:batch", 10); // 等待10个任务
// 在其他线程中完成任务后调用
lockService.decreaseCountDownLatch("task:batch");

// 5. 信号量（控制并发数）
lockService.semaphoreLock("api:concurrent", 100); // 最多100个并发
// 获取许可
lockService.decreaseSemaphoreLock("api:concurrent", 1);
// 释放许可
lockService.increaseSemaphoreLock("api:concurrent", 1);
```

#### 3.2 限流功能（@CurrentLimiting）

**限流规则枚举：**

```java
public enum CurrentLimitingRulesEnum {
    URL,      // 按URL限流
    USER_ID,  // 按用户ID限流
    IP        // 按IP限流
}
```

**注解参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `key` | 限流规则（URL/USER_ID/IP） | URL |
| `time` | 单位时长（秒） | 10 |
| `sum` | 单位时长允许的请求次数 | 2 |
| `waitingTime` | 未获取令牌的最大等待时间（秒） | 5 |
| `waitingTimeErrorStr` | 等待超时后的自定义错误信息 | "" |

**使用示例：**

```java
@RestController
public class ApiController {
    
    // 按URL限流：10秒内最多2次请求
    @GetMapping("/api/data")
    @CurrentLimiting(time = 10, sum = 2)
    public R getData() {
        return R.ok(data);
    }
    
    // 按用户ID限流：60秒内最多10次请求
    @PostMapping("/api/submit")
    @CurrentLimiting(key = CurrentLimitingRulesEnum.USER_ID, time = 60, sum = 10)
    public R submit(@RequestBody Request request) {
        return R.ok();
    }
    
    // 按IP限流：带自定义错误信息
    @GetMapping("/api/search")
    @CurrentLimiting(
        key = CurrentLimitingRulesEnum.IP,
        time = 1,
        sum = 5,
        waitingTimeErrorStr = "搜索太频繁，请稍后再试"
    )
    public R search(@RequestParam String keyword) {
        return R.ok();
    }
}
```

**注意**：使用 `USER_ID` 限流时，需要实现 `CoreLoginUserService` 接口：

```java
@Component
public class CustomLoginUserService implements CoreLoginUserService {
    
    @Override
    public String getUserId() {
        return LoginUserUtils.getUserId();
    }
}
```

#### 3.3 缓存功能（@RedisCache）

**注解参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `head` | 缓存Key前缀 | 请求URL |
| `cacheTime` | 缓存时长（秒） | 10 |
| `appendRandomDuration` | 追加随机时长范围（秒），防止缓存雪崩 | 5 |
| `aclass` | 返回值类型 | R.class |

**使用示例：**

```java
@RestController
public class DataController {
    
    // 基本缓存：缓存10秒，追加0-5秒随机时长
    @GetMapping("/api/user/{id}")
    @RedisCache(cacheTime = 10, appendRandomDuration = 5)
    public R getUser(@PathVariable String id) {
        return R.ok(userService.findById(id));
    }
    
    // 自定义缓存Key前缀
    @GetMapping("/api/config")
    @RedisCache(head = "config:global", cacheTime = 3600)
    public R getConfig() {
        return R.ok(configService.getGlobalConfig());
    }
}
```

**缓存机制：**

```
首次请求
   ↓
查询数据库
   ↓
存入Redis（设置过期时间 + 随机偏移）
   ↓
返回结果

后续请求
   ↓
查询Redis
   ↓
命中 → 直接返回
   ↓
未命中 → 重新查库并缓存

并发请求
   ↓
第一个请求获取分布式锁
   ↓
其他请求等待
   ↓
第一个请求完成后释放锁
   ↓
其他请求从缓存读取
```

**防雪崩机制：**
- ✅ **随机过期时间**：`appendRandomDuration` 在基础过期时间上增加随机偏移
- ✅ **分布式锁**：防止缓存击穿，同一时刻只有一个请求查库
- ✅ **并发控制**：其他请求等待缓存更新完成

---

### 4. simple-common-cache（多缓存模块）

多缓存模块，提供Caffeine本地缓存和Redis分布式缓存的统一抽象。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 缓存管理 | `CacheManager` | 缓存操作统一接口 |
| 本地缓存 | `LocalCacheManager` | 基于Caffeine的本地缓存 |
| Redis缓存 | `RedisCacheManager` | 基于Redis的分布式缓存 |
| 缓存工厂 | `CacheManagerFactory` | 根据配置创建缓存管理器 |

#### 4.1 架构设计

**两级缓存架构：**

```
应用层
   ↓
CacheManager (统一接口)
   ↓
   ├─ LocalCacheManager (一级缓存)
   │   └─ Caffeine (进程内缓存)
   │       ├─ 速度快 (ns级)
   │       └─ 不支持分布式
   │
   └─ RedisCacheManager (二级缓存)
       └─ Redis (分布式缓存)
           ├─ 支持分布式
           └─ 速度稍慢 (ms级)
```

**读写策略：**

```
写入：
   ↓
同时写入本地缓存 + Redis

读取：
   ↓
先查本地缓存
   ↓
命中 → 返回
   ↓
未命中 → 查Redis
   ↓
命中 → 写入本地缓存 + 返回
   ↓
未命中 → 查数据库 → 写入两级缓存 → 返回

删除：
   ↓
同时删除本地缓存 + Redis
```

#### 4.2 使用示例

```java
@Autowired
private CacheManager cacheManager;

// 设置缓存（自动写入本地缓存和Redis）
cacheManager.set("user:1", userData, 30, TimeUnit.MINUTES);

// 获取缓存（优先从本地缓存读取，未命中则查Redis）
User user = cacheManager.get("user:1", User.class);

// 删除缓存（同时删除本地和Redis）
cacheManager.delete("user:1");

// 批量操作
cacheManager.setAll(userMap, 30, TimeUnit.MINUTES);
Map<String, User> users = cacheManager.getAll(keys, User.class);
```

**缓存管理器选择：**

| 类型 | 适用场景 | 特点 |
|------|---------|------|
| `LocalCacheManager` | 单机应用、热点数据 | 速度快，不支持分布式 |
| `RedisCacheManager` | 分布式应用、共享数据 | 支持分布式，性能略低 |

---

### 5. simple-common-rabbitmq（消息队列模块）

RabbitMQ消息队列模块，提供**防重消费、自动重试、锁续期、责任链处理**等企业级特性。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 消费注解 | `@RabbitMqConsumption` | 声明式消息消费，支持防重、重试、锁续期 |
| 消费切面 | `RabbitMqProcessAspect` | AOP实现消息消费的完整流程 |
| 责任链处理 | `RabbitMqProcess` | 消息处理责任链接口 |
| 消费锁管理 | `ConsumptionLockManager` | 防止重复消费的分布式锁 |
| 重试管理 | `RetryMessageManager` | 消息重试控制 |
| 重试计数 | `RetryCountManager` | 重试次数管理 |
| ACK管理 | `AckRMQManager` | 消息确认管理 |
| 失败持久化 | `SendFailurePersistenceManager` | 发送失败消息持久化 |

#### 5.1 架构设计

**消息消费流程：**

```
接收消息
   ↓
尝试获取分布式锁（防重消费）
   ├─ 成功 → 继续
   └─ 失败 → 检查锁状态
       ├─ 已完成且校验通过 → 直接ACK
       ├─ 已完成但校验失败 → 重置锁并重试
       ├─ 处理中 → 拒绝并不放回
       ├─ 僵尸锁 → 重新竞争锁
       └─ 不存在 → 放回队列
   ↓
启动锁续期任务（每 businessTime/3 刷新一次）
   ↓
执行前置处理器（RabbitMqProcess责任链）
   ↓
执行业务方法
   ↓
标记业务完成（存储业务ID）
   ↓
ACK消息
   ↓
清理重试计数
   ↓
取消锁续期任务
```

**重要说明：**

- ✅ **防重消费**：基于Redis分布式锁，防止同一消息被多次消费
- ✅ **自动重试**：消费失败后自动重试，支持配置最大重试次数
- ✅ **锁续期**：业务执行时间超过预估时，自动续期锁，防止锁提前释放
- ✅ **僵尸锁恢复**：检测并恢复异常的长TTL锁
- ✅ **完成校验**：消费完成后验证业务状态，确保一致性
- ✅ **责任链扩展**：支持自定义前置处理器

#### 5.2 消息消费注解（@RabbitMqConsumption）

**这是框架的核心特性！**通过注解即可实现完整的消息消费流程，包括：
- ✅ **防重消费**：基于Redis分布式锁，防止同一消息被多次消费
- ✅ **自动重试**：消费失败后自动重试，支持配置最大重试次数
- ✅ **锁续期**：业务执行时间超过预估时，自动续期锁，防止锁提前释放
- ✅ **僵尸锁恢复**：检测并恢复异常的长TTL锁
- ✅ **完成校验**：消费完成后验证业务状态，确保一致性
- ✅ **责任链扩展**：支持自定义前置处理器

**注解参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `businessTime` | 业务预估执行时长（秒），用于锁的初始TTL | 10 |
| `effectiveTime` | 消息有效时长，用于判断重复消费的最大时间 | 1800 (30分钟) |
| `timeUnit` | 时间单位 | SECONDS |
| `retryCount` | 最大重试次数 | 3 |

**使用示例：**

```java
@Component
public class OrderConsumer {
    
    /**
     * 订单创建消息消费
     * 注意：必须设置手动ACK模式
     */
    @RabbitListener(queues = "order.create.queue", ackMode = "MANUAL")
    @RabbitMqConsumption(
        businessTime = 30,      // 预估业务执行30秒
        effectiveTime = 3600,   // 消息1小时内有效
        retryCount = 5          // 最多重试5次
    )
    public String handleOrderCreate(Message message, Channel channel) throws Exception {
        // 1. 解析消息
        DefaultMessage defaultMessage = SerializeUtils.deserialize(
            message.getBody(), DefaultMessage.class
        );
        
        // 2. 执行业务逻辑
        Order order = JsonUtils.parse(defaultMessage.getData(), Order.class);
        orderService.create(order);
        
        // 3. 返回业务ID（用于完成校验）
        // 如果返回void或null，则使用correlationId作为业务ID
        return order.getId();
    }
    
    /**
     * 订单支付消息消费（无返回值）
     */
    @RabbitListener(queues = "order.pay.queue", ackMode = "MANUAL")
    @RabbitMqConsumption(businessTime = 10, retryCount = 3)
    public void handleOrderPay(Message message, Channel channel) throws Exception {
        DefaultMessage defaultMessage = SerializeUtils.deserialize(
            message.getBody(), DefaultMessage.class
        );
        
        Order order = JsonUtils.parse(defaultMessage.getData(), Order.class);
        orderService.pay(order.getId());
        
        // 返回void，框架会使用correlationId作为业务ID
    }
}
```

**重要说明：**
1. **必须设置手动ACK**：`ackMode = "MANUAL"`
2. **方法签名固定**：`(Message message, Channel channel)`
3. **返回值可选**：
   - 返回String：作为业务ID，用于完成校验
   - 返回void/null：使用correlationId作为业务ID

#### 5.3 消费流程详解

`RabbitMqProcessAspect` 切面实现的完整流程：

```
1. 接收消息
   ↓
2. 尝试获取分布式锁（防重消费）
   ├─ 成功 → 继续
   └─ 失败 → 检查锁状态
       ├─ 已完成且校验通过 → 直接ACK
       ├─ 已完成但校验失败 → 重置锁并重试
       ├─ 处理中 → 拒绝并不放回
       ├─ 僵尸锁 → 重新竞争锁
       └─ 不存在 → 放回队列
   ↓
3. 启动锁续期任务（每 businessTime/3 刷新一次）
   ↓
4. 执行前置处理器（RabbitMqProcess责任链）
   ↓
5. 执行业务方法
   ↓
6. 标记业务完成（存储业务ID）
   ↓
7. ACK消息
   ↓
8. 清理重试计数
   ↓
9. 取消锁续期任务
```

**异常处理：**
- 业务异常 → 释放锁 → NACK（不放回）→ 触发重试
- 中断异常 → NACK（放回队列）→ 让其他消费者处理

#### 5.4 自定义前置处理器

通过实现 `RabbitMqProcess` 接口，在业务执行前进行预处理：

```java
@Component
public class LogRMQProcess implements RabbitMqProcess {
    
    @Override
    public RMQKindProcess getProcess() {
        return RMQKindProcess.LOG_PROCESS; // 需在枚举中定义
    }
    
    @Override
    public void execution(Message message, Channel channel, RabbitMqConsumption annotation) {
        // 记录消息消费日志
        String correlationId = message.getMessageProperties().getCorrelationId();
        log.info("开始消费消息: {}", correlationId);
    }
}
```

**责任链枚举：**

```java
public enum RMQKindProcess implements DefaultKindProcess {
    TEST("测试步骤", false, 1),
    // 添加自定义处理器
    LOG_PROCESS("日志记录", true, 2),
    VALIDATION_PROCESS("数据校验", true, 3);
}
```

#### 5.5 消息重试机制

**自动重试流程：**

```java
// 框架内部自动处理，无需手动调用
try {
    // 执行业务逻辑
} catch (Exception e) {
    // 1. 释放锁
    lockManager.release(queue, correlationId);
    
    // 2. NACK消息（不放回队列）
    channel.basicNack(deliveryTag, false, false);
    
    // 3. 处理重试
    retryMessageManager.handleBusinessFailureRetry(
        retryKey,       // 重试key
        message,        // 原始消息
        channel,        // MQ通道
        maxRetryCount,  // 最大重试次数
        e,              // 异常对象
        defaultMessage  // 解析后的消息体
    );
}
```

**重试策略：**
- 重试次数 < 最大次数：消息重新入队（专用的延迟队列）
- 重试次数 >= 最大次数：消息持久化到失败表，等待人工处理

#### 5.6 锁续期机制

**为什么需要锁续期？**

当业务执行时间超过预估的 `businessTime` 时，分布式锁可能提前过期，导致其他消费者重复消费。锁续期机制会每隔 `businessTime / 3` 的时间刷新锁的TTL。

**续期流程：**

```java
// 框架自动启动续期任务
ScheduledFuture<?> renewalFuture = startLockRenewal(
    queue,           // 队列名称
    correlationId,   // 消息ID
    businessTime,    // 业务时长
    timeUnit,        // 时间单位
    businessThread   // 业务线程
);

// 续期任务：每 businessTime/3 执行一次
lockManager.renewLock(queue, correlationId, businessTime, timeUnit);

// 连续失败3次 → 中断业务线程
if (failureCount >= 3) {
    businessThread.interrupt();
}

// 业务完成后取消续期
renewalFuture.cancel(false);
```

**僵尸锁恢复：**

检测到锁的TTL异常长（可能是之前的消费者崩溃导致），框架会：
1. 记录警告日志
2. 尝试重新竞争锁
3. 成功 → 继续消费
4. 失败 → 放回队列，让其他消费者处理

---

### 6. simple-common-eventbus（事件总线模块）

事件总线模块，提供**同步/异步事件处理、延迟事件、跨系统事件分发**等功能。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 事件服务 | `EventBusService` | 事件发布接口 |
| 同步事件服务 | `SyncEventBusService` | 同步执行事件处理器 |
| 异步事件服务 | `MqEventBusService` | 基于RabbitMQ的异步事件 |
| **循环任务服务** | **`AbsEventCycleService`** | **基于事件总线的可重试循环任务抽象类** |
| 事件处理器管理 | `EventHandlerManager` | 事件处理器注册和管理 |
| 事件注解 | `@Event` | 标记事件类 |
| 事件监听注解 | `@EventHandler` | 标记事件处理方法 |

#### 6.1 架构设计

**同步 vs 异步模式：**

```
配置：simple.event.type = sync/mq

同步模式 (SyncEventBusService)
   ↓
发布事件
   ↓
在当前线程中立即执行所有监听器
   ↓
所有监听器执行完成
   ↓
返回

异步模式 (MqEventBusService)
   ↓
发布事件
   ↓
序列化事件数据
   ↓
发送到RabbitMQ
   ↓
立即返回（不等待监听器执行）
   ↓
RabbitMQ分发给消费者
   ↓
各系统的监听器异步执行
```

**重要说明：**

- ✅ **同步模式**：适合轻量级、快速的事件处理，阻塞发布线程
- ✅ **异步模式**：适合耗时操作，支持跨系统事件分发
- ✅ **延迟事件**：基于RabbitMQ延迟队列或调度线程池
- ✅ **跨系统**：通过`targets`参数指定接收事件的系统

#### 6.2 事件定义（@Event）

**事件类必须添加 `@Event` 注解：**

```java
import com.simple.common.eventbus.common.annotation.Event;
import lombok.Getter;

@Getter
@Event("user.created")  // 事件名称，可选，默认为类名
public class UserCreatedEvent {
    
    private final String userId;
    private final String username;
    private final String email;
    
    public UserCreatedEvent(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
}
```

**@Event 注解参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `value` | 事件名称 | 类的短类名 |
| `targets` | 要接收该事件的目标系统名称 | THIS_MACHINE（本系统） |

**跨系统事件示例：**

```java
@Event(
    value = "order.created",
    targets = {"order-service", "notification-service", "analytics-service"}
)
public class OrderCreatedEvent {
    private final String orderId;
    private final BigDecimal amount;
    
    // ...
}
```

#### 6.3 事件监听（@EventHandler）

**使用 `@EventHandler` 注解标记事件处理方法：**

```java
@Component
public class UserEventListener {
    
    /**
     * 监听用户创建事件
     * 注意：方法只能有一个参数，且参数类型必须带有@Event注解
     */
    @EventHandler
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("用户创建成功: {}, {}", event.getUserId(), event.getUsername());
        
        // 发送欢迎邮件
        emailService.sendWelcomeEmail(event.getEmail());
        
        // 初始化用户数据
        userService.initUserData(event.getUserId());
    }
    
    /**
     * 监听指定名称的事件
     */
    @EventHandler("order.created")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("订单创建: {}", event.getOrderId());
        
        // 扣减库存
        inventoryService.deduct(event.getOrderId());
        
        // 发送通知
        notificationService.sendOrderNotification(event.getOrderId());
    }
}
```

**@EventHandler 注解参数：**

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `value` | 要处理的事件名称 | 参数类型的短类名 |
| `applicationName` | 接收哪些系统的事件 | THIS_MACHINE（本系统） |

**重要限制：**
- ✅ 方法只能有**一个参数**
- ✅ 参数类型必须带有 `@Event` 注解
- ❌ 不支持多参数方法，多参数请全部放到@Event标注的类里面

#### 6.4 事件发布

**EventBusService 提供4种发布方式：**

```java
@Autowired
private EventBusService eventBusService;

// 1. 发布事件（自动识别事件类型）
eventBusService.push(new UserCreatedEvent("123", "张三", "zhangsan@example.com"));

// 2. 发布事件（指定事件类型）
Map<String, Object> data = new HashMap<>();
data.put("userId", "123");
data.put("username", "张三");
eventBusService.push(data, UserCreatedEvent.class);

// 3. 发布延迟事件（自动识别事件类型）
// 30分钟后取消未支付订单
eventBusService.push(
    new OrderTimeoutEvent("ORDER123"), 
    30, 
    TimeUnit.MINUTES
);

// 4. 发布延迟事件（指定事件类型）
// 24小时后发送用户活跃度提醒
Map<String, Object> reminderData = new HashMap<>();
reminderData.put("userId", "123");
eventBusService.push(
    reminderData, 
    UserActiveReminderEvent.class, 
    24, 
    TimeUnit.HOURS
);
```

#### 6.5 同步 vs 异步模式

**配置方式：**

```yaml
simple:
  event:
    type: sync  # sync=同步, mq=异步（默认）
```

**同步模式（SyncEventBusService）：**

- ✅ 事件在发布线程中立即执行
- ✅ 适合轻量级、快速的事件处理
- ✅ 延迟事件使用调度线程池执行
- ❌ 阻塞发布线程，影响性能

```java
// 同步执行：handleUserCreated 在当前线程中执行
eventBusService.push(new UserCreatedEvent(...));
log.info("事件已处理完成"); // 此时事件处理器已经执行完毕
```

**异步模式（MqEventBusService）：**

- ✅ 事件通过RabbitMQ异步分发
- ✅ 不阻塞发布线程，性能更好
- ✅ 支持跨系统事件分发
- ✅ 支持延迟事件（基于RabbitMQ延迟队列）
- ❌ 需要RabbitMQ依赖
- ❌ 多线程消费的情况下，事件处理顺序不保证

```java
// 异步执行：消息发送到RabbitMQ后立即返回
eventBusService.push(new UserCreatedEvent(...));
log.info("消息已发送"); // 此时事件处理器可能还未执行

// 在其他系统或同一系统的消费者中处理
同上 6.2 事件监听（@EventHandler）
```

#### 6.6 跨系统事件分发

**场景：订单创建后，通知多个系统**

```java
// 1. 定义跨系统事件
@Event(
    value = "order.created",
    targets = {"order-service", "inventory-service", "notification-service"}
)
public class OrderCreatedEvent {
    private final String orderId;
    private final BigDecimal amount;
    
    // ...
}

// 2. 发布事件（会自动发送到所有目标系统）
eventBusService.push(new OrderCreatedEvent("ORDER123", new BigDecimal("99.99")));

// 3. 各系统分别监听

// order-service 中
@EventHandler(applicationName = "order-service")
public void handleOrderCreatedInOrderService(OrderCreatedEvent event) {
    // 订单服务处理逻辑
}

// inventory-service 中
@EventHandler(applicationName = "inventory-service")
public void handleOrderCreatedInInventory(OrderCreatedEvent event) {
    // 库存服务处理逻辑：扣减库存
}

// notification-service 中
@EventHandler(applicationName = "notification-service")
public void handleOrderCreatedInNotification(OrderCreatedEvent event) {
    // 通知服务处理逻辑：发送订单确认通知
}
```

#### 6.6 事件处理器注册机制

框架通过 `EventHandlerManager` 自动扫描并注册所有带有 `@EventHandler` 注解的方法：

```java
// 应用启动时自动执行
@PostConstruct
public void init() {
    // 扫描所有Bean
    for (String beanName : applicationContext.getBeanDefinitionNames()) {
        Object bean = applicationContext.getBean(beanName);
        
        // 扫描Bean中的所有方法
        for (Method method : bean.getClass().getMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                // 注册事件处理器
                eventHandlerManager.register(beanName, method);
            }
        }
    }
}
```

**注意事项：**
- ✅ 事件监听器必须是Spring Bean（添加 `@Component` 等注解）
- ✅ 方法必须是public
- ✅ 方法只能有一个参数，所有参数需要聚合在一个类中。

#### 🌟 亮点特性：基于事件的循环任务 AbsEventCycleService

**AbsEventCycleService 是框架的核心亮点之一！**通过事件总线实现可重试、可延迟的循环任务，支持失败自动重试和灵活的生命周期回调。

**核心机制：**

```
启动循环任务
   ↓
创建 CycleEvent 事件
   ↓
执行 handler() 业务逻辑
   ↓
成功 → 调用 ok() 回调
   ↓
失败 → 发送延迟事件（timeInterval秒后）
   ↓
事件触发 → 再次执行 handler()
   ↓
达到最大次数 → 调用 more() 回调
   ↓
异常/发送失败 → 调用 error() 回调
```

**使用示例：**

```java
import com.simple.common.eventbus.common.service.AbsEventCycleService;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 订单超时取消任务
 */
@Component
public class OrderTimeoutCycleService extends AbsEventCycleService<OrderTimeoutTask> {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 指定任务参数类型
     */
    @Override
    public Class<OrderTimeoutTask> getEventClass() {
        return OrderTimeoutTask.class;
    }
    
    /**
     * 业务处理逻辑
     * @return true=成功，false=失败（会重试）
     */
    @Override
    protected Boolean handler(OrderTimeoutTask task, Map<String, Object> parameters) {
        log.info("第{}次检查订单超时: orderId={}", parameters.get("retryCount"), task.getOrderId());
        
        // 查询订单状态
        Order order = orderService.getById(task.getOrderId());
        if (order == null) {
            log.warn("订单不存在: {}", task.getOrderId());
            return true; // 订单不存在，视为成功
        }
        
        // 如果订单已支付，取消任务
        if (order.isPaid()) {
            log.info("订单已支付，取消超时任务: {}", task.getOrderId());
            return true;
        }
        
        // 检查是否超时
        if (order.isTimeout()) {
            // 取消订单
            orderService.cancelOrder(task.getOrderId(), "订单超时自动取消");
            log.info("订单已取消: {}", task.getOrderId());
            return true;
        }
        
        // 未超时，返回false触发重试
        return false;
    }
    
    /**
     * 成功回调
     */
    @Override
    protected void ok(OrderTimeoutTask task, Map<String, Object> parameters) {
        log.info("订单超时任务完成: orderId={}, retryCount={}", 
                 task.getOrderId(), parameters.get("retryCount"));
    }
    
    /**
     * 超过最大次数回调
     */
    @Override
    protected void more(OrderTimeoutTask task, Map<String, Object> parameters) {
        log.error("订单超时任务达到最大重试次数: orderId={}, retryCount={}", 
                  task.getOrderId(), parameters.get("retryCount"));
        // 可以发送告警通知
    }
    
    /**
     * 异常回调
     */
    @Override
    protected void error(OrderTimeoutTask task, Map<String, Object> parameters) {
        log.error("订单超时任务执行异常: orderId={}", task.getOrderId(), 
                  (Throwable) parameters.get("exception"));
        // 可以记录异常日志或告警
    }
}

/**
 * 任务参数类
 */
@Data
public class OrderTimeoutTask {
    private String orderId;
    private Integer timeoutMinutes;
}
```

**启动循环任务：**

```java
@Autowired
private OrderTimeoutCycleService orderTimeoutCycleService;

// 启动任务
OrderTimeoutTask task = new OrderTimeoutTask();
task.setOrderId("ORDER123");
task.setTimeoutMinutes(30);

// 参数说明：
// runBody: 任务参数
// sum: 总执行次数（最多重试次数）
// timeInterval: 每次执行的间隔时间（秒）
// isAccumulate: 是否累加延迟（true=1s,2s,3s...；false=固定1s）
// parameters: 扩展参数（可选）
orderTimeoutCycleService.run(task, 10, 60, false, null);
```

**参数详解：**

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `runBody` | T | 任务参数对象 | OrderTimeoutTask |
| `sum` | Integer | 总执行次数（最大重试次数） | 10 |
| `timeInterval` | Integer | 执行间隔时间（秒） | 60 |
| `isAccumulate` | Boolean | 是否累加延迟 | false |
| `parameters` | Map | 扩展参数（传递额外数据） | null |

**执行流程示例：**

```
假设配置：sum=5, timeInterval=10, isAccumulate=false

第1次执行（0秒） → 失败 → 10秒后重试
第2次执行（10秒） → 失败 → 10秒后重试
第3次执行（20秒） → 失败 → 10秒后重试
第4次执行（30秒） → 失败 → 10秒后重试
第5次执行（40秒） → 失败 → 调用 more() 回调
```

```
假设配置：sum=5, timeInterval=10, isAccumulate=true

第1次执行（0秒） → 失败 → 10秒后重试
第2次执行（10秒） → 失败 → 20秒后重试
第3次执行（30秒） → 失败 → 30秒后重试
第4次执行（60秒） → 失败 → 40秒后重试
第5次执行（100秒） → 失败 → 调用 more() 回调
```

**生命周期回调：**

| 回调方法 | 触发时机 | 用途 |
|---------|---------|------|
| `handler()` | 每次执行时 | **必须实现**，业务逻辑，返回true/false |
| `ok()` | handler返回true | 任务成功时的清理工作 |
| `more()` | 达到最大重试次数 | 任务彻底失败的处理 |
| `error()` | 执行异常或发送失败 | 异常情况处理 |
| `addCounter()` | 每次执行前 | 可选，记录重试次数 |

**优势：**

- ✅ **基于事件总线**：利用EventBus的延迟事件能力，无需定时任务
- ✅ **自动重试**：失败后自动延迟重试，支持配置重试次数和间隔
- ✅ **灵活延迟**：支持固定间隔和累加间隔两种模式
- ✅ **生命周期管理**：提供ok/more/error三个回调，便于监控和告警
- ✅ **分布式友好**：异步模式下支持跨系统任务调度
- ✅ **线程安全**：基于BeanNameAware自动获取Bean名称，避免并发问题

**典型应用场景：**

1. **订单超时取消**：下单后30分钟未支付自动取消
2. **支付结果轮询**：第三方支付结果异步通知，主动轮询确认
3. **数据同步重试**：第三方接口调用失败后重试
4. **定时提醒**：会议开始前5分钟、1分钟分别提醒
5. **库存补偿**：扣减库存失败后重试

**与XXL-JOB对比：**

| 特性 | AbsEventCycleService | XXL-JOB |
|------|---------------------|----------|
| 适用场景 | 短时任务、重试任务 | 长时任务、复杂调度 |
| 部署依赖 | 无需额外部署 | 需要XXL-JOB Admin |
| 配置复杂度 | 代码配置，简单 | 需要在Admin界面配置 |
| 重试机制 | 内置自动重试 | 需手动配置 |
| 延迟执行 | 天然支持 | 需配置Cron |
| 跨系统 | 异步模式支持 | 支持 |
| 可视化 | 无 | 有管理界面 |

**建议：**
- 简单的重试任务、延迟任务 → 使用 `AbsEventCycleService`
- 复杂的定时任务、需要可视化管理 → 使用 `XXL-JOB`

---

### 7. simple-common-websocket（WebSocket模块）

WebSocket实时通信模块，**基于Netty实现**，支持**消息监听注解、连接鉴权、集群部署**。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 消息推送工具 | `WebSocketUtils` | **静态工具类**，提供消息发送、连接管理 |
| 连接校验 | `CheckWebSocketManager` | WebSocket连接鉴权接口 |
| 默认校验实现 | `DefaultCheckWebSocketManager` | 默认放行所有连接 |
| 消息监听管理 | `WebSocketListeningManager` | 消息监听器注册和分发接口 |
| 默认监听管理 | `DefaultWebSocketListeningManager` | 基于type+cliKey的双层Map分发 |
| 消息监听注解 | `@WebSocketListening` | 声明式消息监听 |
| 认证处理器 | `WebSocketAuthHandler` | WebSocket握手认证（Netty Handler） |
| 消息处理器 | `WebSocketServerHandler` | 消息接收、解析、分发（Netty Handler） |
| 通道管理 | `ChannelMap` | 线程安全的通道存储结构 |

#### 7.1 消息监听注解（@WebSocketListening）

**这是框架的核心特性！**通过注解即可实现声明式消息监听：

```java
@Component
public class OrderMessageHandler {
    
    /**
     * 监听订单类型消息
     * type: 消息类型，用于区分不同业务
     * cliKey: 客户端标识，默认为"default"
     */
    @WebSocketListening(type = "order", cliKey = "web")
    public String handleOrderMessage(WebSocketRequest request) {
        // request.getData() 获取消息数据（JSON字符串）
        log.info("收到订单消息: {}", request.getData());
        
        // 处理业务逻辑
        OrderMessage message = JsonUtils.parse(request.getData(), OrderMessage.class);
        orderService.process(message);
        
        // 返回响应（可选），会发送回客户端
        return "{\"status\":\"success\"}";
    }
    
    /**
     * 监听聊天消息
     */
    @WebSocketListening(type = "chat")
    public void handleChatMessage(WebSocketRequest request) {
        ChatMessage chat = JsonUtils.parse(request.getData(), ChatMessage.class);
        
        // 转发给目标用户
        webSocketService.sendToUser(chat.getTargetUserId(), chat.getContent());
    }
}
```

**@WebSocketListening 注解参数：**

| 参数 | 说明 | 必填 | 默认值 |
|------|------|------|--------|
| `type` | 消息类型，用于区分不同业务通道 | 是 | - |
| `cliKey` | 客户端标识，用于区分相同类型下的不同端 | 否 | "default" |

**重要说明：**
- ✅ 方法只能有**一个参数**：`WebSocketRequest`
- ✅ 返回值可选：
  - 返回String：作为响应发送回客户端
  - 返回void：不发送响应
- ✅ 同一个type+cliKey组合只能有一个监听器
- ❌ 不支持多参数方法

#### 7.2 自定义连接鉴权

**继承 `DefaultCheckWebSocketManager` 实现Token验证：**

```java
@Component
public class CustomCheckWebSocketManager extends DefaultCheckWebSocketManager {
    
    @Autowired
    private TokenManager tokenManager;
    
    @Override
    public boolean checkToken(String token, String type, String cliKey) {
        // 验证Token有效性
        try {
            Map<String, Object> payload = tokenManager.check(token, false);
            
            if (payload == null || payload.isEmpty()) {
                log.warn("WebSocket Token无效");
                return false;
            }
            
            // 可以在此处进行额外的权限校验
            String userId = (String) payload.get("userId");
            log.info("WebSocket连接认证成功: userId={}, type={}, cliKey={}", 
                     userId, type, cliKey);
            
            return true;
        } catch (Exception e) {
            log.error("WebSocket Token验证失败", e);
            return false;
        }
    }
}
```

**参数说明：**

| 参数 | 说明 | 示例 |
|------|------|------|
| `token` | 认证Token字符串，通常为JWT Token | "eyJhbGciOiJIUzI1NiJ9..." |
| `type` | 客户端类型，从URL参数中获取 | "web"、"app"、"mini-program" |
| `cliKey` | 客户端唯一标识，从URL参数中获取 | "client_12345" |

**客户端连接示例：**

```javascript
// 连接时传递Token、type、cliKey
const ws = new WebSocket('ws://localhost:8080/ws?token=xxx&type=web&cliKey=client123');

ws.onopen = () => {
    console.log('连接成功');
};

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('收到消息:', data);
};

ws.onerror = (error) => {
    console.error('连接错误:', error);
};

ws.onclose = () => {
    console.log('连接关闭');
    // 重连逻辑
    setTimeout(() => reconnect(), 3000);
};
```

#### 7.3 消息推送（WebSocketUtils）

**重要：WebSocket模块没有Service层，所有消息推送通过静态工具类 `WebSocketUtils` 实现！**

```java
import com.simple.common.websocket.utils.WebSocketUtils;

// 1. 向指定type的所有客户端发送消息
WebSocketUtils.sendMsg("notification", "您有一条新消息");

// 2. 向指定type+cliKey的客户端发送消息
boolean success = WebSocketUtils.sendMsg("order", "user001", "订单已支付");

// 3. 检查客户端是否在线
boolean online = WebSocketUtils.isOnline("chat", "user123");

// 4. 获取当前连接总数
int count = WebSocketUtils.getConnectionCount();

// 5. 清理无效通道（定时任务调用）
int cleaned = WebSocketUtils.cleanInactiveChannels();
```

**方法说明：**

| 方法 | 说明 | 参数 | 返回值 |
|------|------|------|--------|
| `sendMsg(type, msg)` | 向指定type的所有客户端广播 | type: 类型标识<br>msg: 消息内容 | void |
| `sendMsg(type, cliKey, msg)` | 向指定客户端发送消息 | type: 类型标识<br>cliKey: 客户端标识<br>msg: 消息内容 | boolean: 是否发送成功 |
| `isOnline(type, cliKey)` | 检查客户端是否在线 | type: 类型标识<br>cliKey: 客户端标识 | boolean |
| `getConnectionCount()` | 获取当前连接总数 | - | int |
| `cleanInactiveChannels()` | 清理无效通道 | - | int: 清理数量 |
| `clearAll()` | 清空所有通道 | - | void |

#### 7.4 消息监听机制

**DefaultWebSocketListeningManager 使用双层Map结构：**

```
listenerMap:
  type1 ->
    cliKey1 -> InvocationTarget(bean1, method1)
    cliKey2 -> InvocationTarget(bean2, method2)
  type2 ->
    cliKey1 -> InvocationTarget(bean3, method3)
```

**消息分发流程：**

```
1. 客户端发送消息
   ↓
2. WebSocketServerHandler 接收TextWebSocketFrame
   ↓
3. 解析为 WebSocketRequest 对象
   ↓
4. 从Channel属性中获取 type 和 cliKey
   ↓
5. 调用 webSocketListeningManager.invoke(type, cliKey, request)
   ↓
6. 查找对应的 InvocationTarget
   ↓
7. 反射调用监听器方法
   ↓
8. 如果有返回值，发送回客户端
```

**自动注册机制：**

应用启动时，框架会自动扫描所有带有 `@WebSocketListening` 注解的方法并注册：

```java
// 框架内部自动执行
@PostConstruct
public void init() {
    for (String beanName : applicationContext.getBeanDefinitionNames()) {
        Object bean = applicationContext.getBean(beanName);
        
        for (Method method : bean.getClass().getMethods()) {
            if (method.isAnnotationPresent(WebSocketListening.class)) {
                WebSocketListening annotation = method.getAnnotation(WebSocketListening.class);
                
                // 注册监听器
                listeningManager.registerMethod(
                    annotation.type(),
                    annotation.cliKey(),
                    bean,
                    method
                );
            }
        }
    }
}
```

#### 7.5 消息格式

**客户端发送的消息格式：**

```json
{
  "data": "{\"orderId\":\"123\",\"action\":\"pay\"}"
}
```

**注意**：`data` 字段是一个JSON字符串，需要在服务端再次解析。

**服务端响应格式（如果监听器有返回值）：**

```json
{
  "status": "success"
}
```

**错误响应格式：**

```json
{
  "code": 4001,
  "message": "消息过长，最大允许10240字节"
}
```

#### 7.6 配置项

```yaml
simple:
  websocket:
    port: 8080                    # WebSocket端口
    path: /ws                     # WebSocket路径
    max-text-message-length: 10240 # 最大文本消息长度（字节）
    boss-thread-count: 1          # Boss线程数
    worker-thread-count: 4        # Worker线程数
```

#### 7.7 完整示例

**服务端：**

```java
// 1. 定义消息监听器
@Component
public class NotificationHandler {
    
    @WebSocketListening(type = "notification")
    public void handleNotification(WebSocketRequest request) {
        Notification notification = JsonUtils.parse(
            request.getData(), Notification.class
        );
        
        log.info("收到通知: {}", notification.getTitle());
        
        // 转发给其他用户（使用 WebSocketUtils）
        WebSocketUtils.sendMsg(
            "notification",
            notification.getTargetCliKey(),
            notification.getContent()
        );
    }
}

// 2. 自定义鉴权
@Component
public class CustomWebSocketAuth extends DefaultCheckWebSocketManager {
    
    @Override
    public boolean checkToken(String token, String type, String cliKey) {
        // Token验证逻辑
        return token != null && !token.isEmpty();
    }
}
```

**客户端：**

```javascript
// 连接
const ws = new WebSocket('ws://localhost:8080/ws?token=abc123&type=web&cliKey=user001');

// 发送消息
ws.send(JSON.stringify({
  data: JSON.stringify({
    title: "系统通知",
    content: "您有一条新消息",
    targetCliKey: "user002"
  })
}));

// 接收消息
ws.onmessage = (event) => {
  const response = event.data; // 直接是字符串，不是JSON
  console.log('收到响应:', response);
};
```

### 8. simple-common-logs（统一日志模块）

统一日志中心模块，**基于Netty TCP + Protobuf实现远程日志收集**，支持**批量发送、失败降级、自动恢复、WAL兜底**。

#### 模块结构

| 子模块 | 说明 |
|--------|------|
| `simple-common-logs-client` | 日志客户端，负责采集和发送日志 |
| `simple-common-logs-proto` | Protobuf协议定义 |
| `simple-common-logs-server` | 日志服务端，负责接收、保存日志 |

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| **Client端** | | |
| 日志服务 | `LogService` | 日志采集和发送接口 |
| 默认日志服务 | `DefaultLogService` | 从HTTP请求中提取日志信息并发送 |
| 日志管理器 | `LogManager` | 日志发送管理接口 |
| 缓冲日志管理 | `BufferedLogManager` | **核心组件**：批量发送、失败降级、自动恢复 |
| 用户信息管理 | `LogUserManager` | 获取当前登录用户信息接口 |
| 默认用户管理 | `DefaultLogUserManager` | 默认返回"测试用户名"和"测试用户ID" |
| TCP客户端 | `LogProtobufTcpClient` | 基于Netty的TCP客户端，Fire-and-Forget模式 |
| 日志过滤器 | `LogFilter` | 包装HttpServletRequest为CachedBodyHttpServletRequest |
| 日志拦截器 | `LogInterceptor` | 生成TraceId、记录请求开始时间、调用日志服务 |
| 请求体缓存 | `CachedBodyHttpServletRequest` | 支持多次读取请求体的包装类 |
| **Proto定义** | | |
| 日志数据消息 | `LogData` | 包含traceId、userId、operUrl等15个字段 |
| 批量日志容器 | `LogBatch` | 包含repeated LogData logs |
| **Server端** | | |
| TCP服务端 | `LogProtobufTcpServer` | 基于Netty的TCP服务端，监听端口接收日志 |
| 服务端处理器 | `LogProtobufServerHandler` | 接收LogBatch，转换为LogDataEvent列表 |
| 日志保存管理 | `LogsSaveManager` | 日志保存管理接口 |
| 抽象保存管理 | `AbsLogsSaveManager` | **核心组件**：内存队列+批量处理+WAL兜底+死信队列 |

#### 8.1 架构设计

**整体架构：**

```
┌─────────────────────┐         TCP + Protobuf          ┌──────────────────────┐
│   Client 端          │  ──────────────────────────►    │   Server 端           │
│                     │                                 │                      │
│ • LogFilter         │     Fire-and-Forget 模式        │ • LogProtobufTcpServer│
│ • LogInterceptor    │     (单向发送，不等待响应)       │ • LogProtobufServer   │
│ • DefaultLogService │                                 │ • AbsLogsSaveManager  │
│ • BufferedLogManager│                                 │   ├─ 内存队列         │
│ • LogProtobufTcp    │                                 │   ├─ WAL兜底          │
│   Client            │                                 │   ├─ 死信队列         │
│                     │                                 │   └─ 持久化(子类实现)  │
└─────────────────────┘                                 └──────────────────────┘
```

**Client端工作流程：**

```
1. LogFilter（优先级最高）拦截所有请求
   ↓
2. 包装HttpServletRequest为CachedBodyHttpServletRequest（缓存请求体）
   ↓
3. LogInterceptor.preHandle() 执行
   ├─ 生成或沿用TraceId
   ├─ 设置到Request属性
   ├─ 设置到Response Header
   └─ 记录请求开始时间
   ↓
4. 业务逻辑执行
   ↓
5. LogInterceptor.afterCompletion() 执行
   ↓
6. DefaultLogService.send() 被调用
   ├─ 从对象池获取 LogDataEvent
   ├─ 提取HTTP请求信息（URL、IP、参数、用户信息等）
   └─ 调用 bufferedLogManager.send(logDataEvent)
   ↓
7. BufferedLogManager 处理
   ├─ 尝试入队到 LinkedBlockingQueue
   │  ├─ 成功：等待后台线程批量发送
   │  └─ 失败（队列满）：异步降级写入本地文件
   └─ 后台线程 batchSendLoop()
      ├─ 定期从队列拉取数据（最多batchSize条）
      ├─ 组装为 LogBatch
      └─ 调用 tcpClient.sendBatch()
         ├─ 成功：回收对象到对象池
         └─ 失败：触发 SendFailureListener 回调，异步降级写入
   ↓
8. 降级恢复线程 fallbackResendLoop() 定时扫描降级目录
   ├─ 读取降级文件
   ├─ 解析日志记录（魔数校验）
   └─ 重新发送到服务器
```

**Server端工作流程：**

```
1. LogProtobufTcpServer 启动，监听指定端口
   ↓
2. 接收客户端TCP连接
   ↓
3. LogProtobufServerHandler.channelRead0() 接收 LogBatch
   ↓
4. 将 Protobuf LogBatch 转换为 List<LogDataEvent>
   ↓
5. 调用 logsSaveManager.saveLogBatch(events)
   ↓
6. AbsLogsSaveManager 处理
   ├─ 批次入队到 LinkedBlockingQueue<List<LogDataEvent>>
   ├─ 如果队列满：写入WAL文件兜底
   └─ 定时任务 processLogs()
      ├─ 从队列批量取出（drainTo）
      ├─ 扁平化为单个列表
      ├─ 调用 persistence(logsToSave) （子类实现）
      └─ 如果持久化失败：写入WAL文件
   ↓
7. WAL恢复任务 walRecoveryTask()
   ├─ 定时扫描WAL目录
   ├─ 读取WAL文件中的JSON日志
   ├─ 反序列化为 LogDataEvent
   └─ 重新入队或直接持久化
   ↓
8. 死信队列处理
   ├─ WAL恢复失败的日志进入死信目录
   └─ 人工介入处理
```

**BufferedLogManager 核心特性：**

1. ✅ **内存缓冲队列**：使用 `LinkedBlockingQueue` 缓冲日志，后台线程批量发送
2. ✅ **失败降级**：发送失败时自动写入本地文件，避免数据丢失
3. ✅ **自动恢复**：连接恢复后自动补发降级文件中的日志
4. ✅ **优雅停机**：应用关闭时刷新缓冲区，确保数据不丢
5. ✅ **对象池优化**：使用 `LogDataEvent.acquire()` 和 `recycle()` 减少GC压力
6. ✅ **异步降级写入**：降级文件写入使用独立线程池，不阻塞业务线程
7. ✅ **魔数校验**：降级文件采用魔数（0xCAFEBABE）校验，具备损坏恢复能力

**AbsLogsSaveManager 核心特性：**

1. ✅ **内存队列**：`LinkedBlockingQueue<List<LogDataEvent>>`，批次入队减少锁竞争
2. ✅ **批量处理**：定时从队列drainTo批量取出，减少持久化次数
3. ✅ **WAL兜底**：队列满或持久化失败时写入WAL文件，保证数据不丢
4. ✅ **WAL恢复**：定时扫描WAL文件，重新入队或直接持久化
5. ✅ **死信队列**：WAL恢复失败的日志进入死信目录，人工介入
6. ✅ **幂等性要求**：persistence()方法必须保证幂等性（WAL可能重放）
7. ✅ **优雅停机**：应用关闭时取消定时任务，刷新剩余日志

#### 8.2 自定义用户信息获取

**继承 `DefaultLogUserManager` 实现真实用户信息：**

```java
@Component
public class CustomLogUserManager extends DefaultLogUserManager {
    
    @Override
    public String loginNickName() {
        // 从登录上下文获取用户名
        UserTemporary user = LoginUserUtils.getUserTemporary();
        return user != null ? user.getUsername() : "anonymous";
    }
    
    @Override
    public String loginUserId() {
        // 从登录上下文获取用户ID
        return LoginUserUtils.getUserId();
    }
}
```

**重要说明：**

- ❌ 如果不实现此接口，日志中将显示"测试用户名"和"测试用户ID"
- ❌ 控制台会输出警告：`请实现LogUserManager提供用户名和ID`
- ✅ 方法返回null或空字符串时，日志中会显示 `-`

#### 8.3 Protobuf协议定义

**LogData 消息结构（15个字段）：**

```protobuf
message LogData {
  string traceId = 1;          // 追踪ID
  string title = 2;            // 方法名称/操作标题
  string method = 3;           // 请求方式
  string operUrl = 4;          // 请求URL
  string operIp = 5;           // 主机地址
  string operLocation = 6;     // 操作地点
  string userId = 7;           // 操作人员id
  string nickname = 8;         // 用户名
  string operName = 9;         // 操作名称
  string operParam = 10;       // 请求参数
  int32 status = 11;           // 操作状态
  string errorMsg = 12;        // 错误消息
  string errorData = 13;       // 异常信息
  int64 requestTime = 14;      // 接口耗时（毫秒）
  int64 createTime = 15;       // 创建时间
}
```

**LogBatch 批量容器：**

```protobuf
message LogBatch {
  repeated LogData logs = 1;   // 多条日志数据
}
```

**重要说明：**

- ✅ 采用 Fire-and-Forget 模式，**没有响应消息**
- ✅ 客户端单向发送，不等待服务端响应
- ✅ 减少网络往返开销，适合极高吞吐场景

#### 8.4 Server端扩展

**继承 `AbsLogsSaveManager` 实现自定义持久化：**

```java
@Component
public class EsLogsSaveManager extends AbsLogsSaveManager {
    
    @Autowired
    private LogTcpServerProperties properties;
    
    @Autowired
    private ElasticsearchClient esClient;
    
    @Override
    protected LogTcpServerProperties getProperties() {
        return properties;
    }
    
    /**
     * 实现具体的持久化逻辑
     * ⚠️ 必须保证幂等性！WAL恢复可能重放已成功持久化的日志
     */
    @Override
    protected void persistence(List<LogDataEvent> logs) throws Exception {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        
        // 批量写入Elasticsearch
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (LogDataEvent log : logs) {
            bulkBuilder.operations(op -> op
                .index(idx -> idx
                    .index("sys-logs")
                    .document(log)
                )
            );
        }
        
        BulkResponse response = esClient.bulk(bulkBuilder.build());
        if (response.errors()) {
            throw new RuntimeException("ES批量写入失败");
        }
        
        log.info("成功保存 {} 条日志到ES", logs.size());
    }
}
```

**⚠️ 幂等性要求：**

由于WAL恢复机制可能在进程崩溃后重放已成功持久化的日志，`persistence()` 方法**必须保证幂等性**。

**实现幂等性的方法：**

1. **使用唯一ID**：以 `traceId + timestamp` 作为文档ID
2. **先查询再插入**：检查是否已存在相同日志
3. **使用UPSERT**：存在则更新，不存在则插入

**示例：使用traceId保证幂等性**

```java
@Override
protected void persistence(List<LogDataEvent> logs) throws Exception {
    for (LogDataEvent log : logs) {
        // 使用 traceId 作为文档ID，保证幂等性
        String docId = log.getTraceId() + "_" + log.getCreateTime();
        
        esClient.index(op -> op
            .index("sys-logs")
            .id(docId)  // 指定ID，存在则覆盖
            .document(log)
        );
    }
}
```

**配置项：**

```yaml
simple:
  log:
    server:
      port: 9090                         # 监听端口
      boss-threads: 1                    # Boss线程数
      worker-threads: 4                  # Worker线程数
      reader-idle-time: 300              # 读空闲超时（秒）
      queue-capacity: 10000              # 内存队列容量
      batch-size: 100                    # 批量处理大小
      process-interval: 5000             # 处理间隔（毫秒）
      wal-enabled: true                  # 是否启用WAL
      wal-dir: ./logs/wal                # WAL文件目录
      wal-recovery-interval: 60000       # WAL恢复间隔（毫秒）
      dead-letter-dir: ./logs/dead-letter # 死信目录
```

#### 8.5 日志采集详情

框架通过拦截器/过滤器自动调用 `LogService.send()`，无需手动调用。

**采集的信息包括：**

| 字段 | 来源 | 说明 |
|------|------|------|
| `traceId` | Request属性 | 链路追踪ID（由LogInterceptor生成） |
| `userId` | LogUserManager | 用户ID |
| `nickname` | LogUserManager | 用户昵称 |
| `operUrl` | HttpServletRequest | 请求URL |
| `method` | HttpServletRequest | HTTP方法（GET/POST等） |
| `operIp` | IPUtils | 客户端IP地址 |
| `operParam` | HttpServletRequest | 请求参数（JSON格式） |
| `title` | @Operation注解 | 接口描述（Swagger注解） |
| `status` | HttpServletResponse | 响应状态码 |
| `errorMsg` | Exception | 错误消息 |
| `errorData` | Exception | 异常堆栈信息 |
| `requestTime` | 计算得出 | 请求耗时（毫秒） |
| `createTimestamp` | TimeStampProvider | 创建时间戳 |

**请求参数采集规则：**

- `application/json`：从请求体读取（需要CachedBodyHttpServletRequest包装）
- `application/xml` / `text/xml`：从请求体读取
- `text/plain`：从请求体读取
- `application/x-www-form-urlencoded`：从ParameterMap读取
- `multipart/form-data`：从ParameterMap读取（不包含文件内容）
- 其他类型：从ParameterMap读取

**CachedBodyHttpServletRequest 工作原理：**

```java
// LogFilter 中执行
CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, properties);
chain.doFilter(cachedRequest, response);

// 支持的Content-Type：
// - application/json
// - application/xml
// - text/xml
// - text/plain
// - application/x-www-form-urlencoded

// 如果请求体大小超过配置限制（maxBodyCacheSize），则不缓存
```

#### 8.4 TCP客户端（LogProtobufTcpClient）

**Fire-and-Forget 模式：**

- ✅ 单向发送，不等待服务端响应
- ✅ 基于Netty实现，高性能
- ✅ 使用Protobuf序列化，减少网络传输量
- ✅ 支持断线重连（可配置最大重连次数）
- ✅ 心跳检测（30秒空闲检测）

**发送批量日志：**

```java
@Autowired
private LogProtobufTcpClient tcpClient;

// 方式1：不关心失败回调
boolean success = tcpClient.sendBatch(logBatch);

// 方式2：带失败回调（推荐）
tcpClient.sendBatch(logBatch, batch -> {
    // 发送失败时的降级逻辑
    log.error("日志发送失败，降级写入本地文件");
    writeToFallback(batch);
});
```

**配置项：**

```yaml
simple:
  log:
    tcp:
      host: localhost                    # 日志服务器地址
      port: 9090                         # 日志服务器端口
      worker-threads: 2                  # Netty Worker线程数
      connect-timeout: 5000              # 连接超时（毫秒）
      max-reconnect-attempts: 0          # 最大重连次数（0=无限重试）
      reconnect-interval: 5000           # 重连间隔（毫秒）
```

#### 8.6 配置项（Client端）

```yaml
simple:
  log:
    tcp:
      host: localhost                      # 日志服务器地址
      port: 9090                           # 日志服务器端口
      worker-threads: 2                    # Netty Worker线程数
      connect-timeout: 5000                # 连接超时（毫秒）
      max-reconnect-attempts: 0            # 最大重连次数（0=无限重试）
      reconnect-interval: 5000             # 重连间隔（毫秒）
      buffer-capacity: 10000               # 缓冲队列容量
      batch-size: 100                      # 批量发送大小
      batch-flush-interval: 5000           # 批量刷新间隔（毫秒）
      fallback-enabled: true               # 是否启用降级
      fallback-dir: ./logs/fallback        # 降级文件目录
      fallback-resend-enabled: true        # 是否启用降级恢复
      fallback-file-retention-days: 7      # 降级文件保留天数
      max-body-cache-size: 1048576         # 请求体缓存限制（字节）
```

#### 8.7 降级机制详解（Client端）

**降级触发条件：**

1. 缓冲队列已满（offer返回false）
2. 批量发送失败（TCP连接断开或服务器异常）

**降级文件结构：**

```
logs/fallback/
├── fallback.log          # 当前正在写入的降级文件
└── sent/                 # 已成功发送的降级文件（待清理）
    ├── fallback_20240101_120000.log
    └── fallback_20240101_130000.log
```

**降级文件格式（二进制）：**

```
[魔数 4字节][长度 4字节][Protobuf数据 N字节]
[魔数 4字节][长度 4字节][Protobuf数据 N字节]
...
```

**魔数校验算法：**

- 每条记录以魔数 `0xCAFEBABE` 开头
- 如果某条记录损坏，跳过该记录继续扫描下一条
- 避免因单条记录损坏导致整文件丢弃

**降级恢复流程：**

```
1. fallbackResendThread 定时扫描降级目录
   ↓
2. 找到所有 .log 文件（按修改时间排序）
   ↓
3. 逐个读取文件，解析每条记录
   ├─ 读取魔数（4字节）
   ├─ 校验魔数是否正确
   ├─ 读取长度（4字节）
   ├─ 读取Protobuf数据（N字节）
   └─ 反序列化为 LogDataEvent
   ↓
4. 调用 bufferedLogManager.send() 重新发送
   ├─ 成功：移动文件到 sent/ 目录
   └─ 失败：保留原文件，下次继续尝试
   ↓
5. 定期清理 sent/ 目录中超过保留天数的文件
```

#### 8.8 性能优化

**对象池机制：**

```java
// 从对象池获取实例（避免频繁创建对象）
LogDataEvent logDataEvent = LogDataEvent.acquire();

try {
    // 设置日志信息
    logDataEvent.setUserId("123");
    logDataEvent.setOperUrl("/api/order");
    
    // 发送日志（成功后会自动回收到对象池）
    bufferedLogManager.send(logDataEvent);
} catch (Exception e) {
    // 异常情况下必须手动回收，避免对象泄漏
    logDataEvent.recycle();
}
```

**异步降级写入：**

```java
// 队列满时，异步写入降级文件，不阻塞业务线程
if (!bufferQueue.offer(logData)) {
    fallbackExecutor.submit(() -> writeToFallback(logData));
}
```

**异步反序列化：**

```java
// 发送失败回调中，反序列化操作提交到异步线程池
// 避免阻塞 Netty I/O 线程
@Override
public void onSendFailure(LogBatch batch) {
    fallbackExecutor.submit(() -> {
        // 反序列化和降级写入逻辑
    });
}
```

#### 8.9 完整示例

**步骤1：实现自定义用户信息管理**

```java
@Component
public class CustomLogUserManager extends DefaultLogUserManager {
    
    @Autowired
    private TokenManager tokenManager;
    
    @Override
    public String loginNickName() {
        try {
            String token = RequestContextHolder.getToken();
            Map<String, Object> payload = tokenManager.check(token, false);
            return (String) payload.get("username");
        } catch (Exception e) {
            return "anonymous";
        }
    }
    
    @Override
    public String loginUserId() {
        try {
            String token = RequestContextHolder.getToken();
            Map<String, Object> payload = tokenManager.check(token, false);
            return (String) payload.get("userId");
        } catch (Exception e) {
            return null;
        }
    }
}
```

**步骤2：配置日志服务器地址**

```yaml
simple:
  log:
    tcp:
      host: 192.168.1.100
      port: 9090
      buffer-capacity: 10000
      batch-size: 100
      batch-flush-interval: 5000
      fallback-enabled: true
      fallback-dir: ./logs/fallback
      max-body-cache-size: 1048576  # 1MB
```

**步骤3：启动日志服务**

框架自动启动，无需额外配置。`LogFilter` 和 `LogInterceptor` 会被Spring自动注册，`DefaultLogService` 会在请求完成后自动调用。

---

### 9. simple-common-sms（短信服务模块）

短信服务模块，**支持多厂商适配、责任链校验、防刷机制**。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 短信服务 | `SmsService` | 短信发送和校验接口 |
| 抽象基类 | `AbsSmsService` | 实现校验逻辑，子类只需实现发送方法 |
| 阿里云实现 | `AliSmsService` | 阿里云短信服务实现 |
| 校验责任链 | `CheckSmsProcess` | 短信发送前校验接口 |
| 时间间隔校验 | `TimeIntervalBeforeSmsProcess` | 校验发送时间间隔 |
| IP次数校验 | `IpBeforeSmsProcess` | 根据IP校验每日发送次数 |
| 手机号次数校验 | `PhoneBeforeSmsProcess` | 根据手机号校验每日发送次数 |
| 校验枚举 | `BeforeSmsKindProcess` | 定义校验类型和执行顺序 |
| 配置属性 | `SmsProperties` | 短信配置参数 |

#### 9.1 架构设计

**责任链模式：**

```
发送短信
   ↓
执行校验责任链（按order排序）
   ├─ 1. TIME_INTERVAL_PROCESS: 校验发送时间间隔
   ├─ 2. IP_PROCESS: 校验IP每日发送次数
   └─ 3. PHONE_PROCESS: 校验手机号每日发送次数
   ↓
所有校验通过
   ↓
调用 sendTemplateParam() 发送短信
   ↓
记录发送结果到数据库
```

**分布式锁保护：**

```java
// AbsSmsService.sendCode() 中使用分布式锁
lockService.lock(mobile, () -> {
    // 执行校验责任链
    checkSmsProcessList.forEach(process -> {
        if (process.getProcess().isExecute()) {
            process.execution(mobile, code);
        }
    });
    // 发送短信
    sendTemplateParam(mobile, sendType, "{'code':'" + code + "'}");
});
```

**重要说明：**

- ✅ 使用分布式锁防止同一手机号并发发送
- ✅ 责任链模式灵活组合多种校验逻辑
- ✅ 每个校验处理器可独立启用/禁用
- ✅ 所有发送记录保存到数据库，支持审计和统计

#### 9.2 短信服务接口

**SmsService 提供3个核心方法：**

```java
public interface SmsService {
    
    /**
     * 发送短信验证码
     * @param mobile   手机号
     * @param code     验证码
     * @param sendType 短信类型配置标识
     */
    void sendCode(String mobile, String code, String sendType);
    
    /**
     * 发送模板短信
     * @param mobile        手机号
     * @param sendType      短信类型配置标识
     * @param templateParam 模板参数（JSON格式）
     * @param ip            客户端IP地址，用于防刷校验
     */
    void sendTemplateParam(String mobile, String sendType, String templateParam, String ip);
    
    /**
     * 校验短信验证码
     * @param mobile   手机号
     * @param code     验证码
     * @param sendType 短信类型配置标识
     */
    void checkSms(String mobile, String code, String sendType);
}
```

#### 9.3 使用示例

**发送验证码：**

```java
@Autowired
private SmsService smsService;

// 生成6位随机验证码
String code = RandomUtil.randomNumbers(6);

// 发送验证码（自动执行校验责任链 + 分布式锁保护）
smsService.sendCode("13800138000", code, "LOGIN");
```

**发送模板短信：**

```java
// 方式1：自动获取客户端IP
smsService.sendTemplateParam(
    "13800138000",
    "ORDER_NOTIFY",
    "{\"orderNo\":\"ORD123\",\"amount\":\"99.99\"}"
);

// 方式2：手动指定IP
String clientIp = IPUtils.getIpAddr();
smsService.sendTemplateParam(
    "13800138000",
    "ORDER_NOTIFY",
    "{\"orderNo\":\"ORD123\",\"amount\":\"99.99\"}",
    clientIp
);
```

**校验验证码：**

```java
try {
    // 校验验证码（会检查重试次数、过期时间、验证码正确性）
    smsService.checkSms("13800138000", "123456", "LOGIN");
    
    // 校验成功，继续业务逻辑
    loginService.login(username, password);
} catch (Exception e) {
    // 校验失败，返回错误信息
    return R.error(e.getMessage());
}
```

#### 9.4 自定义校验处理器

**实现 `CheckSmsProcess` 接口添加自定义校验：**

```java
@Component
public class BlacklistBeforeSmsProcess implements CheckSmsProcess {
    
    @Autowired
    private SysSmsCodeView sysSmsCodeView;
    
    @Override
    public DefaultKindProcess getProcess() {
        // 需要先在 BeforeSmsKindProcess 枚举中添加 BLACKLIST_PROCESS
        return BeforeSmsKindProcess.BLACKLIST_PROCESS;
    }
    
    @Override
    public void execution(String phone, String code) {
        // 检查手机号是否在黑名单中
        boolean inBlacklist = blacklistService.isInBlacklist(phone);
        AssertUtils.isFalse(inBlacklist, "该手机号已被加入黑名单");
    }
}
```

**责任链执行顺序：**

在 `BeforeSmsKindProcess` 枚举中通过 `order` 参数控制执行顺序：

```java
public enum BeforeSmsKindProcess implements DefaultKindProcess {
    
    TIME_INTERVAL_PROCESS("校验发送时间间隔", true, 1),  // 第1个执行
    IP_PROCESS("根据ip校验发送次数", true, 2),           // 第2个执行
    PHONE_PROCESS("根据手机号校验发送次数", true, 3),    // 第3个执行
    BLACKLIST_PROCESS("黑名单校验", true, 4);            // 第4个执行
    
    private final String label;   // 说明
    private final boolean execute; // 是否执行
    private final int order;       // 执行顺序
}
```

**禁用某个校验：**

将 `execute` 设置为 `false` 即可禁用：

```java
IP_PROCESS("根据ip校验发送次数", false, 2),  // 不执行IP校验
```

#### 9.5 扩展新短信平台

**继承 `AbsSmsService` 实现新的短信平台：**

```java
@Service("tencentSmsService")
public class TencentSmsService extends AbsSmsService {
    
    @Autowired
    private SysSmsTemplateView sysSmsTemplateView;
    
    @Autowired
    private SysSmsCodeView sysSmsCodeView;
    
    @Override
    protected void sendTemplateParam(String mobile, String sendType, String templateParam, String ip) {
        // 1. 获取短信模板配置
        SysSmsTemplate template = sysSmsTemplateView.findByType(sendType);
        AssertUtils.notEmpty(template, "短信模板不存在");
        
        // 2. 构建腾讯云短信请求
        SmsClient client = new SmsClient(credentials);
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumberSet(new String[]{mobile});
        request.setSmsSdkAppId(template.getAppId());
        request.setSignName(template.getSignName());
        request.setTemplateId(template.getTemplateCode());
        request.setTemplateParamSet(parseTemplateParam(templateParam));
        
        // 3. 发送短信
        SysSmsCode codeRecord = new SysSmsCode();
        codeRecord.setPhone(mobile);
        codeRecord.setSendType(sendType);
        codeRecord.setCode(templateParam);
        codeRecord.setIp(ip);
        
        try {
            SendSmsResponse response = client.SendSms(request);
            
            // 4. 记录发送结果
            if ("Ok".equals(response.getSendStatusSet()[0].getCode())) {
                codeRecord.setReqStatus(Status.OK);
                codeRecord.setReqResults(JsonUtils.toJsonStr(response));
            } else {
                codeRecord.setReqStatus(Status.ERROR);
                codeRecord.setReqResults(response.getSendStatusSet()[0].getMessage());
            }
        } catch (Exception e) {
            log.error("腾讯云短信发送异常", e);
            codeRecord.setReqStatus(Status.ERROR);
            codeRecord.setReqResults(e.getMessage());
        }
        
        // 5. 保存到数据库
        sysSmsCodeView.save(codeRecord);
        
        // 6. 如果发送失败，抛出异常
        AssertUtils.isTrue(codeRecord.getReqStatus() == Status.OK, "短信发送失败");
    }
}
```

**切换短信平台：**

通过Spring的 `@Qualifier` 或 `@Primary` 注解切换：

```java
@Configuration
public class SmsConfig {
    
    @Bean
    @Primary  // 设置为主实现
    public SmsService tencentSmsService() {
        return new TencentSmsService();
    }
}
```

#### 9.6 验证码校验机制

**AbsSmsService.checkSms() 的校验流程：**

```java
@Override
public void checkSms(String mobile, String code, String sendType) {
    // 1. Redis计数器：限制重试次数
    Long increment = redisTemplate.opsForValue().increment(mobile);
    AssertUtils.notEmpty(increment, "请重试");
    
    // 第一次进入，设置过期时间
    if (increment == 1L) {
        redisTemplate.expire(mobile, smsProperties.getOutTime(), TimeUnit.SECONDS);
    }
    
    // 检查是否超过最大重试次数
    AssertUtils.isTrue(
        increment <= smsProperties.getErrorSum(), 
        "超过最大重试次数，请重新获取验证码"
    );
    
    // 2. 从数据库查询验证码记录
    List<SysSmsCode> list = sysSmsCodeView.findByTimeAndPhoneAndState(
        new FindAllSysSmsCodeRequest()
            .setPhone(mobile)
            .setStatus(Status.NOT_USED)
            .setSendType(sendType)
            .setCode(code)
    );
    AssertUtils.notEmpty(list, "没有发送消息");
    
    // 3. 校验过期时间
    SysSmsCode sysSmsCode = list.get(0);
    long between = DateUtil.between(
        sysSmsCode.getCreateTime(), 
        DateUtil.date(), 
        DateUnit.SECOND, 
        true
    );
    AssertUtils.isTrue(
        between <= smsProperties.getOutTime(), 
        "验证码已过期，请重新获取验证码"
    );
    
    // 4. 校验验证码是否正确
    AssertUtils.isTrue(
        sysSmsCode.getCode().equals(code), 
        "验证码错误，请重新输入"
    );
    
    // 5. 标记为已使用
    sysSmsCode.setStatus(Status.USED);
    sysSmsCodeView.updateById(sysSmsCode);
}
```

**校验要点：**

1. ✅ **Redis计数器**：限制重试次数（默认3次）
2. ✅ **数据库查询**：查找未使用的验证码记录
3. ✅ **过期时间校验**：检查验证码是否过期（默认300秒）
4. ✅ **验证码比对**：验证用户输入的验证码是否正确
5. ✅ **状态更新**：校验成功后标记为已使用，防止重复使用

#### 9.7 配置项

```yaml
simple:
  ali:
    sms:
      ip-send-max: 20              # 同一IP每日最大发送次数
      phone-send-max: 5            # 同一手机号每日最大发送次数
      time-inter: 60               # 发送时间间隔（秒）
      out-time: 300                # 验证码有效期（秒）
      error-sum: 3                 # 验证码允许的错误重试次数
      endpoint: dysmsapi.aliyuncs.com  # 阿里云短信服务端点
```

#### 9.8 数据库表结构

**sys_sms_code（短信验证码记录表）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| phone | VARCHAR(20) | 手机号 |
| code | VARCHAR(100) | 验证码（JSON格式） |
| send_type | VARCHAR(50) | 短信类型 |
| status | INT | 状态（0=未使用，1=已使用，2=错误） |
| ip | VARCHAR(50) | 客户端IP |
| date | VARCHAR(20) | 日期（yyyy-MM-dd） |
| create_time | DATETIME | 创建时间 |
| req_status | INT | 请求状态（OK/ERROR） |
| req_results | TEXT | 请求结果（JSON） |

**sys_sms_template（短信模板配置表）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| type | VARCHAR(50) | 短信类型标识 |
| sign_name | VARCHAR(100) | 签名 |
| template_code | VARCHAR(100) | 模板CODE |
| app_id | VARCHAR(100) | 应用ID（腾讯云用） |
| status | INT | 状态（启用/禁用） |

#### 9.9 防刷机制

**三层防护：**

1. **分布式锁**：防止同一手机号并发发送
2. **时间间隔校验**：限制发送频率（默认60秒）
3. **每日次数限制**：
   - IP维度：同一IP每日最多20次
   - 手机号维度：同一手机号每日最多5次

**攻击场景防护：**

| 攻击场景 | 防护机制 | 效果 |
|---------|---------|------|
| 单手机号暴力发送 | 分布式锁 + 时间间隔 + 手机号次数限制 | 每分钟最多1次，每日最多5次 |
| 多手机号IP攻击 | IP次数限制 | 同一IP每日最多20次 |
| 验证码爆破 | Redis计数器 + 错误次数限制 | 最多重试3次 |
| 重放攻击 | 验证码一次性使用 | 校验后立即标记为已使用 |

---

### 10. simple-common-xxljob（定时任务模块）

XXL-JOB分布式任务调度模块，**提供代码动态管理任务的能力**。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 任务服务 | `XxlJobService` | 任务管理服务接口 |
| 默认任务服务 | `DefaultXxlJobService` | 调用Manager实现任务管理 |
| 任务管理器 | `XxlJobManager` | 任务管理接口，通过HTTP调用XXL-JOB Admin API |
| 默认任务管理 | `DefaultXxlJobManager` | 基于HttpUtils实现远程调用 |
| 创建请求DTO | `CreateXxlJobTaskRequest` | 创建任务的请求对象 |
| 更新请求DTO | `UpdateXxlJobTaskRequest` | 更新任务的请求对象 |
| 请求URL枚举 | `XxlJobRequestUrl` | 定义XXL-JOB Admin API的URL |
| 配置类 | `XxlJobConfig` | XXL-JOB配置属性 |

#### 10.1 架构设计

**三层架构：**

```
业务代码
   ↓
XxlJobService (服务层)
   ↓
XxlJobManager (管理层)
   ↓
DefaultXxlJobManager (实现层)
   ↓
HttpUtils.post() 调用 XXL-JOB Admin API
   ↓
XXL-JOB Admin 服务器
```

**重要说明：**

- ✅ **不是任务执行器**，而是**任务管理器**
- ✅ 通过HTTP调用XXL-JOB Admin API来管理任务
- ✅ 支持动态创建、修改、删除、启停任务
- ✅ 不处理任务的具体执行逻辑

#### 10.2 任务管理接口

**XxlJobService 提供6个核心方法：**

```java
public interface XxlJobService {
    
    /**
     * 创建定时任务
     * @return 任务ID字符串
     */
    String create(CreateXxlJobTaskRequest request);
    
    /**
     * 修改定时任务配置
     */
    void update(UpdateXxlJobTaskRequest request);
    
    /**
     * 删除定时任务
     */
    void delete(Integer id);
    
    /**
     * 启动定时任务
     */
    void start(Integer id);
    
    /**
     * 停止定时任务
     */
    void end(Integer id);
    
    /**
     * 立即触发任务执行
     */
    void trigger(Integer id);
}
```

#### 10.3 使用示例

**创建并启动任务：**

```java
@Autowired
private XxlJobService xxlJobService;

// 1. 构建创建请求
CreateXxlJobTaskRequest request = new CreateXxlJobTaskRequest();
request.setJobGroup(1);                        // 执行器ID
request.setJobDesc("订单超时取消任务");
request.setAuthor("system");
request.setScheduleConf("0 0/5 * * * ?");      // Cron表达式：每5分钟执行
request.setExecutorHandler("orderTimeoutHandler"); // 任务Handler名称
request.setExecutorParam("{\"timeoutMinutes\":30}"); // 任务参数
request.setExecutorRouteStrategy("FIRST");     // 路由策略：第一个
request.setExecutorBlockStrategy("SERIAL_EXECUTION"); // 阻塞策略：串行执行
request.setMisfireStrategy("DO_NOTHING");      // 过期策略：忽略
request.setExecutorTimeout(30);                // 超时时间：30秒
request.setExecutorFailRetryCount(3);          // 失败重试次数：3次

// 2. 创建任务（返回任务ID）
String jobId = xxlJobService.create(request);
log.info("任务创建成功，任务ID: {}", jobId);

// 3. 启动任务
xxlJobService.start(Integer.parseInt(jobId));
log.info("任务已启动");
```

**修改任务配置：**

```java
UpdateXxlJobTaskRequest request = new UpdateXxlJobTaskRequest();
request.setId(123);                            // 任务ID
request.setScheduleConf("0 0/10 * * * ?");     // 修改为每10分钟执行
request.setJobDesc("订单超时取消任务(已优化)");
request.setExecutorParam("{\"timeoutMinutes\":60}"); // 修改超时时间为60分钟

xxlJobService.update(request);
log.info("任务配置已更新");
```

**停止和删除任务：**

```java
Integer jobId = 123;

// 1. 先停止任务
xxlJobService.end(jobId);
log.info("任务已停止");

// 2. 再删除任务
xxlJobService.delete(jobId);
log.info("任务已删除");
```

**立即触发任务执行：**

```java
// 手动触发任务立即执行一次（不受Cron限制）
xxlJobService.trigger(jobId);
log.info("任务已触发");

// 可以用于测试新创建的任务是否正常
```

#### 10.4 任务处理器定义

**在业务项目中定义任务处理器：**

```java
@Component
public class OrderTimeoutJobHandler {
    
    /**
     * 任务Handler名称必须与 CreateXxlJobTaskRequest.executorHandler 一致
     */
    @XxlJob("orderTimeoutHandler")
    public void execute(String param) throws Exception {
        log.info("开始执行订单超时取消任务，参数: {}", param);
        
        // 解析参数
        JSONObject params = JSON.parseObject(param);
        int timeoutMinutes = params.getIntValue("timeoutMinutes");
        
        // 业务逻辑：查询超时订单并取消
        List<Order> timeoutOrders = orderService.findTimeoutOrders(timeoutMinutes);
        for (Order order : timeoutOrders) {
            orderService.cancelOrder(order.getId(), "订单超时自动取消");
        }
        
        log.info("订单超时取消任务执行完成，共取消 {} 个订单", timeoutOrders.size());
    }
}
```

**重要说明：**

- ✅ `@XxlJob` 注解的值必须与 `executorHandler` 字段一致
- ✅ 任务处理器必须是Spring Bean（添加 `@Component`）
- ✅ 方法可以接收String类型的参数
- ✅ 抛出异常会被XXL-JOB捕获并记录

#### 10.5 配置项

```yaml
simple:
  xxl:
    job:
      admin-addresses: http://localhost:8080/xxl-job-admin  # XXL-JOB Admin地址
      access-token: default_token                            # 访问令牌
      request-timeout: 5000                                  # HTTP请求超时（毫秒）
```

#### 10.6 CreateXxlJobTaskRequest 参数详解

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `jobGroup` | Integer | 是 | - | 执行器ID |
| `jobDesc` | String | 是 | - | 任务描述 |
| `author` | String | 是 | - | 负责人 |
| `scheduleConf` | String | 是 | - | Cron表达式 |
| `executorHandler` | String | 是 | - | 任务Handler名称 |
| `scheduleType` | String | 否 | CRON | 调度类型 |
| `glueType` | String | 否 | BEAN | 运行模式 |
| `executorRouteStrategy` | String | 否 | FIRST | 路由策略 |
| `executorBlockStrategy` | String | 否 | SERIAL_EXECUTION | 阻塞策略 |
| `misfireStrategy` | String | 否 | DO_NOTHING | 过期策略 |
| `executorParam` | String | 否 | - | 任务参数（JSON） |
| `alarmEmail` | String | 否 | - | 报警邮件 |
| `childJobId` | String | 否 | - | 子任务ID |
| `executorTimeout` | int | 否 | 30 | 执行超时时间（秒） |
| `executorFailRetryCount` | int | 否 | 3 | 失败重试次数 |

**路由策略选项：**

- `FIRST`：第一个
- `LAST`：最后一个
- `ROUND`：轮询
- `RANDOM`：随机
- `CONSISTENT_HASH`：一致性HASH
- `LEAST_FREQUENTLY_USED`：最不经常使用
- `LEAST_RECENTLY_USED`：最近最久未使用
- `FAILOVER`：故障转移
- `BUSYOVER`：忙碌转移
- `SHARDING_BROADCAST`：分片广播

**阻塞策略选项：**

- `SERIAL_EXECUTION`：单机串行
- `DISCARD_LATER`：丢弃后续调度
- `COVER_EARLY`：覆盖之前调度

**过期策略选项：**

- `DO_NOTHING`：忽略
- `FIRE_ONCE_NOW`：立即执行一次

#### 10.7 典型应用场景

**场景1：动态创建定时任务**

```java
@Service
public class DynamicJobService {
    
    @Autowired
    private XxlJobService xxlJobService;
    
    /**
     * 根据用户配置动态创建任务
     */
    public String createCustomJob(JobConfig config) {
        CreateXxlJobTaskRequest request = new CreateXxlJobTaskRequest();
        request.setJobGroup(config.getExecutorId());
        request.setJobDesc(config.getJobName());
        request.setAuthor(config.getCreator());
        request.setScheduleConf(config.getCronExpression());
        request.setExecutorHandler(config.getHandlerName());
        request.setExecutorParam(config.getParams());
        
        String jobId = xxlJobService.create(request);
        xxlJobService.start(Integer.parseInt(jobId));
        
        return jobId;
    }
}
```

**场景2：任务生命周期管理**

```java
@Service
public class JobLifecycleService {
    
    @Autowired
    private XxlJobService xxlJobService;
    
    /**
     * 系统维护期间暂停所有任务
     */
    public void pauseAllJobs(List<Integer> jobIds) {
        for (Integer jobId : jobIds) {
            try {
                xxlJobService.end(jobId);
                log.info("任务已暂停: {}", jobId);
            } catch (Exception e) {
                log.error("暂停任务失败: {}", jobId, e);
            }
        }
    }
    
    /**
     * 维护完成后恢复所有任务
     */
    public void resumeAllJobs(List<Integer> jobIds) {
        for (Integer jobId : jobIds) {
            try {
                xxlJobService.start(jobId);
                log.info("任务已恢复: {}", jobId);
            } catch (Exception e) {
                log.error("恢复任务失败: {}", jobId, e);
            }
        }
    }
}
```

**场景3：测试任务逻辑**

```java
@RestController
@RequestMapping("/test/job")
public class JobTestController {
    
    @Autowired
    private XxlJobService xxlJobService;
    
    /**
     * 手动触发任务执行，用于测试
     */
    @PostMapping("/trigger/{jobId}")
    public R triggerJob(@PathVariable Integer jobId) {
        try {
            xxlJobService.trigger(jobId);
            return R.ok("任务已触发");
        } catch (Exception e) {
            return R.error("触发失败: " + e.getMessage());
        }
    }
}
```

---

### 11. simple-common-doc（Word文档模板模块）

Word文档模板替换模块，**基于poi-tl引擎实现**，支持**文本、图片、表格、列表等多种元素替换**。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 文档服务 | `DocReplaceService` | 文档替换服务接口，提供多种便捷方法 |
| 默认文档服务 | `DefaultDocReplaceService` | 调用Manager实现文档替换 |
| 模板管理 | `DocTemplateReplaceManager` | 文档模板替换管理器接口 |
| PoiTl模板管理 | `PoiTlTemplateReplaceManager` | 基于poi-tl引擎的实现 |
| 文档构建器 | `Docs.DocBuilder` | **核心工具**：链式API构建模板数据 |
| 配置类 | `DocConfig` | 文档模块配置 |

#### 11.1 架构设计

**三层架构：**

```
业务代码
   ↓
DocReplaceService (服务层)
   ├─ replace() - 基础替换
   ├─ replaceResponse() - HTTP响应下载
   └─ replaceAndGetInputStream() - 返回流
   ↓
DocTemplateReplaceManager (管理层)
   ↓
PoiTlTemplateReplaceManager (实现层)
   ↓
XWPFTemplate.compile().render() (poi-tl引擎)
```

**重要说明：**

- ✅ **不是API文档生成**，而是**Word文档模板替换**
- ✅ 基于 poi-tl 引擎（https://deepoove.com/poi-tl/）
- ✅ 支持文本、图片、表格、列表等多种元素
- ✅ 提供链式API构建模板数据

#### 11.2 模板语法

**poi-tl 支持的模板语法：**

| 语法 | 说明 | 示例 |
|------|------|------|
| `{{key}}` | 文本替换 | `{{contractNo}}` |
| `{{@key}}` | 图片替换 | `{{@signature}}` |
| `{{#key}}` | 表格循环 | `{{#orderList}}...{{/orderList}}` |
| `{{*key}}` | 列表循环 | `{{*items}}` |
| `{{?key}}` | 条件判断 | `{{?hasDiscount}}...{{/hasDiscount}}` |

#### 11.3 Docs.DocBuilder 构建器

**Docs 提供链式API构建模板数据：**

```java
// 1. 创建构建器
Docs.DocBuilder builder = Docs.builder();

// 2. 添加普通文本
builder.addStr("contractNo", "HT20240115001")
       .addStr("partyA", "甲方公司名称")
       .addStr("partyB", "乙方公司名称");

// 3. 添加带样式的文本
builder.addStrColor("amount", "¥100,000.00", "FF0000")  // 红色字体
       .addStrLink("website", "访问官网", "https://example.com");

// 4. 添加图片
builder.addImgLocal("logo", "/path/to/logo.png", 100, 50)  // 本地图片
       .addImgInputUrl("qrcode", "https://example.com/qr.png", 200, 200)  // 网络图片
       .addImgInputStream("signature", inputStream, 150, 80);  // 图片流

// 5. 添加表格
String[] headers = {"订单号", "商品名称", "数量", "金额"};
List<Order> orders = orderService.list();
builder.addTable("orderTable", "90%", headers, orders, order -> {
    return new String[]{
        order.getOrderNo(),
        order.getProductName(),
        String.valueOf(order.getQuantity()),
        order.getAmount().toString()
    };
});

// 6. 添加列表
List<String> items = Arrays.asList("项目1", "项目2", "项目3");
builder.addList("itemList", items);

// 7. 获取参数Map
Map<String, Object> templateData = builder.create();
```

**DocBuilder 方法详解：**

| 方法 | 说明 | 模板语法 | 参数 |
|------|------|---------|------|
| `addStr(key, value)` | 添加普通文本 | `{{key}}` | key: 占位符<br>value: 文本值 |
| `addStrColor(key, value, color)` | 添加彩色文本 | `{{key}}` | color: 颜色代码（如FF0000） |
| `addStrLink(key, value, link)` | 添加链接文本 | `{{key}}` | link: 超链接地址 |
| `addImgLocal(key, url, w, h)` | 添加本地图片 | `{{@key}}` | url: 本地路径<br>w/h: 宽高 |
| `addImgInputUrl(key, url, w, h)` | 添加网络图片 | `{{@key}}` | url: 网络URL |
| `addImgInputStream(key, is, w, h)` | 添加图片流 | `{{@key}}` | is: InputStream |
| `addTable(key, width, headers, list, fn)` | 添加表格 | `{{#key}}` | width: 宽度百分比<br>fn: 数据转换函数 |
| `addList(key, list)` | 添加无序列表 | `{{*key}}` | list: 字符串列表 |
| `addList(key, format, list)` | 添加有序列表 | `{{*key}}` | format: 编号格式 |

#### 11.4 使用示例

**场景1：生成合同文档并下载**

```java
@RestController
@RequestMapping("/contract")
public class ContractController {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    @GetMapping("/download/{orderId}")
    public void downloadContract(@PathVariable String orderId, HttpServletResponse response) {
        // 1. 查询订单数据
        Order order = orderService.findById(orderId);
        
        // 2. 构建模板数据
        Map<String, Object> data = Docs.builder()
            .addStr("contractNo", order.getContractNo())
            .addStr("partyA", order.getPartyA())
            .addStr("partyB", order.getPartyB())
            .addStrColor("amount", "¥" + order.getAmount(), "FF0000")
            .addStr("signDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
            .create();
        
        // 3. 一行代码完成：加载模板 + 替换 + HTTP响应
        docReplaceService.replaceResponse(
            "合同_" + order.getContractNo(),
            "/templates/contract.docx",
            data
        );
    }
}
```

**场景2：生成证书并上传OSS**

```java
@Service
public class CertificateService {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    @Autowired
    private OssService ossService;
    
    public String generateCertificate(String userId) {
        // 1. 查询用户数据
        User user = userService.findById(userId);
        
        // 2. 构建模板数据
        Map<String, Object> data = Docs.builder()
            .addStr("userName", user.getName())
            .addStr("courseName", "Java高级编程")
            .addStr("completeDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
            .addImgLocal("seal", "/images/seal.png", 100, 100)
            .create();
        
        // 3. 生成文档并获取输入流
        ByteArrayInputStream inputStream = docReplaceService.replaceAndGetInputStream(
            "/templates/certificate.docx",
            data
        );
        
        // 4. 上传到OSS
        String objectKey = "certificates/" + userId + ".docx";
        ossService.upload(objectKey, inputStream);
        
        return objectKey;
    }
}
```

**场景3：生成包含表格的报告**

```java
@GetMapping("/report/monthly")
public void downloadMonthlyReport(HttpServletResponse response) {
    // 1. 查询月度数据
    List<MonthlyData> dataList = reportService.getMonthlyData();
    
    // 2. 构建模板数据
    String[] headers = {"日期", "销售额", "订单数", "客单价"};
    Map<String, Object> data = Docs.builder()
        .addStr("reportTitle", "2024年1月销售报告")
        .addStr("period", "2024-01-01 至 2024-01-31")
        .addTable("dataTable", "95%", 12, headers, dataList, item -> {
            return new String[]{
                item.getDate(),
                item.getSalesAmount().toString(),
                String.valueOf(item.getOrderCount()),
                item.getAvgAmount().toString()
            };
        })
        .addList("summary", Arrays.asList(
            "本月销售额环比增长15%",
            "订单数同比增长20%",
            "客单价保持稳定"
        ))
        .create();
    
    // 3. 生成并下载
    docReplaceService.replaceResponse(
        "月度销售报告_202401",
        "/templates/monthly_report.docx",
        data
    );
}
```

#### 11.5 DocReplaceService 方法详解

**提供4种便捷方法：**

```java
public interface DocReplaceService {
    
    /**
     * 1. 基础替换：从输入流读取模板，输出到指定流
     */
    void replace(InputStream inputStream, OutputStream outputStream, Map<String, Object> values);
    
    /**
     * 2. HTTP响应下载：从流加载模板
     */
    default void replaceResponse(String name, InputStream inputStream, Map<String, Object> values);
    
    /**
     * 3. HTTP响应下载：从resources加载模板（最常用）
     */
    default void replaceResponse(String name, String templatePath, Map<String, Object> values);
    
    /**
     * 4. 返回输入流：用于上传OSS、发送邮件等
     */
    default ByteArrayInputStream replaceAndGetInputStream(String templatePath, Map<String, Object> values);
    
    /**
     * 5. 返回输出流：用于获取字节数组
     */
    default ByteArrayOutputStream replaceAndGetOutputStream(String templatePath, Map<String, Object> values);
}
```

#### 11.6 模板文件放置

**模板文件应放在 resources 目录下：**

```
src/main/resources/
├── templates/
│   ├── contract.docx          # 合同模板
│   ├── certificate.docx       # 证书模板
│   └── monthly_report.docx    # 报告模板
└── images/
    ├── logo.png               # Logo图片
    └── seal.png               # 印章图片
```

**加载方式：**

```java
// 方式1：直接传路径（推荐）
docReplaceService.replaceResponse("合同", "/templates/contract.docx", data);

// 方式2：手动加载流
InputStream inputStream = getClass().getResourceAsStream("/templates/contract.docx");
docReplaceService.replaceResponse("合同", inputStream, data);
```

#### 11.7 完整示例：合同生成系统

**步骤1：准备Word模板**

在 Word 中创建 `contract.docx`，使用以下占位符：

```
合 同 编 号：{{contractNo}}

甲    方：{{partyA}}
乙    方：{{partyB}}

合同金额：{{amount}}
签订日期：{{signDate}}

甲方签字：{{@partyASignature}}
乙方签字：{{@partyBSignature}}

公司盖章：{{@companySeal}}
```

**步骤2：实现合同服务**

```java
@Service
public class ContractGenerationService {
    
    @Autowired
    private DocReplaceService docReplaceService;
    
    @Autowired
    private SignatureService signatureService;
    
    /**
     * 生成合同文档
     */
    public byte[] generateContract(String orderId) {
        // 1. 查询订单信息
        Order order = orderService.findById(orderId);
        
        // 2. 获取电子签名
        InputStream partyASignature = signatureService.getSignature(order.getPartyAUserId());
        InputStream partyBSignature = signatureService.getSignature(order.getPartyBUserId());
        
        // 3. 构建模板数据
        Map<String, Object> data = Docs.builder()
            .addStr("contractNo", order.getContractNo())
            .addStr("partyA", order.getPartyA())
            .addStr("partyB", order.getPartyB())
            .addStrColor("amount", "¥" + order.getAmount(), "FF0000")
            .addStr("signDate", order.getSignDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
            .addImgInputStream("partyASignature", partyASignature, 150, 80)
            .addImgInputStream("partyBSignature", partyBSignature, 150, 80)
            .addImgLocal("companySeal", "/images/seal.png", 100, 100)
            .create();
        
        // 4. 生成文档并返回字节数组
        ByteArrayOutputStream outputStream = docReplaceService.replaceAndGetOutputStream(
            "/templates/contract.docx",
            data
        );
        
        return outputStream.toByteArray();
    }
}
```

**步骤3：Controller提供下载**

```java
@RestController
@RequestMapping("/api/contract")
public class ContractController {
    
    @Autowired
    private ContractGenerationService contractService;
    
    @GetMapping("/generate/{orderId}")
    public void generateAndDownload(@PathVariable String orderId, HttpServletResponse response) {
        byte[] contractBytes = contractService.generateContract(orderId);
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"contract_" + orderId + ".docx\"");
        response.setContentLength(contractBytes.length);
        
        // 写入响应
        try (OutputStream os = response.getOutputStream()) {
            os.write(contractBytes);
            os.flush();
        }
    }
}
```

---

### 12. simple-common-excel（Excel处理模块）

Excel处理模块，**支持EasyExcel和Apache POI双引擎**，提供**读取、写入、导出**功能。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| EasyExcel读取 | `EasyExcelReadService` | 基于阿里巴巴EasyExcel的流式读取，内存占用低 |
| EasyExcel写入 | `EasyExcelWriteService` | 基于EasyExcel的快速写入 |
| POI读取 | `PoiReadService` | 基于Apache POI的灵活读取 |
| POI写入 | `PoiWriteService` | 基于POI的高级写入，支持复杂样式 |

#### 12.1 EasyExcel读取示例

```java
@Autowired
private EasyExcelReadService readService;

@PostMapping("/import/users")
public R importUsers(@RequestParam("file") MultipartFile file) {
    List<UserDTO> userList = new ArrayList<>();
    
    readService.read(file, 1, UserDTO.class, new ReadListener<UserDTO>() {
        @Override
        public void invoke(UserDTO data, AnalysisContext context) {
            userList.add(data);
        }
        
        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            userService.saveBatch(userList);
            log.info("导入成功，共{}条数据", userList.size());
        }
    });
    
    return R.ok("导入成功");
}
```

#### 12.2 EasyExcel写入示例

```java
@Autowired
private EasyExcelWriteService writeService;

@GetMapping("/export/users")
public void exportUsers(HttpServletResponse response) {
    List<UserDTO> users = userService.list();
    
    writeService.exportResponse(
        users,
        UserDTO.class,
        "用户列表"
    );
}
```

#### 12.3 POI写入示例（复杂报表）

```java
@Autowired
private PoiWriteService poiWriteService;

@GetMapping("/export/orders")
public void exportOrders(HttpServletResponse response) {
    List<OrderDTO> orders = orderService.list();
    
    String[] headers = {"订单号", "商品名称", "数量", "金额"};
    Integer[] widths = {20, 30, 10, 15};
    
    poiWriteService.exportResponse(
        (rowNum, order) -> new Object[]{
            order.getOrderNo(),
            order.getProductName(),
            order.getQuantity(),
            order.getAmount()
        },
        orders,
        headers,
        widths,
        1000,  // 每个Sheet最多1000行
        "订单列表"
    );
}
```

---

### 13. simple-common-annex（附件管理模块）

附件管理模块，**基于S3协议实现**，支持MinIO、阿里云OSS、AWS S3等存储服务。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 附件服务 | `AnnexService` | 附件上传、下载、删除接口 |
| S3附件服务 | `S3AnnexService` | 基于S3协议的实现 |
| S3管理器 | `S3Manager` | S3操作管理器 |
| 上传响应DTO | `UploadResponse` | 上传结果封装 |
| 共享类型枚举 | `ShareType` | PUBLIC（公开）或 PRIVATE（私有） |
| 算法枚举 | `Algorithm` | MD5等文件校验算法 |
| 上传函数 | `UploadFunction` | 自定义上传逻辑 |

#### 13.1 使用示例

**上传文件：**

```java
@Autowired
private AnnexService annexService;

@PostMapping("/upload/avatar")
public R<UploadResponse> uploadAvatar(@RequestParam MultipartFile file) {
    UploadResponse response = annexService.upload(
        file,
        "user-service",     // 应用名称
        "avatars",          // 包名（目录）
        ShareType.PUBLIC    // 公开访问
    );
    
    return R.ok(response);
}
```

**生成临时访问URL：**

```java
// 为私有文件生成临时访问链接（默认有效期由配置决定）
String url = annexService.generateUrl(objectUrl);
```

**删除文件：**

```java
annexService.delete(objectUrl);
```

---

### 14. simple-common-ai（AI集成模块）

AI集成模块，提供大语言模型调用能力。

> **注**：该模块处于开发阶段，具体功能待完善。

---

### 15. simple-common-alibaba（阿里云服务集成）

阿里云服务集成模块。

> **注**：该模块用于整合阿里云相关服务（如短信、OSS等），具体功能由各子模块提供。

---

### 16. simple-common-mp（微信公众号模块）

微信公众号模块，提供微信消息处理、菜单管理等功能。

> **注**：该模块基于微信官方SDK封装，简化公众号开发流程。

---

## 核心扩展机制说明

### 一、责任链模式扩展

框架大量使用责任链模式，通过实现 `BasProcessService` 接口并返回对应的枚举值来扩展功能。

#### 扩展步骤

1. **实现接口或继承抽象类（建议先创立节点，例如AbsLoginErrorProcess。具体的任务再集成AbsLoginErrorProcess）**
2. **实现 `getProcess()` 方法**，返回对应的枚举值
3. **实现业务逻辑方法**
4. **添加 `@Component` 注解**，让Spring自动扫描

#### 示例：自定义认证处理

```java
@Component
public class CustomAuthProcess implements AuthProcess {
    
    @Override
    public DefaultKindProcess getProcess() {
        return AuthInterceptorKindProcess.CHECK_TOKEN;
    }
    
    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response, 
                       String token, String path, String ipAddr) {
        // 自定义认证逻辑
    }
}
```

### 二、抽象基类扩展

对于复杂的功能，框架提供了抽象基类，简化扩展过程。

#### 扩展示例：自定义登录错误处理

```java
@Component
public class CustomLoginErrorProcess extends AbsLoginErrorProcess {
    
    @Override
    public LoginErrorKindProcess getProcess() {
        return LoginErrorKindProcess.ACCOUNT_ERROR;
    }
    
    @Override
    protected String getLoginKey(ClientDetails clientDetails, Object adapter, String ip) {
        // 自定义登录标识key
        return ((LoginRequest) adapter).getUsername();
    }
    
    @Override
    protected String getKeyPrefix() {
        return "login:error:";
    }
}
```

**注意**：`AbsLoginErrorProcess` 已经实现了 `checkErrorNum()`、`recordError()`、`clearError()` 方法，子类只需实现三个抽象方法：
- `getProcess()` - 返回处理器类型
- `getLoginKey()` - 获取登录标识
- `getKeyPrefix()` - 获取Redis key前缀

### 三、配置类扩展

通过继承配置抽象类，自定义框架行为。

#### 扩展示例：自定义客户端权限配置

```java
@Configuration
public class MyAuthConfig extends AbsClientAuthConfig {
    
    @Override
    protected void configure(ClientAuthInfo clientAuthInfo) {
        // 开启全局登录校验
        clientAuthInfo.openLogin();
        
        // 配置白名单
        clientAuthInfo.antMatchers("/api/public/**").permitAll();
        
        // 配置角色权限
        clientAuthInfo.antMatchers("/api/admin/**").onlyRole("admin");
    }
}
```

---

## 快速开始

### 1. 引入依赖

在 `pom.xml` 中添加父POM：

```xml
<parent>
    <groupId>com.simple</groupId>
    <artifactId>simple-common</artifactId>
    <version>1.0.0</version>
</parent>
```

按需引入模块：

```xml
<!-- 核心模块（必选） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-core</artifactId>
</dependency>

<!-- 认证授权模块 -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-auth-client</artifactId>
</dependency>

<!-- Redis缓存模块 -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-redis</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
spring:
  application:
    name: my-application
  
  # Redis配置
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

# Simple-Common配置
simple:
  auth:
    server-url: http://localhost:8000  # 认证服务器地址
    login-error-number: 5              # 登录失败最大次数
    login-error-time: 86400            # 失败计次时间窗口（秒）
  
  thread:
    core-size: 10                      # 线程池核心线程数
    max-size: 100                      # 最大线程数
    queue-capacity: 1000               # 队列容量
```

### 3. 启动类

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 最佳实践

### 1. 认证授权配置

✅ **推荐做法**：

```java
@Configuration
public class AuthConfig extends AbsClientAuthConfig {
    @Override
    protected void configure(ClientAuthInfo info) {
        // 明确开启所需功能
        info.openLogin();
        info.openAuthentication();
        
        // 精确配置白名单
        info.antMatchers("/api/public/**").permitAll();
        
        // 细化权限控制
        info.antMatchers("/api/admin/**").onlyRole("admin");
    }
}
```

❌ **避免做法**：

```java
// 不要放行所有接口
info.antMatchers("/**").permitAll();
```

### 2. 缓存使用

✅ **推荐做法**：

```java
// 使用有意义的key前缀
cacheManager.set("user:profile:" + userId, profile, 30, MINUTES);

// 设置合理的过期时间
cacheManager.set("hot:data", data, 5, MINUTES);  // 热点数据短过期
cacheManager.set("config:global", config, 24, HOURS);  // 配置长过期
```

### 3. 责任链扩展

✅ **推荐做法**：

```java
// 实现getProcess()方法，返回正确的枚举
@Override
public DefaultKindProcess getProcess() {
    return LoginErrorKindProcess.ACCOUNT_ERROR;
}

// 继承抽象基类，复用通用逻辑
public class CustomProcess extends AbsLoginErrorProcess {
    // 只需实现抽象方法
}
```

❌ **避免做法**：

```java
// 不要忘记实现getProcess()方法
// 不要直接实现接口而不继承抽象基类（会丢失通用逻辑）
```

---

## 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范

1. **代码风格**：遵循阿里巴巴Java开发手册
2. **注释规范**：所有公共接口必须有Javadoc注释
3. **命名规范**：
   - service后缀：核心功能接口
   - manager后缀：内部支撑接口
   - Process后缀：责任链接口
   - Abs前缀：抽象基类

### 提交流程

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## License

MIT License

---

## 联系方式

- **作者**：

---

## 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Redisson](https://github.com/redisson/redisson)
- [RabbitMQ](https://www.rabbitmq.com/)
- [XXL-JOB](https://github.com/xuxueli/xxl-job)

---

**如果这个项目对您有帮助，请给个Star！**

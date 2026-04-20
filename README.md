# simple-common 框架文档

## 项目概述

`simple-common` 是一个基于 Spring Boot 的企业级开发框架，旨在减少重复造轮子，提升开发效率。框架采用模块化设计，各功能模块可独立引入，支持新旧项目无侵入集成。

### 核心特性

- **模块化设计**：各功能模块独立封装，按需引入，减少依赖冗余
- **开箱即用**：提供默认实现，快速集成，无需繁琐配置
- **高度可扩展**：通过继承和接口实现，轻松扩展自定义逻辑
- **统一规范**：统一的命名规范、异常处理、响应封装，提升代码质量
- **无侵入集成**：可在现有项目中直接引入使用，无需修改原有代码

### 适用场景

- **新项目快速启动**：直接使用框架提供的功能模块，快速搭建项目
- **旧项目功能增强**：按需引入特定模块，无侵入式增强项目功能
- **微服务架构**：模块化设计支持微服务拆分，单体应用可平滑过渡到微服务

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
| 异常处理 | `DefaultException`、`DefaultExceptionHandler` | 统一异常体系 |
| 响应封装 | `R` | 统一响应对象 |
| 分布式锁 | `LockService`、`AbsLockService` | 分布式锁服务接口 |
| 循环任务 | `CycleService`、`AbsCycleService` | 循环任务处理服务 |
| 责任链基础 | `BasProcessService`、`DefaultKindProcess` | 责任链模式基础接口 |

#### ThreadUtils 使用示例

```java
// 异步执行任务
ThreadUtils.execute(() -> {
    // 业务逻辑
});

// 延迟执行任务（5秒后执行）
ThreadUtils.schedule(() -> {
    // 业务逻辑
}, 5, TimeUnit.SECONDS);

// 定时执行任务（每60秒执行一次）
ThreadUtils.scheduleAtFixedRate(() -> {
    // 业务逻辑
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

#### simple-common-auth-client（客户端模块）

客户端认证模块，用于资源服务端的认证拦截和权限校验。

##### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| Token管理 | `TokenManager` | Token解析、验证、创建 |
| 白名单管理 | `WhiteManager` | URL白名单配置 |
| 登录信息管理 | `LoginInfoManager` | 获取当前登录用户信息 |
| 签名校验 | `SignManager` | 接口签名校验 |
| 缓存管理 | `CacheManager` | 认证相关缓存管理 |
| CSRF防御 | `CsrfService` | CSRF Token生成和验证 |
| 责任链处理 | `AuthProcess` | 认证拦截责任链处理接口 |

##### 责任链模式

认证拦截采用责任链模式，通过 `AuthProcess` 接口和 `AuthInterceptorKindProcess` 枚举定义处理流程：

```java
// 责任链枚举定义
public enum AuthInterceptorKindProcess implements DefaultKindProcess {
    CHECK_TOKEN(1, "token校验", CheckTokenAuthProcess.class),
    CHECK_ROLE(2, "角色校验", CheckRoleAuthProcess.class),
    CHECK_SCOPE(3, "授权范围校验", CheckScopeAuthProcess.class);
}
```

##### 扩展指南

**自定义认证处理逻辑**：实现 `AuthProcess` 接口

```java
@Component
public class CustomAuthProcess implements AuthProcess {
    @Override
    public void process(String token, String path, String ipAddr) {
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
        // 添加白名单URL
        clientAuthInfo.getWhiteUrls().add("/api/public/**");
        // 配置权限信息
        clientAuthInfo.getOperations().add(new UrlOperation("/api/user/**", "user:read"));
    }
}
```

**获取当前登录用户**：使用 `LoginUserUtils`

```java
// 获取当前登录用户ID
String userId = LoginUserUtils.getUserId();

// 获取当前登录用户信息
UserTemporary user = LoginUserUtils.getUser();
```

#### simple-common-auth-server（服务端模块）

认证服务端模块，用于认证服务器的登录处理和Token生成。

##### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 登录管理 | `LoginManager` | 登录处理管理器 |
| 登录服务 | `LoginService` | 登录服务接口 |
| 客户端管理 | `ClientManager` | 客户端信息管理 |
| 客户端服务 | `ClientDetailsService` | 客户端详情服务 |
| JWT秘钥管理 | `JwtSecretManager` | JWT秘钥管理 |
| 用户操作管理 | `LoginUserOperationManager` | 登录用户操作管理 |
| 登录用户服务 | `LoginUserService` | 登录用户服务 |
| 登录错误处理 | `LoginErrorProcess` | 登录错误责任链处理 |
| 登录成功处理 | `LoginSucProcess` | 登录成功责任链处理 |

##### 扩展指南

**自定义登录方式**：继承 `AbsLoginManager`

```java
@Component
public class PwdLoginManager extends AbsLoginManager<PwdLoginRequest> {
    @Override
    protected AbsUserDetails doLogin(PwdLoginRequest request, ClientDetails clientDetails) {
        // 验证账号密码
        SysUser user = userService.findByUsername(request.getUsername());
        AssertUtils.isTrue(user != null, LoginException.USER_NOT_FOUND);
        AssertUtils.isTrue(passwordEncoder.matches(request.getPassword(), user.getPassword()), 
                          LoginException.PASSWORD_ERROR);
        return user;
    }
}
```

**自定义客户端信息获取**：继承 `AbsClientDetailsService`

```java
@Service
public class MyClientDetailsService extends AbsClientDetailsService {
    @Autowired
    private ClientRepository clientRepository;
    
    @Override
    protected ClientDetails getClientDetails(String clientId, String clientSecret) {
        SysClientDetails entity = clientRepository.findByClientId(clientId);
        AssertUtils.isTrue(entity != null, "客户端不存在");
        AssertUtils.isTrue(entity.getClientSecret().equals(clientSecret), "客户端密钥错误");
        return convertToClientDetails(entity);
    }
}
```

**自定义登录错误处理**：实现 `LoginErrorProcess`

```java
@Component
public class AccountLoginErrorProcess extends AbsLoginErrorProcess {
    @Override
    protected String getLoginKey(ClientDetails clientDetails, Object adapter, String ip) {
        PwdLoginRequest request = (PwdLoginRequest) adapter;
        return request.getUsername();
    }
    
    @Override
    protected String getKeyPrefix() {
        return "login:error:account:";
    }
}
```

---

### 3. simple-common-eventbus（事件总线模块）

事件总线模块，提供事件驱动架构支持，支持同步和异步两种事件处理方式。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 事件发送 | `EventBusService` | 事件发送服务接口 |
| 事件处理器管理 | `EventHandlerManager` | 事件处理器注册和管理 |
| 循环事件处理 | `AbsEventCycleService` | 循环事件处理抽象基类 |

#### 事件定义规范

```java
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class UserCreatedEvent {
    private String userId;
    private String username;
}
```

#### 事件处理器定义

```java
@Component
public class UserEventHandler {
    @EventHandler
    public void handleUserCreated(UserCreatedEvent event) {
        // 处理逻辑
        log.info("用户创建事件: {}", event.getUserId());
    }
}
```

#### 事件发送

```java
@Autowired
private EventBusService eventBusService;

// 同步发送事件
eventBusService.send(new UserCreatedEvent().setUserId("123"));

// 异步发送事件（通过RabbitMQ）
eventBusService.sendAsync(new UserCreatedEvent().setUserId("123"));

// 延迟发送事件
eventBusService.sendDelay(new UserCreatedEvent().setUserId("123"), 5, TimeUnit.SECONDS);
```

#### 扩展指南

**自定义循环事件处理**：继承 `AbsEventCycleService`

```java
@Service
public class DefaultCycleService extends AbsEventCycleService<DataDemo> {
    @Override
    public void execute(DataDemo data) {
        // 任务执行逻辑
        log.info("执行循环任务: {}", data);
    }
    
    @Override
    public void complete(DataDemo data) {
        // 任务完成回调
        log.info("任务完成: {}", data);
    }
}
```

---

### 4. simple-common-websocket（WebSocket模块）

WebSocket通讯模块，提供WebSocket服务端功能，支持通道管理和消息监听。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| WebSocket鉴权 | `CheckWebSocketManager` | WebSocket连接鉴权管理 |
| 消息监听管理 | `WebSocketListeningManager` | WebSocket消息监听管理 |
| 通道管理 | `WebSocketUtils` | WebSocket通道管理工具 |

#### 扩展指南

**自定义WebSocket鉴权逻辑**：继承 `DefaultCheckWebSocketManager`

```java
@Component
public class MyCheckWebSocketManager extends DefaultCheckWebSocketManager {
    @Override
    public boolean check(ChannelHandlerContext chc, WebSocketRequest request) {
        // 自定义鉴权逻辑
        String token = request.getToken();
        // 验证token
        return validateToken(token);
    }
}
```

**WebSocket消息监听**：使用 `@WebSocketListening` 注解

```java
@Component
public class WebSocketMessageHandler {
    @WebSocketListening(type = "order")
    public void handleOrderMessage(String message) {
        // 处理订单消息
        log.info("收到订单消息: {}", message);
    }
}
```

**WebSocket通道操作**：使用 `WebSocketUtils`

```java
// 添加通道
WebSocketUtils.addMap("order", "client123", chc);

// 发送消息
WebSocketUtils.sendMsg("order", "client123", "消息内容");

// 移除通道
WebSocketUtils.removeMap("order", "client123");
```

---

### 5. simple-common-logs（日志模块）

日志模块，分为客户端和服务端两个子模块，提供高性能的日志收集和处理功能。

#### simple-common-logs-client（客户端模块）

日志客户端模块，用于应用端的日志收集和发送。

##### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 日志发送 | `LogService` | 日志发送服务接口 |
| 用户信息获取 | `LogUserManager` | 日志用户信息获取管理 |

##### 扩展指南

**自定义日志用户信息获取**：继承 `DefaultLogUserManager`

```java
@Component
public class MyLogUserManager extends DefaultLogUserManager {
    @Override
    public String getUserId() {
        // 自定义获取用户ID逻辑
        return LoginUserUtils.getUserId();
    }
    
    @Override
    public String getUserName() {
        // 自定义获取用户名逻辑
        return LoginUserUtils.getUserName();
    }
}
```

#### simple-common-logs-server（服务端模块）

日志服务端模块，用于日志服务器的日志接收和持久化。

##### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 日志保存 | `LogsSaveManager` | 日志保存管理接口 |

##### 扩展指南

**自定义日志保存方式**：继承 `AbsLogsSaveManager`

```java
@Service
public class MyLogsSaveManager extends AbsLogsSaveManager {
    @Autowired
    private LogRepository logRepository;
    
    @Override
    public void saveLogBatch(List<LogDataEvent> events) {
        List<LogEntity> entities = events.stream()
            .map(this::convertToEntity)
            .collect(Collectors.toList());
        logRepository.saveAll(entities);
    }
}
```

---

### 6. simple-common-cache（缓存模块）

缓存模块，提供本地缓存和分布式缓存的统一封装。

#### 核心功能

| 功能分类 | 工具类 | 说明 |
|---------|-------|------|
| 缓存操作 | `CacheUtils` | 本地缓存 + 分布式缓存统一封装 |

#### 使用示例

**本地缓存**

```java
// 获取本地缓存（带回调）
Object data = CacheUtils.getLocalCache("cacheName", key, () -> loadData());

// 移除本地缓存
CacheUtils.removeLocalCache("cacheName", key);
```

**分布式缓存（带穿透保护）**

```java
// 获取分布式缓存（Redis + DB）
Object data = CacheUtils.get(key, cacheKey,
    () -> redisTemplate.opsForValue().get(key),  // Redis获取
    () -> dbRepository.findById(id)              // DB获取
);
```

---

### 7. simple-common-redis（Redis模块）

Redis模块，提供Redis相关功能，包括分布式锁、缓存注解、限流等。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 分布式锁 | `RedissonLockService` | Redisson分布式锁服务 |
| 缓存注解 | `@RedisCache` | Redis缓存注解 |
| 限流注解 | `@CurrentLimiting` | 接口限流注解 |
| 限流管理 | `CurrentLimitingManager` | 限流处理管理 |
| 缓存切面管理 | `RedisCacheAspectManager` | Redis缓存切面管理 |

#### 使用示例

**分布式锁**

```java
@Autowired
private RedissonLockService lockService;

// 加锁
boolean locked = lockService.tryLock("lockKey", 10, TimeUnit.SECONDS);
if (locked) {
    try {
        // 业务逻辑
    } finally {
        lockService.unlock("lockKey");
    }
}
```

**缓存注解**

```java
@RedisCache(key = "user:#id", expire = 3600)
public User getUserById(String id) {
    return userRepository.findById(id);
}
```

**限流注解**

```java
@CurrentLimiting(key = "api:user", rate = 10, interval = 1)
public R<List<User>> getUsers() {
    return R.ok(userService.findAll());
}
```

---

### 8. simple-common-rabbitmq（RabbitMQ模块）

RabbitMQ模块，提供RabbitMQ消息队列功能。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 消息发送 | `RabbitMqService` | RabbitMQ消息发送服务 |
| 死信处理 | `DeadLetterService` | 死信消息处理服务 |
| ACK管理 | `AckRMQManager` | 消息ACK管理 |
| 重试计数 | `RetryCountManager` | 消息重试计数管理 |
| 消费锁 | `ConsumptionLockManager` | 消息消费锁管理 |
| 消息处理 | `RabbitMqProcess` | 消息处理责任链接口 |

#### 使用示例

**消息发送**

```java
@Autowired
private RabbitMqService rabbitMqService;

// 发送消息
rabbitMqService.sendMsg(new DefaultMessage().setData("消息内容"), 
    "exchangeName", "routingKey");

// 发送延迟消息
rabbitMqService.sendMsg(new DefaultMessage().setData("消息内容"), 
    "exchangeName", "routingKey", 5, TimeUnit.SECONDS);
```

**消息消费**

```java
@Component
public class MessageConsumer {
    @RabbitListener(queues = "queueName")
    @RabbitMqConsumption
    public void consume(String message, Channel channel, 
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 处理消息
            processMessage(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
```

---

### 9. simple-common-annex（附件模块）

附件模块，提供文件上传和下载功能，基于S3协议（支持MinIO、阿里云OSS等）。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 附件服务 | `AnnexService` | 附件上传、下载、删除服务 |
| S3管理 | `S3Manager` | S3协议操作管理 |

#### 使用示例

```java
@Autowired
private AnnexService annexService;

// 上传文件
UploadResponse response = annexService.upload(file, AnnexType.IMAGE);

// 获取文件访问URL
String url = annexService.getUrl(response.getObjectKey());

// 删除文件
annexService.delete(response.getObjectKey());
```

---

### 10. simple-common-doc（文档模块）

文档模块，提供Word文档模板替换功能。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 文档替换 | `DocReplaceService` | Word文档模板替换服务 |
| 模板管理 | `DocTemplateReplaceManager` | 文档模板替换管理 |

#### 使用示例

```java
@Autowired
private DocReplaceService docService;

// 模板替换
Map<String, Object> data = new HashMap<>();
data.put("title", "合同标题");
data.put("partyA", "甲方名称");
data.put("partyB", "乙方名称");

byte[] result = docService.replace("template.docx", data);

// 输出到响应
response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
response.getOutputStream().write(result);
```

---

### 11. simple-common-excel（Excel模块）

Excel模块，提供Excel文件读写功能，支持EasyExcel和POI两种方式。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| EasyExcel写入 | `EasyExcelWriteService` | EasyExcel导出服务 |
| EasyExcel读取 | `EasyExcelReadService` | EasyExcel导入服务 |
| POI写入 | `PoiWriteService` | POI导出服务 |
| POI读取 | `PoiReadService` | POI导入服务 |

#### 使用示例

**EasyExcel导出**

```java
@Autowired
private EasyExcelWriteService excelService;

// 导出Excel
List<User> users = userService.findAll();
excelService.export(response, users, User.class, "用户列表");
```

**EasyExcel导入**

```java
@Autowired
private EasyExcelReadService excelService;

// 导入Excel
List<User> users = excelService.read(file.getInputStream(), User.class);
```

---

### 12. simple-common-sms（短信模块）

短信模块，提供短信发送功能，支持阿里云短信服务。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 短信服务 | `SmsService` | 短信发送服务接口 |
| 发送前校验 | `CheckSmsProcess` | 短信发送前校验责任链接口 |

#### 扩展指南

**自定义短信服务**：继承 `AbsSmsService`

```java
@Service
public class AliSmsService extends AbsSmsService {
    @Override
    protected void doSend(String phone, String code) {
        // 阿里云短信发送逻辑
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(phone);
        request.setSignName("签名");
        request.setTemplateCode("模板ID");
        request.setTemplateParam("{\"code\":\"" + code + "\"}");
        client.sendSms(request);
    }
}
```

---

### 13. simple-common-xxljob（任务调度模块）

任务调度模块，提供XXL-Job任务调度集成功能。

#### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 任务服务 | `XxlJobService` | XXL-Job任务管理服务 |
| 任务管理 | `XxlJobManager` | XXL-Job任务管理 |

#### 使用示例

```java
@Autowired
private XxlJobService xxlJobService;

// 创建任务
XxlJobTaskRequest request = new XxlJobTaskRequest();
request.setJobDesc("定时任务描述");
request.setExecutorHandler("taskHandler");
request.setJobType(1);
request.setScheduleConf("0 0 1 * * ?");
xxlJobService.createTask(request);
```

---

### 14. simple-common-mp（MyBatis-Plus模块）

MyBatis-Plus模块，提供MyBatis-Plus配置和扩展。

#### 核心功能

| 功能分类 | 组件 | 说明 |
|---------|-----|------|
| 分页配置 | `MybatisPlusConfig` | MyBatis-Plus分页配置 |
| ID生成器 | `CustomIdGenerator` | 自定义ID生成器 |
| 操作处理器 | `MybatisPlusOperationHandler` | 自动填充处理器 |

---

### 15. simple-common-ai（AI模块）

AI模块，提供AI服务集成功能。

#### 核心功能

| 功能分类 | 组件 | 说明 |
|---------|-----|------|
| AI构建器 | `AiAliBuilder` | 阿里云AI服务构建器 |

---

## 核心扩展机制说明

### 责任链模式

框架在多种场景使用责任链模式，如认证拦截、登录错误处理、短信发送前校验等。

#### 实现步骤

1. 定义 `Process` 接口（继承 `BasProcessService`）
2. 创建枚举类实现 `DefaultKindProcess` 接口
3. 实现多个 `Process` 实现类
4. 通过枚举的 `order` 字段控制执行顺序

#### 示例

```java
// 1. Process接口
public interface AuthProcess extends BasProcessService {
    void process(String token, String path, String ipAddr);
}

// 2. 枚举定义
public enum AuthInterceptorKindProcess implements DefaultKindProcess {
    CHECK_TOKEN(1, "token校验", CheckTokenAuthProcess.class),
    CHECK_ROLE(2, "角色校验", CheckRoleAuthProcess.class),
    CHECK_SCOPE(3, "授权范围校验", CheckScopeAuthProcess.class);
    
    private final int order;
    private final String msg;
    private final Class<?> processClass;
}

// 3. 实现类
@Component
public class CheckTokenAuthProcess implements AuthProcess {
    @Override
    public void process(String token, String path, String ipAddr) {
        // token校验逻辑
    }
}
```

### 事件驱动模式

框架使用事件总线实现模块间解耦。

#### 事件定义规范

```java
@Data
@Event
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class UserCreatedEvent {
    private String userId;
    private String username;
}
```

#### 事件处理器

```java
@Component
public class UserEventHandler {
    @EventHandler
    public void handleUserCreated(UserCreatedEvent event) {
        // 处理逻辑
    }
}
```

### 继承扩展模式

框架提供多个抽象基类，业务系统可通过继承实现自定义逻辑。

#### 关键抽象类

| 抽象类 | 用途 | 需要实现的方法 |
|-------|------|--------------|
| `AbsLoginManager` | 自定义登录方式 | `doLogin()` |
| `AbsClientDetailsService` | 自定义客户端信息获取 | `getClientDetails()` |
| `AbsLogsSaveManager` | 自定义日志保存 | `saveLogBatch()` |
| `AbsSmsService` | 自定义短信发送 | `doSend()` |
| `AbsCycleService` | 自定义循环任务 | `execute()`、`complete()` |
| `AbsClientAuthConfig` | 自定义认证配置 | `configure()` |

---

## 配置属性说明

框架各模块通过 `@ConfigurationProperties` 注解统一管理配置。

### 核心配置

```yaml
simple:
  # 应用配置
  application:
    name: my-application
    
  # 线程池配置
  thread:
    core-size: 10
    max-size: 100
    queue-capacity: 1000
    
  # 锁配置
  lock:
    enabled: true
    wait-time: 10
    lease-time: 30
```

### 认证配置

```yaml
simple:
  auth:
    enabled: true
    token-header: Authorization
    white-urls:
      - /api/public/**
      - /api/login
```

### Redis配置

```yaml
simple:
  cache:
    redis:
      bag: true
      default-bag: default-cache
      
  redis:
    host: localhost
    port: 6379
    password: 
    database: 0
```

### WebSocket配置

```yaml
simple:
  websocket:
    port: 8081
    path: /ws
    clean-task:
      enabled: true
      initial-delay: 60
      period: 60
```

---

## 快速开始

### 1. 引入依赖

```xml
<!-- 核心模块 -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-core</artifactId>
    <version>${simple-common.version}</version>
</dependency>

<!-- 认证模块（客户端） -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-auth-client</artifactId>
    <version>${simple-common.version}</version>
</dependency>

<!-- Redis模块 -->
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-redis</artifactId>
    <version>${simple-common.version}</version>
</dependency>
```

### 2. 配置application.yml

```yaml
simple:
  auth:
    enabled: true
    token-header: Authorization
  redis:
    host: localhost
    port: 6379
```

### 3. 创建配置类

```java
@Configuration
public class MyAuthConfig extends AbsClientAuthConfig {
    @Override
    protected void configure(ClientAuthInfo clientAuthInfo) {
        clientAuthInfo.getWhiteUrls().add("/api/public/**");
    }
}
```

### 4. 启动应用

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

---

## 最佳实践

1. **优先使用框架封装**：线程池、缓存、锁、事件等优先使用框架提供的工具类
2. **遵循命名规范**：Manager/Service/Process 严格区分使用场景
3. **责任链模式**：多种机制联合处理时必须使用责任链
4. **事件驱动**：模块间解耦优先使用事件总线
5. **异常处理**：统一使用 `AssertUtils` 和 `DefaultException`
6. **响应封装**：统一使用 `R` 类
7. **配置管理**：使用 `@ConfigurationProperties` 统一管理配置
8. **初始化顺序**：通过 `@Order` 注解控制初始化顺序

---

## 版本说明

当前版本：1.0.0

支持Spring Boot版本：2.7.x - 3.x

支持JDK版本：17+

---

## 许可证

本项目采用 Apache License 2.0 许可证。

---

## 联系方式

如有问题或建议，请提交Issue或Pull Request。
# simple-common 框架认知文档

> 作者: qty | 快速索引+按行号锁定，无需通读全文

---

## 快速查找索引

| # | 模块 | 行号 | 一句话定位 |
|---|------|------|-----------|
| 1 | **core** 核心基础设施 | L30-L126 | 统一响应 `R<T>`、异常体系、30+工具类、线程池/分布式锁/循环任务接口 |
| 2 | **mp** MyBatis Plus封装 | L128-L180 | `PageBase` 分页基类、雪花ID、自动填充 `createTime/updateTime`、`@DataScopeTable` 数据权限 |
| 3 | **redis** 缓存/限流/分布式锁 | L182-L258 | `@RedisCache` 声明式缓存、`@CurrentLimiting` 限流、`RedissonLockService` 分布式锁+闭锁+信号量 |
| 4 | **cache** 多级缓存 | L260-L310 | Caffeine本地缓存+Redis分布式缓存两级方案，防击穿 |
| 5 | **rabbitmq** 消息队列 | L312-L358 | `RabbitMqService` 即时/延迟消息、`@RabbitMqConsumption` 防重复消费 |
| 6 | **eventbus** 事件总线 | L360-L420 | `@Event`+`@EventHandler` 注解驱动、同步/MQ异步/延迟三种模式 |
| 7 | **auth** 认证授权体系 | L422-L506 | `@HasAuthority`权限、`@Sign`签名、`@CsrfDefense`防重复、`AuthHandlerInterceptor`认证拦截器 |
| 8 | **logs** 日志系统 | L508-L542 | TCP+Protobuf高性能日志收集，Client/Server架构 |
| 9 | **websocket** 通信 | L544-L582 | Netty+`@WebSocketListening`注解分发、`ChannelMap`多级通道 |
| 10 | **excel** 导入导出 | L584-L626 | EasyExcel写入/读取 + POI复杂导出（自定义列宽分页） |
| 11 | **sms** 短信服务 | L628-L674 | 验证码发送校验、模板短信、IP/手机号防刷 |
| 12 | **annex** 附件管理 | L676-L728 | S3协议上传/下载/删除/生成URL，支持MinIO/OSS/AWS |
| 13 | **doc** 文档模板 | L730-L774 | Poi-Tl Word模板填充：文本/图片/表格/列表 |
| 14 | **xxljob** 任务调度 | L776-L818 | XXL-JOB任务CRUD HTTP封装 |
| 15 | **ai** 大模型集成 | L820-L860 | 阿里云百炼对话封装 |
| 16 | **alibaba** 阿里组件 | L862-L882 | Sentinel用户级两级限流+Feign配置 |
| 17 | POM依赖关系 | L884-L914 | 全模块依赖树 |

---

## 1. core 核心模块 ← L31-L126

**Maven**: `simple-common-core`  
**位置**: [`com.simple.common.core`](simple-common-core/src/main/java)

### 统一响应 + 异常体系

| 类 | 说明 |
|----|------|
| [`R<T>`](simple-common-core/src/main/java/com/simple/common/core/response/R.java) | `R.ok()` / `R.ok(data)` / `R.error()` / `R.error(code,msg)` |
| [`AssertUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/AssertUtils.java) | `notEmpty(obj,"msg")` / `isTrue(expr,"msg")` / `error("msg")` |
| [`DefaultException`](simple-common-core/src/main/java/com/simple/common/core/exception/DefaultException.java) | `new DefaultException(code, msg)` |
| [`DefaultExceptionHandler`](simple-common-core/src/main/java/com/simple/common/core/exception/DefaultExceptionHandler.java) | 全局 `@ControllerAdvice` 统一拦截 |
| [`DefaultExceptionEnum`](simple-common-core/src/main/java/com/simple/common/core/exception/DefaultExceptionEnum.java) | `OK("200")` / `ERROR("500")` |
| [`AbstractException`](simple-common-core/src/main/java/com/simple/common/core/exception/AbstractException.java) | 自定义异常枚举接口 |

### 工具类速查

| 工具类 | 关键方法 |
|--------|---------|
| [`Base64Utils`](simple-common-core/src/main/java/com/simple/common/core/utils/Base64Utils.java) | `encode(text)` `decodeStr(text)` |
| [`JsonUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/JsonUtils.java) | `toJsonStr(obj)` `toJsonObj(json,Class)` `toList(json,Class)` |
| [`IdUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/IdUtils.java) | `getSnowflakeNextIdStr()` `getFastSimpleUUID()` `randomNumbers()` |
| [`IPUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/IPUtils.java) | `getIpAddr()` `getIntranetIp()` |
| [`ThreadUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/ThreadUtils.java) | `supplyAsync()` `runAsync()` `schedule()` `scheduleWithFixedDelay()` |
| [`SignUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/SignUtils.java) | `generateSignStr(obj,exclude)` `signWeb(msg,key)` `verifyWeb(msg,sig,key)` |
| [`BeanUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/BeanUtils.java) | `toMap(obj)` `fillBeanWithMap(map,Class)` |
| [`FileUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/FileUtils.java) | `write(inputStream,fileName)` `getResourcesFileInputStream(path)` |
| [`CryptoUtil`](simple-common-core/src/main/java/com/simple/common/core/utils/CryptoUtil.java) | 对称/非对称/国密加解密 |
| [`CollectionUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/CollectionUtils.java) | `matches(specify,collection)` |
| [`HttpServletUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/HttpServletUtils.java) | `getRequest()` `getResponse()` |
| [`ResponseUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/ResponseUtils.java) | 直接写JSON到response |
| [`UrlRulesUtils`](simple-common-core/src/main/java/com/simple/common/core/utils/UrlRulesUtils.java) | URL路由匹配 |

### 核心服务接口

| 接口 | 默认实现 | 用途 |
|------|---------|------|
| [`ThreadService`](simple-common-core/src/main/java/com/simple/common/core/common/service/thread/ThreadService.java) | `DefaultThreadService` | 线程池管理，`getExecutor()`调度池/`getAsyncExecutor()`异步池 |
| [`LockService`](simple-common-core/src/main/java/com/simple/common/core/common/service/lock/LockService.java) | `RedissonLockService` | 分布式锁：`lock(key,r)` / `lockHaveValue(key,f)` / `fairLock(key,r)` |
| [`CycleService<T>`](simple-common-core/src/main/java/com/simple/common/core/common/service/cycle/CycleService.java) | `AbsCycleService<T>` | 循环任务：`runUniform`均匀间隔 / `runAccumulate`累加间隔 |
| [`BasProcessService`](simple-common-core/src/main/java/com/simple/common/core/common/service/process/BasProcessService.java) | 各模块实现 | 责任链模式基接口 |

### 对接
```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-core</artifactId></dependency>
```

---

## 2. mp MyBatis Plus封装 ← L128-L180

**核心组件**:

| 类 | 说明 |
|----|------|
| [`PageBase`](simple-common-mp/src/main/java/com/simple/common/mp/page/PageBase.java) | 分页基类，业务DTO继承后调用 `getPage()` 获得MP分页对象 |
| [`CustomIdGenerator`](simple-common-mp/src/main/java/com/simple/common/mp/generator/CustomIdGenerator.java) | 雪花ID生成器（自动生效） |
| [`MybatisPlusOperationHandler`](simple-common-mp/src/main/java/com/simple/common/mp/handler/MybatisPlusOperationHandler.java) | 自动填充 `createTime` / `updateTime` |
| [`DataScopeTable`](simple-common-mp/src/main/java/com/simple/common/mp/common/annotation/DataScopeTable.java) | 实体类上声明需要数据权限的注解 |
| [`DataScopeInnerInterceptor`](simple-common-mp/src/main/java/com/simple/common/mp/common/interceptor/DataScopeInnerInterceptor.java) | 数据权限SQL注入拦截器 |
| [`DataScopeSqlHandler`](simple-common-mp/src/main/java/com/simple/common/mp/common/handler/DataScopeSqlHandler.java) | 数据权限条件构建接口（需自定义实现） |

**数据权限接入**: 实体加 `@DataScopeTable` → 实现 `DataScopeSqlHandler` 返回WHERE条件 → 拦截器自动注入

**分页标准流程**: DTO extends `PageBase` → Repository 接收 `Page<?>` + `@Param("req")` → Service 调用 `dao.selectPage(req.getPage(), req)`

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-mp</artifactId></dependency>
```

---

## 3. Redis 缓存/限流/分布式锁 ← L182-L258

### 注解

| 注解 | 用法 |
|------|------|
| [`@RedisCache`](simple-common-redis/src/main/java/com/simple/common/redis/annotation/RedisCache.java) | 方法级自动缓存：`@RedisCache(cacheTime=300, appendRandomDuration=60)` |
| [`@CurrentLimiting`](simple-common-redis/src/main/java/com/simple/common/redis/annotation/CurrentLimiting.java) | 接口限流：`@CurrentLimiting(key=USER_ID, time=10, sum=5)` |

### 锁服务

| 类 | 关键方法 |
|----|---------|
| [`RedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/common/service/RedissonLockService.java) | `countDownLatch(key,sum)` / `decreaseCountDownLatch(key)` / `semaphoreLock(key,sum)` / `decreaseSemaphoreLock(key,num)` |
| [`DefaultRedissonLockService`](simple-common-redis/src/main/java/com/simple/common/redis/service/DefaultRedissonLockService.java) | 默认实现 |

也可以通过 [`LockService`](simple-common-core/src/main/java/com/simple/common/core/common/service/lock/LockService.java) 统一调用：`lock("order:"+id, ()->{...})`

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-redis</artifactId></dependency>
```

---

## 4. Cache 多级缓存 ← L260-L310

| 类 | 说明 |
|----|------|
| [`LocalCacheFactory`](simple-common-cache/src/main/java/com/simple/common/cache/common/factory/LocalCacheFactory.java) | Caffeine工厂单例，`createCache(name, spec->{...})` / `createLoadingCache(name, loader, spec->{...})` |
| [`CacheUtils`](simple-common-cache/src/main/java/com/simple/common/cache/Utils/CacheUtils.java) | 工具门面 |

**分布式缓存**: `CacheUtils.get(request, lockKey, redisFunc, dbFunc)` → 先Redis→未命中→分布式锁→双重检查→查DB→写回Redis

**本地缓存**: `CacheUtils.createLocalCache("users", spec -> spec.maximumSize(10000).expireAfterWrite(300))`

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-cache</artifactId></dependency>
```

---

## 5. RabbitMQ 消息队列 ← L312-L358

| 接口/注解 | 说明 |
|-----------|------|
| [`RabbitMqService`](simple-common-rabbitmq/src/main/java/com/simple/common/rabbitmq/common/service/RabbitMqService.java) | `sendMsg(msg, exchange, routingKey)` / `sendMsg(msg, exchange, routingKey, time, unit)` 延迟消息 |
| [`@RabbitMqConsumption`](simple-common-rabbitmq/src/main/java/com/simple/common/rabbitmq/annotation/RabbitMqConsumption.java) | 消费端：`businessTime`/`effectiveTime`/`retryCount` |
| [`DefaultMessage`](simple-common-rabbitmq/src/main/java/com/simple/common/rabbitmq/common/entity/DefaultMessage.java) | 统一消息实体 |

**延迟消息前提**: 安装 `rabbitmq-delayed-message-exchange` 插件，交换机类型为 `x-delayed-message`

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-rabbitmq</artifactId></dependency>
```

---

## 6. EventBus 事件总线 ← L360-L420

| 注解/接口 | 说明 |
|-----------|------|
| [`@Event`](simple-common-eventbus/src/main/java/com/simple/common/eventbus/common/annotation/Event.java) | 标记事件类 |
| [`@EventHandler`](simple-common-eventbus/src/main/java/com/simple/common/eventbus/common/annotation/EventHandler.java) | 标记处理方法 |
| [`EventBusService`](simple-common-eventbus/src/main/java/com/simple/common/eventbus/common/service/EventBusService.java) | `push(event)`同步 / `push(event, time, unit)` 延迟 / `push(obj, eventClass)`指定类型 |
| [`SyncEventBusService`](simple-common-eventbus/src/main/java/com/simple/common/eventbus/service/SyncEventBusService.java) | 同步实现 |
| [`MqEventBusService`](simple-common-eventbus/src/main/java/com/simple/common/eventbus/service/MqEventBusService.java) | 异步(MQ)实现 |

**三步使用**: ①定义类加 `@Event` ②处理器加 `@EventHandler` ③调用 `eventBusService.push(event)`

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-eventbus</artifactId></dependency>
```

---

## 7. Auth 认证授权体系 ← L422-L506

### 客户端 (auth-client) 注解

| 注解 | 说明 |
|------|------|
| [`@HasAuthority({"sys:user:page"})`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/HasAuthority.java) | 方法级权限校验，超级管理员自动放行 |
| [`@Sign`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/Sign.java) | 接口签名：`excludeFields` / `checkTimestamp` / `checkNonce` |
| [`@CsrfDefense`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/CsrfDefense.java) | CSRF+防重复提交，`consume=true`一次性token |

### 客户端核心接口

| 类 | 说明 |
|----|------|
| [`AuthHandlerInterceptor`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/interceptor/AuthHandlerInterceptor.java) | 认证拦截器：白名单→Token校验→权限校验 |
| [`LoginUserUtils`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/util/LoginUserUtils.java) | `getUserTemporary()` 获取当前用户信息 |
| [`DataScopeEnum`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/enums/DataScopeEnum.java) | 数据权限：ALL(1) > DEPT_AND_CHILD(2) > DEPT(3) > SELF(4) |
| [`LoginInfoManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/user/LoginInfoManager.java) | 登录信息管理 |
| [`TokenManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/token/TokenManager.java) | Token管理 |
| [`CacheManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/cache/CacheManager.java) | 缓存管理（本地/Redis） |

### 服务端 (auth-server) 核心接口

| 接口/类 | 说明 |
|---------|------|
| [`LoginManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/login/LoginManager.java) | 登录管理（需实现） |
| [`AbsLoginManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/login/AbsLoginManager.java) | 登录抽象基类 |
| [`DefaultLoginService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/service/login/DefaultLoginService.java) | 登录服务实现 |
| [`ClientManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/client/ClientManager.java) | 客户端管理 |
| [`PermissionManageService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/permission/PermissionManageService.java) | 权限管理 |
| [`UnifiedSecretManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/secret/UnifiedSecretManager.java) | 统一秘钥管理 |

### 权限值命名规则
`@RequestMapping类路径:方法路径`，`/`→`:`，如 `sys:user:page`

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-auth-client</artifactId></dependency>
```

---

## 8. Logs 日志系统 ← L508-L542

TCP+Protobuf 高性能日志收集。三子模块：

| 子模块 | 核心类 |
|--------|--------|
| `logs-proto` 协议 | [`LogDataEvent`](simple-common-logs/simple-common-logs-proto/src/main/java/com/simple/common/logs/proto/common/event/LogDataEvent.java) |
| `logs-client` 客户端 | [`LogService`](simple-common-logs/simple-common-logs-client/src/main/java/com/simple/common/logs/client/common/service/LogService.java) / [`BufferedLogManager`](simple-common-logs/simple-common-logs-client/src/main/java/com/simple/common/logs/client/manager/BufferedLogManager.java) / [`LogProtobufTcpClient`](simple-common-logs/simple-common-logs-client/src/main/java/com/simple/common/logs/client/common/tcp/LogProtobufTcpClient.java) |
| `logs-server` 服务端 | [`LogProtobufTcpServer`](simple-common-logs/simple-common-logs-server/src/main/java/com/simple/common/logs/server/common/tcp/LogProtobufTcpServer.java) / [`AbsLogsSaveManager`](simple-common-logs/simple-common-logs-server/src/main/java/com/simple/common/logs/server/manager/AbsLogsSaveManager.java) |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-logs-client</artifactId></dependency>
```

---

## 9. WebSocket 通信 ← L544-L582

| 注解/类 | 说明 |
|---------|------|
| [`@WebSocketListening`](simple-common-websocket/src/main/java/com/simple/common/websocket/common/annotation/WebSocketListening.java) | `type="chat" cliKey="default"` 标记消息处理方法 |
| [`ChannelMap<K,V,T>`](simple-common-websocket/src/main/java/com/simple/common/websocket/common/channel/ChannelMap.java) | 多级通道存储：`add(key,key2,value)` / `get(key)` / `del(key,key2)` |
| [`WebSocketServerHandler`](simple-common-websocket/src/main/java/com/simple/common/websocket/handler/WebSocketServerHandler.java) | Netty处理器 |
| [`WebSocketAuthHandler`](simple-common-websocket/src/main/java/com/simple/common/websocket/handler/WebSocketAuthHandler.java) | 认证处理器 |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-websocket</artifactId></dependency>
```

---

## 10. Excel 导入导出 ← L584-L626

| 接口 | 说明 | 关键方法 |
|------|------|---------|
| [`EasyExcelWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/EasyExcelWriteService.java) | EasyExcel写入 | `writeResponse(clazz,list,"文件名")` / `writeOutputStream(clazz,list)` |
| [`EasyExcelReadService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/EasyExcelReadService.java) | EasyExcel读取 | `read(file/inputStream/path, headRowNum, headClass, listener)` |
| [`PoiWriteService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/PoiWriteService.java) | POI写入（复杂样式） | `exportResponse(function,list,head,width,num,"文件名")` |
| [`PoiReadService`](simple-common-excel/src/main/java/com/simple/common/excel/common/service/PoiReadService.java) | POI读取 | `read(...)` |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-excel</artifactId></dependency>
```

---

## 11. SMS 短信服务 ← L628-L674

| 接口/类 | 方法 |
|---------|------|
| [`SmsService`](simple-common-sms/src/main/java/com/simple/common/sms/common/service/SmsService.java) | `sendCode(mobile,code,sendType)` / `sendTemplateParam(mobile,sendType,param)` / `checkSms(mobile,code,sendType)` |
| [`AliSmsService`](simple-common-sms/src/main/java/com/simple/common/sms/service/AliSmsService.java) | 阿里云实现 |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-sms</artifactId></dependency>
```

---

## 12. Annex 附件管理 ← L676-L728

| 接口/类 | 方法 |
|---------|------|
| [`AnnexService`](simple-common-annex/src/main/java/com/simple/common/annex/common/service/AnnexService.java) | `upload(file,appName,shareType)` / `upload(file,appName,pkg,shareType)` / `writeGetObjectResponse(objectUrl)` / `generateUrl(objectUrl)` / `delete(objectUrl)` |
| [`S3Manager`](simple-common-annex/src/main/java/com/simple/common/annex/common/manager/S3Manager.java) | S3底层操作 |
| [`UploadResponse`](simple-common-annex/src/main/java/com/simple/common/annex/common/dto/UploadResponse.java) | 上传响应 |
| [`ShareType`](simple-common-annex/src/main/java/com/simple/common/annex/common/enums/ShareType.java) | `PUBLIC` / `PRIVATE` |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-annex</artifactId></dependency>
```

---

## 13. Doc 文档模板替换 ← L730-L774

| 类 | 说明 |
|----|------|
| [`Docs`](simple-common-doc/src/main/java/com/simple/common/doc/common/builder/Docs.java) | 模板数据构建器，链式调用 |
| [`DocReplaceService`](simple-common-doc/src/main/java/com/simple/common/doc/common/service/DocReplaceService.java) | `replace(templateInputStream, data, outputStream)` |

Docs用法: `Docs.builder().addStr("name","张三").addImgLocal("seal","seal.png",100,100).addTable(...).create()`

模板语法: `{{text}}`文本 / `{{@picture}}`图片 / `{{#table}}`表格 / `{{*list}}`列表

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-doc</artifactId></dependency>
```

---

## 14. XxlJob 分布式任务调度 ← L776-L818

| 类 | 方法 |
|----|------|
| [`XxlJobManager`](simple-common-xxljob/src/main/java/com/simple/common/xxljob/common/manager/XxlJobManager.java) | `create(req)` / `update(req)` / `delete(id)` / `start(id)` / `end(id)` / `trigger(id)` |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-xxljob</artifactId></dependency>
```

---

## 15. AI 大模型集成 ← L820-L860

| 类 | 说明 |
|----|------|
| [`AiAliBuilder<T>`](simple-common-ai/src/main/java/com/simple/common/ai/common/builder/AiAliBuilder.java) | `AiAliBuilder.builder().sendMsg(sessionId, msg)` 返回 `AiResult`，支持 `toObj(Class)` 结构化解析 |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-ai</artifactId></dependency>
```

---

## 16. Alibaba 阿里组件 ← L862-L882

| 类 | 说明 |
|----|------|
| [`UserRateLimitAspect`](simple-common-alibaba/src/main/java/com/come/on/alibaba/aspect/UserRateLimitAspect.java) | 自动拦截所有Controller，按userId做Sentinel两级限流（接口级 `Controller:method` + 全局 `user:rate:limit`） |
| [`FeignConfig`](simple-common-alibaba/src/main/java/com/come/on/alibaba/FeignConfig.java) | Feign配置 |

```xml
<dependency><groupId>com.simple</groupId><artifactId>simple-common-alibaba</artifactId></dependency>
```

---

## POM 依赖关系 ← L884-L914

```
simple-common (parent)
├── core          ← 所有模块的基础
├── mp            ← 依赖core
├── redis         ← 依赖core
├── cache         ← 依赖redis+core
├── rabbitmq      ← 依赖core
├── eventbus      ← 依赖rabbitmq+core
├── auth-client   ← 依赖core
├── auth-server   ← 依赖core
├── logs-*        ← client/server/proto
├── websocket     ← 依赖auth-client+core
├── excel         ← 依赖core
├── sms           ← 依赖core
├── annex         ← 依赖core
├── doc           ← 依赖core
├── xxljob        ← 依赖core
├── ai            ← 依赖core
├── alibaba       ← 依赖auth-client+core
└── test          ← 测试模块
```

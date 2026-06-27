---
name: "simple-common-logs"
description: "TCP+Protobuf 高性能日志收集模块。包含 logs-client（LogService/BufferedLogManager 缓冲队列批量发送/失败降级本地文件/优雅停机）和 logs-server（LogProtobufTcpServer/AbsLogsSaveManager 接收存储）；需实现 LogUserManager 提供用户信息。当需要操作日志收集时使用。"
---

# simple-common-logs 认知文档

**Maven**: `simple-common-logs-client` / `simple-common-logs-server`
**架构**: TCP + Protobuf 高性能日志收集
**包路径**: `com.simple.common.logs`

## 架构概览

| 子模块 | 核心类 | 说明 |
|--------|--------|------|
| `logs-proto` | `LogDataEvent` | Protobuf协议定义，日志数据结构 |
| `logs-client` | `LogService` / `BufferedLogManager` / `LogProtobufTcpClient` | 日志发送客户端，自动拦截Controller请求 |
| `logs-server` | `LogProtobufTcpServer` / `AbsLogsSaveManager` | 日志接收+存储服务端 |

## LogService — 日志服务接口

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

## LogManager — 日志发送管理器

```java
@Autowired
private LogManager logManager;

LogDataEvent logData = LogDataEvent.newBuilder()
    .setUserId("123")
    .setUserName("张三")
    .setOperation("创建订单")
    .setModule("订单管理")
    .setIp("192.168.1.100")
    .setCreateTime(System.currentTimeMillis())
    .build();

boolean success = logManager.send(logData);
```

```java
boolean send(LogDataEvent logData);
void start();
void stop();
```

## BufferedLogManager — 缓冲日志管理器（默认实现）

继承 `LogManager`，提供增强特性：

| 特性 | 说明 |
|------|------|
| 内存缓冲队列 | 后台线程批量发送，减少网络和编解码开销 |
| 失败降级 | 发送失败时自动写入本地文件，连接恢复后补发 |
| 优雅停机 | 应用关闭时刷新缓冲区，确保数据不丢 |
| 魔数校验 | 降级文件采用魔数（0xCAFEBABE）校验，损坏自动恢复 |

## LogUserManager — 日志用户信息管理器（需自行实现）

```java
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

```java
String loginNickName();   // 返回当前操作用户昵称
String loginUserId();     // 返回当前操作用户ID
```

## LogProtobufTcpClient — TCP 日志客户端

```java
logProtobufTcpClient.connect("192.168.1.100", 9090);
logProtobufTcpClient.send(logData);
logProtobufTcpClient.addSendFailureListener(new SendFailureListener() {
    @Override
    public void onSendFailure(LogDataEvent logData) {
        fallbackWriter.write(logData);
    }
});
```

## LogDataEvent — Protobuf 日志事件

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

## 服务端 — AbsLogsSaveManager（需自行实现）

```java
@Component
public class MyLogsSaveManager extends AbsLogsSaveManager {
    @Override
    protected void save(List<LogDataEvent> logDataList) {
        logRepository.saveBatch(logDataList);
    }
}
```

## 核心流程图形化

### TCP + Protobuf 日志收集架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                    TCP + Protobuf 日志收集架构                         │
│                                                                      │
│  ┌─────────────────┐          ┌─────────────────┐                   │
│  │  业务服务 (N个)  │          │  日志收集服务     │                   │
│  │                  │          │                  │                   │
│  │ LogFilter        │          │ LogProtobufTcp   │                   │
│  │   ↓拦截请求      │          │ Server            │                   │
│  │ LogInterceptor   │   TCP    │   ↓接收          │                   │
│  │   ↓收集上下文    │◄────────►│ AbsLogsSaveManager│                   │
│  │ LogService       │ Protobuf │   ↓保存到DB      │                   │
│  │   ↓发送          │          │                  │                   │
│  │                  │          │                  │                   │
│  │ BufferedLogManager│         │                  │                   │
│  │ ┌──────────────┐ │          └─────────────────┘                   │
│  │ │内存缓冲队列   │ │                                               │
│  │ │批量发送       │ │                                               │
│  │ │失败降级→本地  │ │                                               │
│  │ │魔数校验恢复   │ │                                               │
│  │ │优雅停机       │ │                                               │
│  │ └──────────────┘ │                                               │
│  └─────────────────┘                                               │
│                                                                      │
│  LogDataEvent (Protobuf):                                            │
│  ┌──────────────────────────────────────────────────────┐           │
│  │ userId | userName | operation | module | ip | url    │           │
│  │ method | params | result | costTime | createTime     │           │
│  └──────────────────────────────────────────────────────┘           │
│                                                                      │
│  LogUserManager (需实现):                                            │
│  ┌──────────────────────────────────────────────────────┐           │
│  │ loginNickName() → 从 LoginUserUtils 获取当前用户昵称  │           │
│  │ loginUserId()   → 从 LoginUserUtils 获取当前用户ID    │           │
│  └──────────────────────────────────────────────────────┘           │
└──────────────────────────────────────────────────────────────────────┘
```

### BufferedLogManager 缓冲机制

```
┌──────────────────────────────────────────────────────────────────────┐
│  LogService.send()                                                    │
│       │                                                               │
│       ▼                                                               │
│  ┌─────────────┐     ┌──────────────┐     ┌────────────────────┐    │
│  │ 内存缓冲队列 │────►│ 后台批量发送 │────►│ LogProtobufTcpClient│    │
│  │ (异步入队)  │     │ (定时/定量)  │     │ → TCP 发送         │    │
│  └─────────────┘     └──────┬───────┘     └────────┬───────────┘    │
│                             │  发送失败              │                │
│                             ▼                       ▼                │
│                    ┌────────────────┐    ┌──────────────────┐       │
│                    │ 写入本地降级文件│    │ 连接恢复后补发    │       │
│                    │ (魔数校验)     │───►│ 保证数据不丢      │       │
│                    └────────────────┘    └──────────────────┘       │
│                                                                      │
│  优雅停机：应用关闭时 flush 缓冲区 → 等待发送完成 → 关闭连接         │
└──────────────────────────────────────────────────────────────────────┘
```

## POM依赖

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
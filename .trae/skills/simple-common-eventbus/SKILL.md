---
name: "simple-common-eventbus"
description: "事件驱动架构模块。提供 @Event 定义事件类、@EventHandler 定义事件处理器、EventBusService 发布事件（同步/SyncEventBusService、异步MQ/MqEventBusService、延迟发布）。当需要服务内解耦或跨服务事件通信时使用。"
---

# simple-common-eventbus 认知文档

**Maven**: `simple-common-eventbus`
**包路径**: `com.simple.common.eventbus`

## 三步使用流程

### 步骤一：定义事件类

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

### 步骤二：定义处理器

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

### 步骤三：发布事件

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

## EventBusService 完整 API

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

## 核心流程图形化

### 事件驱动架构

```
┌──────────────────────────────────────────────────────────────────────┐
│                      事件驱动架构流程                                   │
│                                                                      │
│  ┌──────────────────┐                                               │
│  │ ① 定义事件类      │  @Event("userCreated")                       │
│  │   UserCreatedEvent│  public class UserCreatedEvent { ... }        │
│  └────────┬─────────┘                                               │
│           │                                                          │
│  ┌────────▼─────────┐                                               │
│  │ ② 定义处理器      │  @EventHandler("userCreated")                 │
│  │   handleUserCreated│ public void handle(UserCreatedEvent e) { }   │
│  └────────┬─────────┘                                               │
│           │                                                          │
│  ┌────────▼─────────┐                                               │
│  │ ③ 发布事件        │  eventBusService.push(event)                 │
│  └────────┬─────────┘                                               │
│           │                                                          │
│     ┌─────┴─────┐                                                   │
│     ▼           ▼                                                   │
│  ┌────────┐ ┌────────┐                                              │
│  │ Sync   │ │  MQ    │  两种实现自动切换                             │
│  │EventBus│ │EventBus│                                              │
│  │Service │ │Service │                                              │
│  │(默认)  │ │(需MQ)  │                                              │
│  └───┬────┘ └───┬────┘                                              │
│      │          │                                                    │
│      ▼          ▼                                                    │
│  同一线程     RabbitMQ                                               │
│  直接调用     异步投递                                                │
│      │          │                                                    │
│      ▼          ▼                                                    │
│  ┌──────────────────────────────┐                                   │
│  │ DefaultEventHandlerManager   │                                   │
│  │ 扫描所有 @EventHandler 方法   │                                   │
│  │ 按事件名 → MethodHandle 映射  │                                   │
│  │ 支持同步/延迟两种模式         │                                   │
│  └──────────────────────────────┘                                   │
└──────────────────────────────────────────────────────────────────────┘
```

### 三种发布模式对比

```
┌────────────────┬──────────────────┬─────────────────────────────────┐
│ 模式           │ 实现类            │ 适用场景                         │
├────────────────┼──────────────────┼─────────────────────────────────┤
│ 同步           │ SyncEventBus     │ 同一进程内解耦，如发邮件/短信    │
│                │ Service          │ 事件和处理在同一线程             │
├────────────────┼──────────────────┼─────────────────────────────────┤
│ 异步(MQ)       │ MqEventBus       │ 跨服务通信，如订单服务通知       │
│                │ Service          │ 库存服务；需引入RabbitMQ         │
├────────────────┼──────────────────┼─────────────────────────────────┤
│ 延迟           │ 两种都支持        │ 定时任务替代方案，如：           │
│                │ push(event,time) │ 30分钟后检查订单支付状态         │
└────────────────┴──────────────────┴─────────────────────────────────┘
```

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-eventbus</artifactId>
</dependency>
```
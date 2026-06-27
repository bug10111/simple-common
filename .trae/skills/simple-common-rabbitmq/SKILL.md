---
name: "simple-common-rabbitmq"
description: "RabbitMQ 消息队列模块。提供 RabbitMqService 发送即时/延迟消息（DefaultMessage 含 msgData/bizType/雪花ID），@RabbitMqConsumption 防重复消费注解（支持业务时长/有效时长/重试次数配置）。当需要MQ消息发送和消费时使用。"
---

# simple-common-rabbitmq 认知文档

**Maven**: `simple-common-rabbitmq`
**包路径**: `com.simple.common.rabbitmq`

## RabbitMqService — 消息发送

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

## @RabbitMqConsumption — 防重复消费

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

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-rabbitmq</artifactId>
</dependency>
```
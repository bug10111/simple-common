## 6. simple-common-rabbitmq

### 模块介绍

RabbitMQ消息队列模块，提供防重消费、自动重试、锁续期、责任链处理等企业级特性。

### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 消费注解 | `@RabbitMqConsumption` | 声明式消息消费，支持防重、重试、锁续期 |
| 消费切面 | `RabbitMqProcessAspect` | AOP实现消息消费的完整流程 |
| 责任链处理 | `RabbitMqProcess` | 消息处理责任链接口 |
| 消费锁管理 | `ConsumptionLockManager` | 防止重复消费的分布式锁 |
| 重试管理 | `RetryMessageManager` | 消息重试控制 |
| ACK管理 | `AckRMQManager` | 消息确认管理 |
| 重试计数 | `RetryCountManager` | 原子递增重试次数 |
| 失败持久化 | `SendFailurePersistenceManager` | 发送失败消息持久化 |
| 完成校验策略 | `CompletionVerificationStrategyManager` | 业务完成校验策略 |
| 校验策略路由 | `CompletionVerificationRouterManager` | 根据队列名路由校验策略 |

### 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-rabbitmq</artifactId>
    <version>${version}</version>
</dependency>
```

**步骤2：配置RabbitMQ连接（必须）**

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    listener:
      simple:
        acknowledge-mode: manual  # 必须设置为手动ACK
```

**步骤3：实现失败消息持久化管理器（推荐）**

继承 `SendFailurePersistenceManager`，定义发送失败消息的持久化逻辑：

```java
@Primary
@Component
public class DefaultSendFailurePersistenceManager implements SendFailurePersistenceManager {
    
    @Autowired
    private FailedMessageMapper failedMessageMapper;
    
    @Override
    public void saveConfirmFailure(String correlationId, String cause, 
                                   String exchange, String routingKey,
                                   DefaultMessage defaultMessage, byte[] messageBody) {
        FailedMessage record = new FailedMessage();
        record.setCorrelationId(correlationId);
        record.setCause(cause);
        record.setExchange(exchange);
        record.setRoutingKey(routingKey);
        record.setMessageBody(messageBody);
        record.setStatus("PENDING_RETRY");
        record.setCreateTime(LocalDateTime.now());
        
        failedMessageMapper.insert(record);
        
        log.error("消息发送失败，已持久化: correlationId={}", correlationId);
    }
    
    @Override
    public void saveReturnFailure(Message message, int replyCode, 
                                  String replyText, String exchange, String routingKey) {
        // 类似实现...
    }
}
```

**步骤4：实现业务完成校验策略（可选，按队列配置）**

为需要二次确认的队列实现 `CompletionVerificationStrategyManager`：

```java
@Component
public class OrderCompletionVerificationStrategy implements CompletionVerificationStrategyManager {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Override
    public boolean isCompleted(String businessId, DefaultMessage defaultMessage, Message message) {
        // 查询订单状态，确认订单已创建
        Order order = orderMapper.selectById(businessId);
        return order != null && order.getStatus() == OrderStatus.CREATED;
    }
    
    @Override
    public String getQueueName() {
        return "order.create.queue";
    }
}
```

**说明：**
- 如果不实现任何校验策略，框架默认信任Redis中的锁状态
- 每个队列可以配置不同的校验策略
- 校验策略用于防止"假成功"情况（业务方法返回但实际未成功）

### 使用示例

**1. 基本消息消费（防重+重试+锁续期）**

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

**2. 自定义前置处理器（责任链）**

通过实现 `RabbitMqProcess` 接口，在业务执行前进行预处理：

```java
@Component
public class LogRMQProcess implements RabbitMqProcess {
    
    @Override
    public RMQKindProcess getProcess() {
        // 需在枚举中定义 LOG_PROCESS
        return RMQKindProcess.LOG_PROCESS;
    }
    
    @Override
    public void execution(Message message, Channel channel, RabbitMqConsumption annotation) {
        // 记录消息消费日志
        String correlationId = message.getMessageProperties().getCorrelationId();
        log.info("开始消费消息: {}", correlationId);
    }
}
```

在枚举中注册：

```java
public enum RMQKindProcess implements DefaultKindProcess {
    TEST("测试步骤", false, 1),
    LOG_PROCESS("日志记录", true, 2);  // 新增
}
```

**3. 消息重试机制**

框架内部自动处理，无需手动调用：

```java
// 业务异常 → 释放锁 → NACK（不放回）→ 触发重试
try {
    // 执行业务逻辑
} catch (Exception e) {
    // 框架自动处理：
    // 1. 释放锁
    // 2. NACK消息（不放回队列）
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

**4. 锁续期机制**

当业务执行时间超过预估的 `businessTime` 时，分布式锁可能提前过期。锁续期机制会每隔 `businessTime / 3` 的时间刷新锁的TTL。

```java
// 框架自动启动续期任务，无需手动调用
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

**5. RabbitMqProcessAspect的核心Manager支持**

RabbitMQ模块提供了7个核心Manager，协同工作实现完整的消息消费流程：

**(1) ConsumptionLockManager - 消费防重锁管理器**

防止同一条消息被重复消费，支持5种锁状态：

```java
public interface ConsumptionLockManager {
    // 尝试获取消费锁（原子操作）
    boolean tryAcquire(String queue, String correlationId, int businessTime, TimeUnit timeUnit);
    
    // 续期锁的过期时间（用于长时间运行的业务）
    boolean renewLock(String queue, String correlationId, int businessTime, TimeUnit timeUnit);
    
    // 检查已存在的锁状态，返回处理建议
    LockStatus checkLockStatus(String queue, String correlationId, DefaultMessage defaultMessage, Message message);
    
    // 删除锁
    void release(String queue, String correlationId);
    
    // 将锁标记为已完成
    void markAsDone(String queue, String correlationId, String businessId, long effectiveSeconds, TimeUnit timeUnit);
    
    // 重置锁为处理中状态（用于校验失败时重新进入处理状态）
    void resetToProcessing(String queue, String correlationId, int businessTime, TimeUnit timeUnit);
}

// 锁状态枚举
enum LockStatus {
    NOT_EXISTS,                          // 锁不存在
    PROCESSING,                          // 处理中
    COMPLETED_AND_VERIFIED,              // 已完成且校验通过
    COMPLETED_BUT_VERIFICATION_FAILED,   // 已完成但校验未通过
    PROCESSING_WITH_RECOVERED_TTL        // 处理中但TTL异常长，已自动缩短
}
```

**(2) RetryMessageManager - 消息重试管理器**

管理业务失败和校验失败两种重试场景：

```java
public interface RetryMessageManager {
    // 处理业务失败重试（数据库超时、第三方API失败等）
    void handleBusinessFailureRetry(
        String retryKey,      // 重试计数Key
        Message message,      // 原始消息
        Channel channel,      // MQ通道
        int maxRetryCount,    // 最大重试次数
        Exception cause,      // 失败原因
        DefaultMessage defaultMessage  // 解析后的消息体
    );
    
    // 处理校验失败重试（数据一致性校验未通过）
    void handleVerificationFailureRetry(
        String verifyRetryKey,  // 校验重试Key（与业务重试隔离）
        Message message,
        Channel channel,
        int maxRetryCount,
        Exception cause,
        DefaultMessage defaultMessage
    );
}
```

**重试策略：**
- 重试次数 < 最大次数：消息重新入队（专用的延迟队列）
- 重试次数 >= 最大次数：调用 `SendFailurePersistenceManager` 持久化失败消息

**(3) AckRMQManager - 消息确认管理器**

封装RabbitMQ的ACK/NACK操作：

```java
public interface AckRMQManager {
    // 确认消息已成功消费
    void basicAck(Channel channel, Long deliveryTag);
    
    // 拒绝消息（可选择是否重新入队）
    void basicNack(Channel channel, Long deliveryTag, boolean requeue);
}
```

**(4) RetryCountManager - 重试次数管理器**

使用Redis原子递增控制重试次数：

```java
public interface RetryCountManager {
    // 原子递增计数，并设置过期时间（若首次创建）
    long incrementAndGet(String retryKey);
    
    // 删除计数（消费成功后清理）
    void delete(String retryKey);
    
    // 构建业务失败重试 Key: "queue:correlationId"
    String buildRetryKey(String queue, String correlationId);
    
    // 构建校验失败重试 Key（独立计数）: "queue:verify:correlationId"
    String buildVerifyRetryKey(String queue, String correlationId);
}
```

**(5) SendFailurePersistenceManager - 发送失败持久化管理器**

集成方需实现此接口，持久化失败消息并提供补偿机制。

**接口定义：**

```java
public interface SendFailurePersistenceManager {
    // 保存到交换机失败的消息（ConfirmCallback ack=false）
    void saveConfirmFailure(
        String correlationId,     // 消息唯一ID
        String cause,             // 失败原因
        String receivedExchange,  // 目标交换机
        String receivedRoutingKey,// 路由键
        DefaultMessage defaultMessage, // 消息体对象
        byte[] messageBody        // 原始消息字节数组（兜底）
    );
    
    // 保存路由失败的消息（ReturnCallback触发）
    void saveReturnFailure(
        Message message,    // 原始消息
        int replyCode,      // 回复码
        String replyText,   // 回复文本
        String exchange,    // 交换机
        String routingKey   // 路由键
    );
}
```

**实现示例请参考"集成方式"步骤3。**

**(6) CompletionVerificationStrategyManager - 业务完成校验策略**

每个队列可配置不同的校验实现，验证业务是否真正完成。

**接口定义：**

```java
public interface CompletionVerificationStrategyManager {
    /**
     * 判断指定业务ID对应的业务是否已完成
     *
     * @param businessId 业务方法返回的唯一标识
     * @param defaultMessage 消息体对象
     * @param message 原始消息对象
     * @return true-已完成，可安全丢弃消息；false-未完成，应重新消费
     */
    boolean isCompleted(String businessId, DefaultMessage defaultMessage, Message message);
    
    // 返回该策略支持的队列名称
    String getQueueName();
}
```

**实现示例请参考"集成方式"步骤4。**

**(7) CompletionVerificationRouterManager - 校验策略路由器**

根据队列名自动路由到对应的校验策略：

```java
public interface CompletionVerificationRouterManager {
    /**
     * 根据队列名获取校验策略，若未找到则返回null
     *
     * @param queueName 队列名称
     * @return 对应的策略，或null表示无需校验（默认信任Redis）
     */
    CompletionVerificationStrategyManager getStrategy(String queueName);
}
```

**工作流程：**

```
消息到达
   ↓
ConsumptionLockManager.tryAcquire()  // 尝试获取锁
   ↓
├─ 成功 → 执行消费逻辑
│         ↓
│      业务成功 → markAsDone() 标记完成
│         ↓
│      ACK消息
│
└─ 失败 → checkLockStatus() 检查锁状态
          ├─ PROCESSING → NACK（不放回）
          ├─ COMPLETED_AND_VERIFIED → ACK
          ├─ COMPLETED_BUT_VERIFICATION_FAILED → 重置锁 + 重试
          └─ PROCESSING_WITH_RECOVERED_TTL → 重新竞争锁
               
消费过程中：
- RetryCountManager.incrementAndGet()  // 原子递增重试次数
- RenewalScheduler.renewLock()         // 每businessTime/3续期一次
- AckRMQManager.basicAck/Nack()        // 确认/拒绝消息

超过最大重试次数：
- SendFailurePersistenceManager.saveConfirmFailure()  // 持久化失败消息
```

---

[返回主文档](../README.md)

---
name: "simple-common-xxljob"
description: "Provides complete API documentation for simple-common-xxljob module (distributed task scheduling). Invoke when creating/updating/starting/stopping XXL-JOB tasks via HTTP API."
---

# simple-common-xxljob 认知文档

**Maven**: `simple-common-xxljob`
**包路径**: `com.simple.common.xxljob`

## XxlJobManager API

```java
@Autowired
private XxlJobManager xxlJobManager;

// 创建任务
CreateXxlJobTaskRequest req = new CreateXxlJobTaskRequest()
    .setJobGroup(1)                                     // 执行器ID
    .setJobDesc("订单超时取消任务")                       // 任务描述
    .setAuthor("system")                                 // 负责人
    .setScheduleConf("0 0/5 * * * ?")                    // Cron表达式
    .setExecutorHandler("orderTimeoutHandler")            // JobHandler名称
    .setExecutorParam("{\"timeoutMinutes\":30}")          // 任务参数JSON
    .setExecutorTimeout(30)                               // 超时秒数
    .setExecutorFailRetryCount(3);                        // 失败重试次数
String jobId = xxlJobManager.create(req);

// 启动任务
xxlJobManager.start(Integer.parseInt(jobId));

// 立即触发一次（测试用）
xxlJobManager.trigger(Integer.parseInt(jobId));

// 修改任务
UpdateXxlJobTaskRequest updateReq = new UpdateXxlJobTaskRequest()
    .setId(123)
    .setScheduleConf("0 0/10 * * * ?")  // 改为每10分钟
    .setJobDesc("订单超时取消任务(已优化)");
xxlJobManager.update(updateReq);

// 停止/删除
xxlJobManager.end(jobId);    // 停止
xxlJobManager.delete(jobId); // 删除
```

## CreateXxlJobTaskRequest 字段

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `jobGroup` | `Integer` | ✅ | - | 执行器主键ID |
| `scheduleConf` | `String` | ✅ | - | 调度配置（Cron表达式） |
| `executorHandler` | `String` | ✅ | - | JobHandler名称，需和 `@XxlJob` 注解一致 |
| `author` | `String` | ✅ | - | 负责人 |
| `jobDesc` | `String` | ✅ | - | 任务描述 |
| `scheduleType` | `String` | ❌ | `"CRON"` | 调度类型 |
| `glueType` | `String` | ❌ | `"BEAN"` | 运行模式 |
| `executorRouteStrategy` | `String` | ❌ | `"FIRST"` | 路由策略 |
| `misfireStrategy` | `String` | ❌ | `"DO_NOTHING"` | 调度过期策略 |
| `executorBlockStrategy` | `String` | ❌ | `"SERIAL_EXECUTION"` | 阻塞处理策略 |
| `alarmEmail` | `String` | ❌ | - | 报警邮件 |
| `executorParam` | `String` | ❌ | - | 任务参数（JSON格式） |
| `childJobId` | `String` | ❌ | - | 子任务ID |
| `executorTimeout` | `int` | ❌ | `30` | 超时时间（秒） |
| `executorFailRetryCount` | `int` | ❌ | `3` | 失败重试次数 |

## UpdateXxlJobTaskRequest

继承 `CreateXxlJobTaskRequest`，额外字段：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `Integer` | ✅ | 任务主键ID |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-xxljob</artifactId>
</dependency>
```
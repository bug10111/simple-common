# simple-common

> 基于 Spring Boot 3 的 Java 企业级通用基础框架 —— 16 个独立模块，按需引入，零配置启动。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-1.0.1-red.svg)](https://central.sonatype.com/)

`simple-common` 是一套面向中大型 Java 后端项目的通用基础组件库。采用严格的模块化设计，每个模块独立解耦、按需引入，基于 Spring Boot AutoConfiguration 实现自动装配。开箱即用，无需修改原有代码结构。

---

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.3.4 |
| 微服务 | Spring Cloud | 2023.0.3 |
| 微服务 | Spring Cloud Alibaba | 2023.0.3.4 |
| ORM | MyBatis-Plus | 3.5.9 |
| 缓存 | Redisson | 3.36.0 |
| 消息队列 | RabbitMQ | — |
| WebSocket | Netty | 4.1.100.Final |
| 日志传输 | Protobuf | 3.24.0 |
| 任务调度 | XXL-JOB | 2.4.1 |
| Excel | EasyExcel + Apache POI | 4.0.3 / 5.2.3 |
| Word | Poi-Tl | 1.12.2 |
| 工具库 | Hutool | 5.8.38 |
| JSON | Fastjson2 | 2.0.61 |
| JWT | jjwt | 0.12.6 |
| Java | JDK | 17+ |

---

## 适用场景

- **新项目快速启动** — 直接使用框架提供的功能模块，快速搭建项目
- **旧项目功能增强** — 按需引入特定模块，无侵入式增强项目功能
- **微服务架构** — 模块化设计支持微服务拆分，单体应用可平滑过渡到微服务
- **政府/国企项目** — 完整国密算法支持（SM2/SM3/SM4），符合安全合规要求

---

## 模块总览

| 模块 | 说明 | 文档 |
|------|------|------|
| simple-common-core | 核心基础：统一响应、雪花ID、加解密、线程池、分布式锁 | [📖 文档](doc/modules/simple-common-core.md) |
| simple-common-mp | MyBatis-Plus 增强：分页基类、审计字段自动填充、数据权限 | [📖 文档](doc/modules/simple-common-mp.md) |
| simple-common-cache | 两级缓存：Caffeine + Redis，防击穿/穿透/雪崩 | [📖 文档](doc/modules/simple-common-cache.md) |
| simple-common-redis | Redis 声明式：@RedisCache、@CurrentLimiting、Redisson 锁 | [📖 文档](doc/modules/simple-common-redis.md) |
| simple-common-auth | 权限认证：@HasAuthority、@Sign、@CsrfDefense、JWT Token | [📖 文档](doc/modules/simple-common-auth.md) |
| simple-common-alibaba | Sentinel 两级限流 + Feign 统一配置 | [📖 文档](doc/modules/simple-common-alibaba.md) |
| simple-common-annex | S3 协议文件管理（MinIO / 阿里云OSS / AWS S3） | [📖 文档](doc/modules/simple-common-annex.md) |
| simple-common-doc | Word 模板替换（Poi-Tl） | [📖 文档](doc/modules/simple-common-doc.md) |
| simple-common-excel | Excel 导入导出（EasyExcel + POI 双引擎） | [📖 文档](doc/modules/simple-common-excel.md) |
| simple-common-eventbus | 事件驱动架构（同步/异步MQ/延迟发布） | [📖 文档](doc/modules/simple-common-eventbus.md) |
| simple-common-logs | TCP + Protobuf 高性能日志收集（client + server） | [📖 文档](doc/modules/simple-common-logs.md) |
| simple-common-rabbitmq | RabbitMQ 消息队列（防重复消费、重试、死信） | [📖 文档](doc/modules/simple-common-rabbitmq.md) |
| simple-common-sms | 短信服务（验证码/模板短信/校验责任链） | [📖 文档](doc/modules/simple-common-sms.md) |
| simple-common-websocket | Netty WebSocket（多端登录、注解式消息分发） | [📖 文档](doc/modules/simple-common-websocket.md) |
| simple-common-xxljob | XXL-JOB 分布式任务调度（HTTP API 动态管理） | [📖 文档](doc/modules/simple-common-xxljob.md) |

---

## 核心特性

### 🔧 核心工具集（simple-common-core）

所有模块的基础依赖，提供项目级通用能力：

- **统一响应** `R<T>` — 标准化 `{code, message, data}` 响应结构
- **雪花 ID / UUID** — `IdUtils` 分布式唯一 ID 生成
- **加解密** — `CryptoUtil` 支持 AES / SM4 / RSA / SM2 / BCrypt，覆盖国密场景
- **线程池 & 定时任务** — `ThreadUtils` 统一线程池管理
- **分布式锁** — `LockService` 接口化分布式锁
- **JSON 转化** — `JsonUtils` 基于 Fastjson2
- **Bean 拷贝** — `BeanUtils` 继承 Hutool BeanUtil，增强属性复制
- **签名工具** — `SignUtils` 接口签名验证
- **二维码** — `QRCodeUtils` 基于 ZXing

### 🗄️ MyBatis-Plus 增强（simple-common-mp）

- `PageBase` 分页基类（current / size / pageSort 排序）
- `CustomIdGenerator` 雪花 ID 自动生成
- `MybatisPlusOperationHandler` 自动填充 `createTime` / `updateTime`
- `@DataScopeTable` 数据权限注解

### 💾 两级缓存（simple-common-cache）

Caffeine 本地缓存 + Redis 分布式缓存双层架构：

- `CacheUtils` 防缓存击穿 / 穿透 / 雪崩的分布式缓存获取
- Caffeine 支持手动加载和自动加载两种模式
- 随机过期时间防雪崩

### 🔴 Redis 声明式（simple-common-redis）

- `@RedisCache` — 声明式缓存，随机过期防雪崩
- `@CurrentLimiting` — 接口限流（按 URL / 用户ID / IP 维度）
- `RedissonLockService` — 可重入锁 / 公平锁 / CountDownLatch / Semaphore

### 🔐 权限认证（simple-common-auth）

分为 `auth-client` 和 `auth-server` 两个子模块：

- `@HasAuthority` — 基于注解的权限校验
- `@Sign` — 接口签名验证
- `@CsrfDefense` — CSRF 防御
- `LoginManager` / `LoginService` — 登录管理
- `TokenManager` — JWT Token 管理
- `PermissionManageService` — 权限管理

### 🛡️ Sentinel 限流 + Feign（simple-common-alibaba）

- `UserRateLimitAspect` — 按用户 ID 对 Controller 做接口级 + 全局级两级限流
- `FeignConfig` — 统一请求拦截器、编码器等基础配置

### 📁 文件管理（simple-common-annex）

S3 协议统一封装，一套 API 兼容三种存储：

- 支持 MinIO / 阿里云 OSS / AWS S3
- `AnnexService` — 上传 MultipartFile / 输入流、浏览器下载、临时签名 URL、删除
- 支持公开 / 私有访问权限

### 📝 Word 文档（simple-common-doc）

基于 Poi-Tl 的 Word 模板替换：

- `Docs.builder()` — 构建模板数据（文本 / 图片 / 表格 / 列表 / 超链接 / 颜色）
- `DocReplaceService` — 替换模板并输出到浏览器下载 / 输出流 / 输入流

### 📊 Excel 导入导出（simple-common-excel）

EasyExcel + POI 双引擎：

- `EasyExcelWriteService` — 全量写入 / 流式分批写入，导出到浏览器或输出流
- `EasyExcelReadService` — 从 MultipartFile / 文件路径 / 输入流读取
- `PoiWriteService` — 复杂导出（自定义列宽 / 合并单元格 / 分页 Sheet）

### 📡 事件驱动（simple-common-eventbus）

- `@Event` 定义事件类，`@EventHandler` 定义事件处理器
- `EventBusService` 发布事件，支持同步 / 异步 MQ / 延迟发布三种模式
- 服务内解耦与跨服务事件通信

### 🐰 消息队列（simple-common-rabbitmq）

- `RabbitMqService` — 发送即时 / 延迟消息（`DefaultMessage` 含 msgData / bizType / 雪花 ID）
- `@RabbitMqConsumption` — 防重复消费注解（业务时长 / 有效时长 / 重试次数可配置）
- 完整的死信处理和重试机制

### 📋 日志收集（simple-common-logs）

TCP + Protobuf 高性能日志收集架构，分为三个子模块：

- `logs-proto` — Protobuf 协议定义
- `logs-client` — `LogService` / `BufferedLogManager` 缓冲队列批量发送，失败降级本地文件，优雅停机
- `logs-server` — `LogProtobufTcpServer` / `AbsLogsSaveManager` 接收存储

### 📱 短信服务（simple-common-sms）

- `SmsService` — 发送验证码、模板短信、校验验证码
- 责任链模式校验（IP 限制 / 手机号限制 / 时间间隔限制）
- 默认阿里云短信实现，可扩展其他厂商

### 🔌 WebSocket（simple-common-websocket）

基于 Netty 的高性能 WebSocket：

- `@WebSocketListening` — 注解式按 type / cliKey 分发消息
- `ChannelMap` — 多级通道存储（按用户 ID + 设备管理 Channel，支持多端登录）
- 内置认证 Handler 和空闲连接清理

### ⏰ 任务调度（simple-common-xxljob）

- `XxlJobManager` — 通过 HTTP API 创建任务（Cron / JobHandler / 超时 / 重试）
- 启动 / 停止 / 立即触发 / 修改调度配置 / 删除任务

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Spring Boot | 3.x |
| Maven | 3.6+ |

### Maven 依赖

根据业务需要引入对应模块，`simple-common-core` 为必选基础模块。本项目未提供 BOM，需在每个依赖中显式指定版本号（当前版本 `1.0.1`）：

```xml
<dependencies>
    <!-- 核心模块（必选） -->
    <dependency>
        <groupId>com.simple.common</groupId>
        <artifactId>simple-common-core</artifactId>
        <version>1.0.1</version>
    </dependency>

    <!-- 按需引入其他模块 -->
    <dependency>
        <groupId>com.simple.common</groupId>
        <artifactId>simple-common-mp</artifactId>
        <version>1.0.1</version>
    </dependency>
    <dependency>
        <groupId>com.simple.common</groupId>
        <artifactId>simple-common-redis</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```

### 最小配置示例

```yaml
# application.yaml
spring:
  # Redis 连接（使用 redis/cache/auth 等模块时需要）
  data:
    redis:
      host: 127.0.0.1        # Redis 服务器地址 [必填]
      port: 6379              # Redis 服务器端口，默认 6379
      database: 0             # Redis 数据库索引，默认 0

  # 数据源（使用 mp/sms 等模块时需要）
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/your_database  # JDBC 连接地址 [必填]，替换 your_database 为实际库名
    username: root             # 数据库用户名 [必填]
    password: your_password    # 数据库密码 [必填]
    driver-class-name: com.mysql.cj.jdbc.Driver  # JDBC 驱动类名，默认 com.mysql.cj.jdbc.Driver
```

### 使用示例

**声明式缓存：**

```java
@RedisCache(key = "'user:' + #id", expire = 3600)
public User getUserById(String id) {
    return userRepository.selectById(id);
}
```

**分布式锁：**

```java
@Autowired
private RedissonLockService lockService;

public void doBusiness(String key) {
    lockService.lock(key, () -> {
        // 业务逻辑
    });
}
```

**事件发布：**

```java
@Event
public class OrderCreatedEvent {
    private String orderId;
    // ...
}

@EventHandler
public class OrderCreatedHandler {
    public void handle(OrderCreatedEvent event) {
        // 处理订单创建事件
    }
}

// 发布事件
eventBusService.publish(new OrderCreatedEvent());
```

---

## 🏗️ 模块架构

```
                    ┌─────────────────┐
                    │  simple-common   │
                    │      -core       │
                    │  (核心基础模块)   │
                    └───────┬─────────┘
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
    ┌─────┴─────┐    ┌─────┴─────┐    ┌─────┴─────┐
    │    mp     │    │  redis    │    │  cache    │
    │ (MyBatis) │    │ (缓存/锁)  │    │ (两级缓存) │
    └─────┬─────┘    └─────┬─────┘    └───────────┘
          │                 │
    ┌─────┴─────┐    ┌─────┴─────┐
    │   auth    │    │ alibaba   │
    │ (认证授权) │    │ (限流/Feign)│
    └───────────┘    └───────────┘

    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │   annex   │    │    doc    │    │  excel    │
    │ (文件管理) │    │ (Word)   │    │ (Excel)   │
    └───────────┘    └───────────┘    └───────────┘

    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │ eventbus  │    │ rabbitmq  │    │   logs    │
    │ (事件总线) │    │ (消息队列) │    │ (日志收集) │
    └───────────┘    └───────────┘    └───────────┘

    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │    sms    │    │ websocket │    │  xxljob   │
    │ (短信服务) │    │ (实时通信) │    │ (任务调度) │
    └───────────┘    └───────────┘    └───────────┘
```

---

## 设计理念

| 原则 | 说明 |
|------|------|
| **模块化** | 每个功能模块独立封装，按需引入，互不耦合 |
| **自动装配** | 基于 Spring Boot AutoConfiguration，引入即生效 |
| **高度可扩展** | 接口 + 责任链设计，默认实现前缀 `Default`，轻松替换 |
| **无侵入** | 直接引入使用，无需修改原有代码结构 |
| **统一规范** | 命名标准化：`Service`（对外接口）/ `Manager`（内部支撑）/ `Process`（责任链）/ `Abs`（抽象基类） |

---

## 📚 详细文档

各模块的完整文档（包含核心类说明、配置项、使用示例、扩展点、注意事项）请查看：

- 📁 [doc/modules/](doc/modules/) — 模块文档目录
- 📁 [doc/dev-plan/](doc/dev-plan/) — 开发计划文档

---

## 🧪 测试工程

项目包含 [`simple-common-test`](simple-common-test/) 测试工程，提供了所有模块的使用示例：

| 测试控制器 | 演示模块 |
|-----------|---------|
| [CacheController](simple-common-test/src/main/java/com/simple/common/test/controller/CacheController.java) | cache / core |
| [RedisCacheController](simple-common-test/src/main/java/com/simple/common/test/controller/RedisCacheController.java) | redis |
| [RedissonLockController](simple-common-test/src/main/java/com/simple/common/test/controller/RedissonLockController.java) | redis / core |
| [CurrentLimitingController](simple-common-test/src/main/java/com/simple/common/test/controller/CurrentLimitingController.java) | redis / core |
| [ComplaintController](simple-common-test/src/main/java/com/simple/common/test/controller/ComplaintController.java) | alibaba |
| [AnnexController](simple-common-test/src/main/java/com/simple/common/test/controller/AnnexController.java) | annex |
| [DocController](simple-common-test/src/main/java/com/simple/common/test/controller/DocController.java) | doc |
| [EventController](simple-common-test/src/main/java/com/simple/common/test/controller/EventController.java) | eventbus |
| [EasyExcelController](simple-common-test/src/main/java/com/simple/common/test/controller/EasyExcelController.java) | excel |
| [PoiController](simple-common-test/src/main/java/com/simple/common/test/controller/PoiController.java) | excel |
| [WebSocketController](simple-common-test/src/main/java/com/simple/common/test/controller/WebSocketController.java) | websocket |
| [QRCodeAndThreadController](simple-common-test/src/main/java/com/simple/common/test/controller/QRCodeAndThreadController.java) | core |
| [CycleController](simple-common-test/src/main/java/com/simple/common/test/controller/CycleController.java) | core / eventbus |
| [AviatorController](simple-common-test/src/main/java/com/simple/common/test/controller/AviatorController.java) | core |

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交变更 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

---

## 📄 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源协议发布。

Copyright 2024 qty

---

## 致谢

Simple-Common 框架的发展离不开以下优秀开源项目的支持：

### 核心框架

- **[Spring Boot](https://spring.io/projects/spring-boot)** - 应用开发框架
    - Copyright (c) Pivotal Software, Inc.
    - Licensed under Apache License 2.0

- **[Spring Cloud](https://spring.io/projects/spring-cloud)** - 微服务框架
    - Copyright (c) Pivotal Software, Inc.
    - Licensed under Apache License 2.0

- **[MyBatis-Plus](https://baomidou.com/)** - MyBatis 增强工具
    - Copyright (c) baomidou
    - Licensed under Apache License 2.0

### 数据存储

- **[Redis](https://redis.io/)** - 内存数据库
    - Copyright (c) Redis Ltd.
    - Licensed under BSD 3-Clause

- **[Redisson](https://redisson.org/)** - Redis Java 客户端
    - Copyright (c) Nikita Koksharov
    - Licensed under Apache License 2.0

- **[PostgreSQL](https://www.postgresql.org/)** / **[MySQL](https://www.mysql.com/)** - 关系型数据库
    - PostgreSQL: Copyright (c) The PostgreSQL Global Development Group
    - MySQL: Copyright (c) Oracle Corporation

### 消息队列

- **[RabbitMQ](https://www.rabbitmq.com/)** - 消息代理
    - Copyright (c) VMware, Inc. or its affiliates
    - Licensed under Mozilla Public License 2.0

- **[Spring AMQP](https://spring.io/projects/spring-amqp)** - AMQP 支持
    - Copyright (c) Pivotal Software, Inc.
    - Licensed under Apache License 2.0

### 文档与 API

- **[Swagger/OpenAPI](https://swagger.io/)** - API 文档生成
    - Copyright (c) SmartBear Software
    - Licensed under Apache License 2.0

- **[SpringDoc](https://springdoc.org/)** - OpenAPI 3 集成
    - Copyright (c) springdoc
    - Licensed under Apache License 2.0

### 数据处理

- **[EasyExcel](https://easyexcel.opensource.alibaba.com/)** - Excel 处理
    - Copyright (c) Alibaba Group
    - Licensed under Apache License 2.0

- **[Apache POI](https://poi.apache.org/)** - Office 文档处理
    - Copyright (c) The Apache Software Foundation
    - Licensed under Apache License 2.0

- **[Hutool](https://hutool.cn/)** - Java 工具类库
    - Copyright (c) looly
    - Licensed under MIT License

### 对象存储

- **[MinIO](https://min.io/)** - 高性能对象存储
    - Copyright (c) MinIO, Inc.
    - Licensed under GNU AGPL v3

- **[AWS SDK for Java](https://aws.amazon.com/sdk-for-java/)** - AWS S3 SDK
    - Copyright (c) Amazon.com, Inc. or its affiliates
    - Licensed under Apache License 2.0

### 日志与监控

- **[Protobuf](https://developers.google.com/protocol-buffers)** - 数据序列化
    - Copyright (c) Google LLC
    - Licensed under BSD 3-Clause

- **[Elasticsearch](https://www.elastic.co/elasticsearch)** - 搜索引擎（可选）
    - Copyright (c) Elasticsearch BV
    - Licensed under Elastic License 2.0 / SSPL

### 其他依赖

- **[Lombok](https://projectlombok.org/)** - 代码简化工具
    - Copyright (c) The Project Lombok Authors
    - Licensed under MIT License

- **[Jackson](https://github.com/FasterXML/jackson)** - JSON 处理
    - Copyright (c) FasterXML
    - Licensed under Apache License 2.0

- **[Netty](https://netty.io/)** - 网络应用框架
    - Copyright (c) The Netty Project
    - Licensed under Apache License 2.0

- **[Caffeine](https://github.com/ben-manes/caffeine)** - 高性能缓存库
    - Copyright (c) Ben Manes
    - Licensed under Apache License 2.0

感谢以上所有开源项目的作者和贡献者，正是你们的辛勤工作让 Simple-Common 成为可能！

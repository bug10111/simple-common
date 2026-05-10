# Simple-Common 企业级开发框架

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen)
![License](https://img.shields.io/badge/License-Apache--2.0-blue)

> **开源协议**：本项目采用 Apache License 2.0 开源协议，可自由使用、修改和分发。
>
> **项目性质**：这是一个完全开源的通用技术框架，适用于各类 Java 项目开发。

## 项目简介

**Simple-Common** 是一个基于 **Spring Boot 3.3 + Java 17** 构建的模块化企业级开发框架，提供认证授权、分布式缓存、消息队列、事件总线、实时通信等完整的企业级能力。

### 核心特性

#### 安全与认证
- **统一加密工具 CryptoUtil** - 枚举切换AES/SM4/RSA/SM2等国密算法，自动安全提示，BCrypt密码哈希
- **OAuth2认证授权** - 完整的OAuth2协议实现，支持JWT Token、角色权限、白名单机制
- **灵活缓存策略** - Auth模块Redis/Local一键切换，零代码修改适配不同部署环境

#### 缓存与性能
- **两级缓存架构** - Caffeine(本地) + Redis(分布式)，智能读写策略优化
- **分布式锁服务** - Redisson实现可重入锁/公平锁/闭锁/信号量，支持锁续期
- **线程池管理** - 异步执行、定时任务、延迟调度，统一线程资源管理

#### 消息与事件
- **消息队列防重消费** - RabbitMQ + Redisson分布式锁，支持锁续期、僵尸锁恢复、完成校验
- **事件总线双模式** - 同步(立即执行) / 异步(RabbitMQ分发)，支持跨系统事件、延迟事件
- **责任链模式实践** - Auth/RabbitMQ/SMS/Logs多模块采用，轻松扩展自定义逻辑

#### 通信与集成
- **WebSocket实时通信** - 静态工具类WebSocketUtils，支持点对点/广播消息推送
- **日志中心** - TCP + Protobuf高性能传输，client/proto/server三模块架构
- **短信服务** - 阿里云SMS集成，多层防刷机制(IP限流+频次控制+黑名单)

#### 数据与文档
- **Excel处理** - EasyExcel + Apache POI双引擎，支持大数据量导出、模板填充
- **Word文档生成** - poi-tl引擎，支持动态模板替换、表格生成
- **附件管理** - MinIO/AWS S3对象存储，S3协议兼容
- **API文档** - SpringDoc OpenAPI 3.0，自动生成Swagger UI

#### 运维与监控
- **定时任务** - XXL-JOB集成，支持分布式任务调度、失败重试
- **AI集成** - Spring AI Alibaba，支持通义千问等大模型接入
- **MyBatis-Plus增强** - 通用CRUD、分页插件、自动填充

### 技术栈

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

### 设计理念

- **模块化设计** - 各功能模块独立封装，按需引入，减少依赖冗余
- **高度可扩展** - 继承/接口/责任链轻松扩展，默认实现前缀`Default`
- **无侵入集成** - 现有项目直接引入使用，无需修改原有代码
- **统一规范** - 命名/service/manager/Process后缀标准化，异常/响应统一封装
- **性能优化** - 本地缓存/线程池/异步处理/防重消费等多维度优化

### 适用场景

- **新项目快速启动** - 直接使用框架提供的功能模块，快速搭建项目
- **旧项目功能增强** - 按需引入特定模块，无侵入式增强项目功能
- **微服务架构** - 模块化设计支持微服务拆分，单体应用可平滑过渡到微服务
- **政府/国企项目** - 完整国密算法支持(SM2/SM3/SM4)，符合安全合规要求

---

## 模块文档导航

### 核心模块

- [1. simple-common-core](docs/core.md) - 核心工具模块，提供通用工具类、异常处理、响应封装等基础功能

### 认证授权模块

- [2. simple-common-auth](#) - 认证授权模块
  - [2.1 客户端模块](docs/auth-client.md) - 资源服务端认证拦截和权限校验
  - [2.2 服务端模块](docs/auth-server.md) - 用户认证、Token颁发、OAuth2授权

### 缓存与数据模块

- [3. simple-common-cache](docs/cache.md) - 两级缓存架构（Caffeine + Redis）
- [4. simple-common-redis](docs/redis.md) - Redis分布式缓存封装
- [5. simple-common-mp](docs/mp.md) - MyBatis-Plus增强模块

### 消息与事件模块

- [6. simple-common-rabbitmq](docs/rabbitmq.md) - RabbitMQ消息队列封装
- [7. simple-common-eventbus](docs/eventbus.md) - 事件总线（同步/异步双模式）

### 通信与集成模块

- [8. simple-common-websocket](docs/websocket.md) - WebSocket实时通信
- [9. simple-common-excel](docs/excel.md) - Excel处理（EasyExcel + POI）
- [10. simple-common-sms](docs/sms.md) - 短信服务（阿里云SMS）
- [11. simple-common-doc](docs/doc.md) - API文档（SpringDoc OpenAPI）
- [12. simple-common-xxljob](docs/xxljob.md) - 定时任务（XXL-JOB）
- [13. simple-common-logs](docs/logs.md) - 日志中心（TCP + Protobuf）
- [14. simple-common-annex](docs/annex.md) - 附件管理（MinIO/OSS对象存储）

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

## 快速开始

### 1. 添加依赖

在您的项目中引入所需模块：

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-core</artifactId>
    <version>${version}</version>
</dependency>
```

### 2. 配置应用

根据模块文档进行相应配置。

### 3. 开始使用

参考各模块文档中的使用示例。

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

### AI 与大模型

- **[Spring AI Alibaba](https://sca.aliyun.com/ai/)** - AI 应用开发框架
  - Copyright (c) Alibaba Group
  - Licensed under Apache License 2.0

- **[通义千问](https://tongyi.aliyun.com/qianwen/)** - 大语言模型
  - Copyright (c) Alibaba Group

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

---

## Star History

如果这个项目对您有帮助，请给我们一个 ⭐ Star！

[![Star History Chart](https://api.star-history.com/svg?repos=your-repo/simple-common&type=Date)](https://star-history.com/#your-repo/simple-common&Date)

---

**Made with ❤️ by Simple-Common Team**

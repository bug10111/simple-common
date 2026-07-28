# simple-common

> 基于 Spring Boot 3 的 Java 通用框架工具集，提供从核心工具到分布式架构的全栈基础设施封装。

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-1.0.1-red.svg)](https://central.sonatype.com/)

## 📖 项目简介

**simple-common** 是一套基于 Spring Boot 3 的 Java 通用框架工具集，旨在为业务系统提供开箱即用的基础设施能力。项目采用模块化设计，各模块独立解耦、按需引入，覆盖从核心工具类、数据访问、缓存、消息队列、权限认证、文件管理到实时通信等企业级开发场景。

所有模块均基于 Spring Boot AutoConfiguration 自动装配机制，引入依赖即可使用，零配置启动。

## 设计理念

- **模块化设计** - 各功能模块独立封装，按需引入，减少依赖冗余
- **高度可扩展** - 继承/接口/责任链轻松扩展，默认实现前缀`Default`
- **无侵入集成** - 现有项目直接引入使用，无需修改原有代码
- **统一规范** - 命名/service/manager/Process后缀标准化，异常/响应统一封装

## 适用场景

- **新项目快速启动** - 直接使用框架提供的功能模块，快速搭建项目
- **旧项目功能增强** - 按需引入特定模块，无侵入式增强项目功能
- **微服务架构** - 模块化设计支持微服务拆分，单体应用可平滑过渡到微服务
- **政府/国企项目** - 完整国密算法支持(SM2/SM3/SM4)，符合安全合规要求

## ✨ 核心特性

- 🔧 **核心工具集** — 统一响应封装、断言工具、雪花ID、JSON转化、加解密（AES/SM4/RSA/SM2/BCrypt）、线程池、分布式锁、循环任务
- 🗄️ **MyBatis-Plus 增强** — 分页基类、雪花ID自动生成、审计字段自动填充、数据权限拦截器
- 💾 **两级缓存** — Caffeine 本地缓存 + Redis 分布式缓存，防击穿/穿透/雪崩
- 🔴 **Redis 声明式** — `@RedisCache` 声明式缓存、`@CurrentLimiting` 接口限流、Redisson 高级分布式锁
- 🔐 **权限认证** — `@HasAuthority` 权限校验、`@Sign` 签名验证、`@CsrfDefense` CSRF 防御、JWT Token 管理
- 🛡️ **Sentinel 限流** — 按用户ID的两级限流（接口级+全局级）、Feign 客户端统一配置
- 📁 **文件管理** — S3 协议统一接口（MinIO/阿里云OSS/AWS S3），上传/下载/签名URL/删除
- 📝 **Word 文档** — 基于 Poi-Tl 的模板替换，支持文本/图片/表格/列表/超链接
- 📡 **事件驱动** — `@Event` + `@EventHandler` 事件总线，支持同步/异步MQ/延迟发布/循环重试
- 📊 **Excel 导入导出** — EasyExcel + POI 双引擎，流式分批写入、SAX 事件驱动读取
- 📋 **日志收集** — TCP + Protobuf 高性能日志收集，缓冲队列批量发送、失败降级、优雅停机
- 🐰 **消息队列** — RabbitMQ 封装，防重复消费、指数退避重试、死信队列、发送失败持久化
- 📱 **短信服务** — 验证码发送/校验、模板短信、发送前校验责任链（手机号/时间间隔/IP限制）
- 🔌 **WebSocket** — Netty WebSocket，`@WebSocketListening` 消息分发、多端登录、同步请求响应
- ⏰ **任务调度** — XXL-JOB HTTP API 动态管理任务（创建/启动/停止/触发/删除）

## 📦 模块总览

| 模块 | 说明 | 文档 |
|------|------|------|
| simple-common-core | 核心基础模块（R/AssertUtils/IdUtils/CryptoUtil/ThreadUtils等） | [📖 文档](doc/modules/simple-common-core.md) |
| simple-common-mp | MyBatis-Plus 封装（PageBase/CustomIdGenerator/数据权限） | [📖 文档](doc/modules/simple-common-mp.md) |
| simple-common-cache | 两级缓存（Caffeine + Redis，防击穿/穿透/雪崩） | [📖 文档](doc/modules/simple-common-cache.md) |
| simple-common-redis | Redis 声明式缓存/接口限流/Redisson 分布式锁 | [📖 文档](doc/modules/simple-common-redis.md) |
| simple-common-auth | 权限认证（@HasAuthority/@Sign/@CsrfDefense/JWT/登录管理） | [📖 文档](doc/modules/simple-common-auth.md) |
| simple-common-alibaba | Sentinel 两级限流 + Feign 客户端配置 | [📖 文档](doc/modules/simple-common-alibaba.md) |
| simple-common-annex | S3 文件管理（MinIO/OSS/S3，上传/下载/签名URL） | [📖 文档](doc/modules/simple-common-annex.md) |
| simple-common-doc | Word 模板替换（Poi-Tl，文本/图片/表格/列表） | [📖 文档](doc/modules/simple-common-doc.md) |
| simple-common-eventbus | 事件驱动架构（同步/异步MQ/延迟发布/循环重试） | [📖 文档](doc/modules/simple-common-eventbus.md) |
| simple-common-excel | Excel 导入导出（EasyExcel + POI 双引擎） | [📖 文档](doc/modules/simple-common-excel.md) |
| simple-common-logs | TCP+Protobuf 日志收集（client/proto/server） | [📖 文档](doc/modules/simple-common-logs.md) |
| simple-common-rabbitmq | RabbitMQ 消息队列（防重复消费/重试/死信） | [📖 文档](doc/modules/simple-common-rabbitmq.md) |
| simple-common-sms | 短信服务（验证码/模板短信/校验责任链） | [📖 文档](doc/modules/simple-common-sms.md) |
| simple-common-websocket | Netty WebSocket（消息分发/多端登录/同步请求） | [📖 文档](doc/modules/simple-common-websocket.md) |
| simple-common-xxljob | XXL-JOB 分布式任务调度（HTTP API 动态管理） | [📖 文档](doc/modules/simple-common-xxljob.md) |

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

## 📚 详细文档

各模块的完整文档（包含核心类说明、配置项、使用示例、扩展点、注意事项）请查看：

- 📁 [doc/modules/](doc/modules/) — 模块文档目录
- 📁 [doc/dev-plan/](doc/dev-plan/) — 开发计划文档

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

## 🤝 贡献

欢迎提交 Issue 和 Pull Request。

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交变更 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 📄 开源协议

本项目基于 [Apache License 2.0](LICENSE) 开源协议发布。

Copyright 2024 qty

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

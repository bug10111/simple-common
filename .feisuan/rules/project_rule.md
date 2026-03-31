
# 开发规范指南
为保证代码质量、可维护性、安全性与可扩展性，请在开发过程中严格遵循以下规范。

## 一、项目基本信息

- **作者**：Admin
- **用户工作目录**：`C:\start\simple-common`
- **构建工具**：Maven
- **JDK 版本**：Java 17.0.6
- **主框架**：Spring Boot 3.3.4
- **Spring Cloud 版本**：2023.0.3
- **Spring Cloud Alibaba 版本**：2023.0.3.4
- **项目编码**：UTF-8

## 二、项目目录结构

本项目为多模块聚合项目，采用分层与模块化设计。

```text
simple-common
    ├── doc                              # 项目文档（SQL、Nginx配置等）
    │   ├── nginx
    │   ├── sql
    │   └── vp
    ├── simple-common-ai                 # AI 集成模块 (DashScope)
    ├── simple-common-alibaba            # 阿里云相关集成
    ├── simple-common-annex              # 附件管理模块 (MinIO, OSS, S3)
    ├── simple-common-auth               # 认证授权模块
    │   ├── simple-common-auth-client    # 认证客户端 (JWT, 拦截器)
    │   └── simple-common-auth-server    # 认证服务端
    ├── simple-common-cache              # 多级缓存模块 (Caffeine + Redis)
    ├── simple-common-core               # 核心基础模块 (工具类、基础配置、全局异常)
    ├── simple-common-doc                # 文档处理模块 (POI-TL, Word/Excel生成)
    ├── simple-common-eventbus           # 事件总线模块 (基于 RabbitMQ)
    ├── simple-common-excel              # Excel 处理模块 (EasyExcel)
    ├── simple-common-logs               # 日志模块
    │   ├── simple-common-logs-client    # 日志客户端 (AOP采集)
    │   └── simple-common-logs-server    # 日志服务端 (持久化)
    ├── simple-common-mp                 # MyBatis Plus 增强模块
    ├── simple-common-rabbitmq           # RabbitMQ 封装模块
    ├── simple-common-redis               # Redis 封装模块 (Redisson)
    ├── simple-common-sms                # 短信服务模块 (阿里云 SMS)
    ├── simple-common-test               # 测试模块
    ├── simple-common-websocket          # WebSocket 模块 (Netty 实现)
    └── simple-common-xxljob             # XXL-JOB 调度模块
```

## 三、技术栈与核心依赖

| 分类 | 依赖组件 | 版本 | 用途说明 |
|------|----------|------|----------|
| **框架** | Spring Boot | 3.3.4 | 基础框架 |
| | Spring Cloud | 2023.0.3 | 微服务框架 |
| | Spring Cloud Alibaba | 2023.0.3.4 | 阿里微服务生态 |
| **数据库** | MyBatis Plus | 3.5.9 | ORM 框架 |
| | PostgreSQL | - | 主数据库 |
| | MySQL | - | 兼容支持 |
| **缓存** | Redisson | 3.36.0 | 分布式锁、Redis 客户端 |
| | Caffeine | - | 本地缓存 |
| **中间件** | RabbitMQ (AMQP) | - | 消息队列 |
| **工具库** | Hutool | 5.8.38 | Java 工具集 |
| | Fastjson2 | 2.0.61 | JSON 处理 |
| | MapStruct | 1.6.3 | 对象映射 |
| | Lombok | - | 简化代码 |
| | Aviator | 5.4.3 | 表达式引擎 |
| **文档** | Knife4j | 4.5.0 | 接口文档 (Swagger 增强) |
| **办公** | EasyExcel | 4.0.3 | Excel 读写 |
| | POI / POI-TL | 5.2.3 / 1.12.2 | Office 文档处理 |
| **安全** | jjwt | 0.12.6 | JWT 生成与解析 |
| | jBCrypt | 0.4 | 密码加密 |
| **对象存储** | MinIO | 8.5.10 | 对象存储 |
| | Aliyun OSS | 3.10.2 | 阿里云 OSS |
| | AWS S3 SDK | 1.12.594 | S3 协议支持 |
| **任务调度** | XXL-Job | 2.4.1 | 分布式任务调度 |
| **AI** | DashScope SDK | 2.22.12 | 阿里云灵积 |

## 四、分层架构规范

| 层级 | 职责说明 | 开发约束与注意事项 |
|------|----------|---------------------|
| **Controller** | 处理 HTTP 请求与响应，定义 API 接口 | 不得直接访问数据库，必须通过 Service 层调用；使用 Knife4j 注解生成文档 |
| **Service** | 实现业务逻辑、事务管理与数据校验 | 必须通过 Repository/Mapper 层访问数据库；返回 DTO/VO 而非 Entity |
| **Mapper/Repository** | 数据库访问与持久化操作 | 继承 `BaseMapper` (MyBatis Plus)；位于 `mapper` 包下 |
| **Entity** | 映射数据库表结构 | 使用 Lombok 简化；逻辑删除字段统一为 `deleted` (1-删除, 0-正常) |

### 接口与实现分离

- 所有业务逻辑通过接口定义（如 `UserService`），具体实现放在 `impl` 包中（如 `UserServiceImpl`）。
- 跨模块交互推荐使用 `Manager` 层或 `EventBus` 进行解耦。

## 五、通用开发规则

### 1. 配置与环境

- **多环境支持**：通过 Maven Profile 切换环境 (`local`, `dev`, `test`, `produce`)。
- **配置文件**：使用 `application.yaml`，配置项需通过 `@ConfigurationProperties` 映射到实体类。
- **资源过滤**：Maven 构建时已配置资源过滤，支持 `xls`, `xlsx`, `docx` 等二进制文件不被破坏。

### 2. 数据库规范

- **持久层框架**：统一使用 MyBatis Plus。
- **逻辑删除**：全局配置逻辑删除字段 `deleted`，删除值 `1`，未删除值 `0`。
- **Mapper XML**：存放于 `src/main/resources/mapper` 目录下。
- **字段映射**：开启下划线自动转驼峰 `map-underscore-to-camel-case: true`。
- **防注入**：禁止手动拼接 SQL 字符串，必须使用 MP 提供的方法或 XML 注解。

### 3. 缓存策略

- **本地缓存**：使用 Caffeine（`simple-common-cache` 模块）。
- **分布式缓存**：使用 Redis/Redisson（`simple-common-redis` 模块）。
- **缓存注解**：优先使用自定义封装的注解，避免直接操作 RedisTemplate。

### 4. 异步与事件

- **事件驱动**：使用 `simple-common-eventbus` 模块发布订阅事件，基于 RabbitMQ 实现跨服务通信。
- **线程池**：核心模块提供线程池配置，禁止在代码中显式创建 `new Thread`。

### 5. 日志规范

- **日志采集**：引入 `simple-common-logs-client`，通过 AOP 自动采集接口调用日志。
- **日志记录**：使用 `@Slf4j` 注解，禁止使用 `System.out.println`。
- **敏感信息**：日志输出前需对密码、密钥等敏感信息进行脱敏。

### 6. 安全规范

- **认证授权**：使用 `simple-common-auth` 模块。
- **接口鉴权**：Controller 方法添加自定义注解（如 `@RequireLogin`），由拦截器统一处理。
- **XSS 防护**：核心模块已集成 XSS 过滤器。
- **加密算法**：密码加密使用 BCrypt；数据签名使用 JWT。

### 7. 文件上传

- **对象存储**：统一使用 `simple-common-annex` 模块，支持 MinIO、阿里云 OSS、AWS S3。
- **配置**：通过配置文件动态切换存储类型 (`type: oss/minio/s3`)。

## 六、代码风格规范

### 命名规范

| 类型 | 命名方式 | 示例 |
|------|----------|------|
| 类名 | UpperCamelCase | `UserServiceImpl` |
| 方法/变量 | lowerCamelCase | `saveUser()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS` |
| 包名 | 全小写，点分隔 | `com.simple.common.test` |

### 注释规范

- **语言**：使用中文编写注释。
- **Javadoc**：所有 `public` 类、方法、字段必须添加 Javadoc 注释。
- **逻辑注释**：复杂业务逻辑必须添加行内注释说明意图。

### 类型命名规范（阿里巴巴风格）

| 后缀 | 用途说明 | 示例 |
|------|----------|------|
| DTO | 数据传输对象 | `UserDTO` |
| VO | 视图展示对象 | `UserVO` |
| Entity/DO | 数据库实体对象 | `UserEntity` |
| Query | 查询参数封装对象 | `UserQuery` |
| Manager | 内部业务编排层（跨模块调用） | `UserManager` |

### 实体类简化工具

- 使用 Lombok 注解替代手动编写 getter/setter/构造方法：
  - `@Data`
  - `@NoArgsConstructor`
  - `@AllArgsConstructor`
  - `@Builder` (推荐使用构建者模式)

## 七、编码原则总结

| 原则 | 说明 |
|------|------|
| **SOLID** | 高内聚、低耦合，增强可维护性与可扩展性 |
| **DRY** | 避免重复代码，提取公共逻辑到 `simple-common-core` |
| **KISS** | 保持代码简洁易懂 |
| **YAGNI** | 不实现当前不需要的功能 |
| **OWASP** | 防范常见安全漏洞，如 SQL 注入、XSS 等 |

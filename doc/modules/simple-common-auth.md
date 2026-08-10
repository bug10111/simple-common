# simple-common-auth 权限认证模块

> @author qty
> 版本：1.0.0
> 日期：2026-07-28

---

## 1. 模块介绍

`simple-common-auth` 是 simple-common 框架的权限认证核心模块，提供完整的 OAuth2 风格认证授权体系，包括登录认证、Token 管理、权限校验、签名验证、CSRF 防御等功能。

### 1.1 架构设计

模块采用 **client / server 分离设计**，适用于微服务架构中的认证中心 + 业务服务模式：

```
┌─────────────────────────────────────────────────────────────────┐
│                    simple-common-auth                           │
│                     (聚合 POM 模块)                              │
├──────────────────────────┬──────────────────────────────────────┤
│  simple-common-auth-     │  simple-common-auth-                 │
│  client                  │  server                              │
│                          │                                      │
│  · 注解: @HasAuthority   │  · LoginService 登录服务             │
│         @Sign            │  · LoginManager 登录管理器           │
│         @CsrfDefense     │  · ClientDetailsService 客户端认证   │
│  · 拦截器: AuthHandler   │  · PermissionManageService 权限管理  │
│    Interceptor           │  · LoginUserOperationManager 用户   │
│  · TokenManager          │    信息操作                          │
│  · SignManager           │  · UnifiedSecretManager 密钥管理     │
│  · CsrfService           │  · LoginSucProcess 登录成功处理链    │
│  · LoginInfoManager      │  · LoginErrorProcess 登录失败处理链  │
│  · CacheManager          │  · TokenData Token 数据构建          │
│  · 权限变更事件监听       │  · 权限变更事件发布                   │
│  · 密钥事件监听           │                                      │
└──────────────────────────┴──────────────────────────────────────┘         EventBus 事件总线
           ▲                              │
           │   PermissionChangeEvent      │
           │   UserLoggedOutEvent         │
           │   SecretEvent                │
           └──────────────────────────────┘
```

**核心设计理念：**

| 设计点 | 说明 |
|--------|------|
| client/server 分离 | client 用于业务服务（Token 校验、权限拦截），server 用于认证中心（登录、Token 签发、权限管理） |
| 责任链模式 | 登录成功处理、登录失败处理、请求鉴权处理均采用责任链模式，支持灵活扩展 |
| 模板方法模式 | `AbsLoginManager` 提供登录流程模板，子类只需实现 `doLogin` 方法 |
| 事件驱动 | 权限变更、用户登出、密钥变更通过 EventBus 事件广播到所有客户端 |
| 项目维度隔离 | 权限按 `projectCode` 隔离，支持多项目共用同一认证中心 |
| 缓存可插拔 | 通过 `CacheTypeEnum` 支持 Redis 和 Local 两种缓存策略 |

### 1.2 模块依赖关系

```
simple-common-auth-client
  ├── simple-common-core
  ├── simple-common-eventbus
  └── simple-common-cache

simple-common-auth-server
  ├── simple-common-core
  ├── simple-common-eventbus
  └── simple-common-auth-client
```

> server 依赖 client，因为 server 签发 Token 后，client 侧需要验证 Token 和拦截权限。

---

## 2. Maven 依赖

### 2.1 客户端依赖（业务服务）

业务服务只需引入 client 模块：

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-auth-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

client 模块依赖：
- `io.jsonwebtoken:jjwt-impl` — JWT 实现
- `io.jsonwebtoken:jjwt-jackson` — JWT Jackson 序列化
- `simple-common-core` — 框架核心
- `simple-common-eventbus` — 事件总线（接收权限变更事件）
- `simple-common-cache` — 缓存支持

### 2.2 服务端依赖（认证中心）

认证中心需引入 server 模块（自动传递 client 依赖）：

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-auth-server</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2.3 自动装配

| 模块 | AutoConfiguration.imports 文件 | 注册的配置类 |
|------|-------------------------------|-------------|
| client | [`org.springframework.boot.autoconfigure.AutoConfiguration.imports`](simple-common-auth/simple-common-auth-client/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1) | [`ClientAuthConfig`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/config/ClientAuthConfig.java:25) |
| server | [`org.springframework.boot.autoconfigure.AutoConfiguration.imports`](simple-common-auth/simple-common-auth-server/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1) | [`ServerAuthConfig`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/config/ServerAuthConfig.java:23) |

---

## 3. 核心功能表格

### 3.1 客户端核心功能

| 功能 | 核心类/接口 | 说明 |
|------|------------|------|
| 权限校验注解 | [`@HasAuthority`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/HasAuthority.java:14) | 方法级权限校验，超级管理员自动放行 |
| 签名验证注解 | [`@Sign`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/Sign.java:14) | 接口签名验证，防篡改 + 防重放 |
| CSRF 防御注解 | [`@CsrfDefense`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/CsrfDefense.java:13) | CSRF Token 校验，兼防重复提交 |
| 请求拦截器 | [`AuthHandlerInterceptor`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/interceptor/AuthHandlerInterceptor.java:31) | 统一鉴权拦截，责任链执行 |
| Token 管理 | [`TokenManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/token/TokenManager.java:22) | Token 创建、验证、密钥管理 |
| 签名管理 | [`SignManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/sign/SignManager.java:25) | 签名生成、验证、防重放 |
| CSRF 服务 | [`CsrfService`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/service/CsrfService.java:57) | CSRF Token 生成、保存、校验 |
| 登录信息管理 | [`LoginInfoManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/user/LoginInfoManager.java:21) | 用户信息、权限信息获取 |
| 缓存管理 | [`CacheManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/cache/CacheManager.java:12) | 统一缓存接口（Redis/Local） |
| 权限自动加载 | [`PermissionAutoLoader`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/permission/PermissionAutoLoader.java:43) | 缓存未命中时从数据库回源加载 |
| 权限变更监听 | [`PermissionChangeEventHandler`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/handler/PermissionChangeEventHandler.java:43) | 接收权限变更事件，更新本地缓存 |
| 登录用户工具 | [`LoginUserUtils`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/util/LoginUserUtils.java:14) | ThreadLocal 获取当前登录用户 |

### 3.2 服务端核心功能

| 功能 | 核心类/接口 | 说明 |
|------|------------|------|
| 登录服务 | [`LoginService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/login/LoginService.java:31) | 登录、刷新 Token、登出 |
| 登录管理器 | [`LoginManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/login/LoginManager.java:47) | 特定登录类型认证逻辑 |
| 登录管理基类 | [`AbsLoginManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/login/AbsLoginManager.java:40) | 模板方法模式，封装登录流程 |
| 客户端认证 | [`ClientDetailsService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/client/ClientDetailsService.java:41) | OAuth 客户端身份验证 |
| 权限管理 | [`PermissionManageService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/permission/PermissionManageService.java:33) | 角色权限增删改查，事件广播 |
| 用户信息操作 | [`LoginUserOperationManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/user/LoginUserOperationManager.java:16) | 登录信息存储、登出清除 |
| 密钥管理 | [`UnifiedSecretManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/secret/UnifiedSecretManager.java:64) | JWT/签名密钥统一管理 |
| 客户端管理 | [`ClientManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/client/ClientManager.java:24) | 客户端凭证加解密 |
| 登录成功处理 | [`LoginSucProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/process/LoginSucProcess.java:32) | 责任链：保存信息、记录日志 |
| 登录失败处理 | [`LoginErrorProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/process/LoginErrorProcess.java:31) | 责任链：IP 限制、账号锁定 |
| Token 数据 | [`TokenData`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/entity/TokenData.java:25) | 构建 AccessToken/RefreshToken 载荷 |
| 用户详情基类 | [`AbsUserDetails`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/entity/AbsUserDetails.java:23) | 登录用户信息载体 |
| 客户端详情 | [`ClientDetails`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/entity/ClientDetails.java:20) | OAuth 客户端配置信息 |
| 登录类型适配 | [`LoginTypeAdapter`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/adapter/LoginTypeAdapter.java:11) | 登录类型路由到对应 LoginManager |

---

## 4. 配置说明

### 4.1 客户端配置（`simple.auth.*`）

配置类：[`AuthProperties`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/properties/AuthProperties.java:21)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `simple.auth.server-url` | `http://localhost:8000` | 认证服务端地址 |
| `simple.auth.time-out` | `10000` | 获取服务端用户信息超时时间（毫秒） |
| `simple.auth.cache-type` | `REDIS` | 缓存类型：`REDIS` / `LOCAL` |
| `simple.auth.project-code` | **必填** | 项目编码，用于权限隔离 |
| `simple.auth.super-admin-role-key` | `admin` | 超级管理员角色标识，自动拥有所有权限 |
| `simple.auth.permission-cache-expire` | `86400` | 权限缓存过期时间（秒），默认 24 小时 |
| `simple.auth.login-error-number` | `5` | 登录失败最大次数 |
| `simple.auth.login-error-time` | `86400` | 登录失败计次单位时间（秒） |
| `simple.auth.decrypt-check-validity-period` | `false` | 解密是否校验有效时间 |
| `simple.auth.decrypt-validity-period` | `2` | 解密字符串有效期（分钟） |

### 4.2 签名配置（`simple.sign.*`）

配置类：[`SignProperties`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/properties/SignProperties.java:20)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `simple.sign.sign-defense` | `true` | 是否开启签名校验 |
| `simple.sign.sign` | `X-SIGN` | 签名请求头名称 |
| `simple.sign.timestamp` | `X-TIMESTAMP` | 时间戳请求头名称 |
| `simple.sign.nonce` | `X-NONCE` | 随机数请求头名称 |
| `simple.sign.cache-time` | `300` | nonce 缓存时间（秒） |
| `simple.sign.default-time-window-ms` | `300000` | 时间窗口（毫秒），默认 5 分钟 |
| `simple.sign.cache-type` | `REDIS` | 缓存类型 |

### 4.3 CSRF 配置（`simple.csrf.*`）

配置类：[`CsrfProperties`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/properties/CsrfProperties.java:20)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `simple.csrf.csrf-defense` | `true` | 是否开启 CSRF 防御 |
| `simple.csrf.csrf-header` | `X-CSRF-TOKEN` | CSRF Token 请求头名称 |
| `simple.csrf.cache-time` | `1800` | CSRF Token 缓存时间（秒），默认 30 分钟 |
| `simple.csrf.cache-type` | `REDIS` | 缓存类型 |

### 4.4 服务端配置（`simple.auth.server.*`）

配置类：[`AuthServerProperties`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/properties/AuthServerProperties.java:18)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `simple.auth.server.cache-type` | `REDIS` | 服务端缓存类型 |

### 4.5 配置示例

```yaml
simple:
  auth:
    server-url: http://auth-center:8000   # 认证服务端地址，默认 http://localhost:8000
    project-code: my-business-service     # 项目编码 [必填]，用于权限隔离
    super-admin-role-key: admin           # 超级管理员角色标识，默认 "admin"
    cache-type: REDIS                     # 缓存类型（REDIS/LOCAL），默认 REDIS
    permission-cache-expire: 86400        # 权限缓存过期时间（秒），默认 86400（24小时）
  sign:
    sign-defense: true                    # 是否开启签名校验，默认 true
    cache-type: REDIS                     # 签名 nonce 缓存类型，默认 REDIS
  csrf:
    csrf-defense: true                    # 是否开启 CSRF 防御，默认 true
    cache-type: REDIS                     # CSRF Token 缓存类型，默认 REDIS
```

---

## 5. 核心类与接口详细说明

### 5.1 注解

#### 5.1.1 `@HasAuthority` — 权限校验

[`@HasAuthority`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/HasAuthority.java:14)

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | `String[]` | 权限标识数组，用户拥有其中任一即通过 |

**切面逻辑**（[`HasAuthorityAspect`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/aspect/HasAuthorityAspect.java:30)）：

1. 前置通知（`@Before`），在方法执行前校验
2. 若 `clientAuthInfo.getLogin()` 和 `clientAuthInfo.getAuthentication()` 均为 `true` 时才校验
3. 超级管理员（`loginRole` 包含 `superAdminRoleKey`）直接放行
4. 调用 `loginInfoManager.hasAuth(loginRole, value)` 校验权限
5. 校验失败抛出 `LoginException.INSUFFICIENT_PERMISSIONS`（code=1002）

#### 5.1.2 `@Sign` — 签名验证

[`@Sign`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/Sign.java:14)

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | `boolean` | `true` | 是否启用签名校验 |
| `excludeFields` | `String[]` | `{}` | 参与签名字段中需排除的字段名 |
| `checkTimestamp` | `boolean` | `true` | 是否校验时间戳时效性 |
| `checkNonce` | `boolean` | `true` | 是否校验 nonce 防重放 |

**切面逻辑**（[`SignAspect`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/aspect/SignAspect.java:30)）：

1. 环绕通知（`@Around`），仅允许 POST/PUT 请求
2. 方法参数只允许 1 个（业务参数对象）
3. 从请求头获取 `sign`、`timestamp`、`nonce`
4. 时效性校验：时间差超过 `defaultTimeWindowMs`（默认 5 分钟）则拒绝
5. 构建待签名字符串：业务参数 + `&timestamp=xxx&nonce=xxx`
6. **先验签，再防重放**（防止攻击者用错误签名消耗 nonce）
7. 签名算法：HMAC-SHA256

#### 5.1.3 `@CsrfDefense` — CSRF 防御

[`@CsrfDefense`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/CsrfDefense.java:13)

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `consume` | `boolean` | `true` | 校验后是否立即删除 Token（一次性使用） |

**切面逻辑**（[`CsrfDefenseAspect`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/annotation/aspect/CsrfDefenseAspect.java:29)）：

1. 前置通知（`@Before`），仅在 `csrfProperties.isCsrfDefense()` 为 `true` 时执行
2. 获取当前登录用户的 `userId` 和 `path`
3. 从请求头获取 CSRF Token
4. 调用 `csrfService.checkToken(userId, path, token, consume)` 校验

### 5.2 Token 管理

#### 5.2.1 `TokenManager` 接口

[`TokenManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/token/TokenManager.java:22)

| 方法 | 说明 |
|------|------|
| `String create(Map<String, Object> headers, Map<String, Object> payload)` | 创建 JWT Token |
| `Map<String, Object> check(String token, boolean isRefresh)` | 验证 Token 并返回载荷，`isRefresh` 区分 Access/Refresh |
| `void addSecret(String secret)` | 添加 JWT 签名密钥（仅本地缓存） |
| `String generateSecret()` | 生成新的随机密钥 |

**抽象基类** [`AbsTokenManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/token/AbsTokenManager.java:16) 提供 Token 过期校验：
- Access Token 过期抛出 `LoginException.LOGIN_EXPIRED`（code=1000）
- Refresh Token 过期抛出 `LoginException.RE_LOGIN_EXPIRED`（code=1001）

**默认实现** [`JJwtTokenManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/manager/jwt/JJwtTokenManager.java:22)（`@Primary`）：
- 基于 JJWT 库
- 密钥长度至少 64 位
- 验签失败抛出 `LoginException.RE_LOGIN_EXPIRED`

### 5.3 签名管理

#### 5.3.1 `SignManager` 接口

[`SignManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/sign/SignManager.java:25)

| 方法 | 说明 |
|------|------|
| `void checkTimestamp(String timestamp)` | 校验请求时效性 |
| `void checkNonce(String nonce)` | 防重放校验 |
| `String signWeb(String message)` | 生成 HMAC-SHA256 签名 |
| `boolean verifyWeb(String message, String signature)` | 验证签名 |
| `void addSecret(String secret)` | 添加签名密钥 |
| `String generateSecret()` | 生成新密钥 |
| `String getKey()` | 获取当前签名密钥 |

**默认实现** [`DefaultSignManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/manager/sign/DefaultSignManager.java:24)：
- nonce 防重放使用 Caffeine 本地缓存（容量 10000，过期时间从配置读取）
- 签名密钥通过 [`SignSecretUtils`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/util/SignSecretUtils.java) 管理

### 5.4 CSRF 服务

#### 5.4.1 `CsrfService` 接口

[`CsrfService`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/service/CsrfService.java:57)

| 方法 | 说明 |
|------|------|
| `void saveToken(String userId, String path, String token)` | 保存 CSRF Token |
| `String getToken(String userId, String path)` | 获取 Token |
| `void removeToken(String userId, String path)` | 删除 Token |
| `void checkToken(String userId, String path, String token, boolean consume)` | 校验 Token，`consume=true` 校验后删除 |

**默认实现** [`DefaultCsrfService`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/service/DefaultCsrfService.java:16)：基于 `CacheManager` 存储，缓存 key 格式为 `csrf:{userId}&&{path}`。

**Token 生成端点** [`CsrfTokenController`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/controller/CsrfTokenController.java:30)：
- `GET /csrf/generate?path=xxx` — 生成 32 字节随机 Token，保存到缓存并返回到响应头

### 5.5 登录信息管理

#### 5.5.1 `LoginInfoManager` 接口

[`LoginInfoManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/user/LoginInfoManager.java:21)

| 方法 | 说明 |
|------|------|
| `Map<Object, Object> getUserInfo(String key)` | 根据 jti 获取用户信息 |
| `Map<Object, Map<Object, Object>> getAuthorities(Set<String> loginRole)` | 获取角色权限映射 |
| `Map<Object, Map<Object, Object>> getAuthoritiesByProjectCode(Set<String> loginRole, String projectCode)` | 按项目维度获取权限 |
| `Set<String> getUserToken(String userId)` | 获取用户关联的所有 Token |
| `Boolean hasAuth(Set<String> loginRole, String[] authority)` | 判断是否拥有指定权限 |

**两个 Bean 名称常量：**
- `clientLoginInfoManager` — 客户端模式使用
- `serverLoginInfoManager` — 服务端模式使用

#### 5.5.2 `LoginUserOperationManager` 接口（服务端）

[`LoginUserOperationManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/user/LoginUserOperationManager.java:16) 继承 `LoginInfoManager`，扩展以下方法：

| 方法 | 说明 |
|------|------|
| `void saveUserInfo(TokenData tokenData, boolean isLogin)` | 保存登录信息到缓存 |
| `void loginOut(String userId)` | 完全登出，清除该用户所有 Token |
| `void loginOut(String userId, String jti)` | 部分登出，仅清除指定 Token |
| `void loginOut()` | 当前用户登出 |

**默认实现** [`ServerLoginUserOperationManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/manager/ServerLoginUserOperationManager.java:30)（Bean 名 `serverLoginInfoManager`）：
- 用户信息存储为 Redis Hash，key 格式 `login:info:{jti}`
- 用户 Token 关联存储为 Redis Set，key 格式 `login:id:{userId}`
- 支持 `PermissionAutoLoader` SPI 自动回源加载权限

### 5.6 登录服务

#### 5.6.1 `LoginService` 接口

[`LoginService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/login/LoginService.java:31)

| 方法 | 说明 |
|------|------|
| `Map<String, String> login(Object adapter, LoginTypeAdapter loginType)` | 用户登录，返回 Token 数据 |
| `Map<String, String> refresh(String refreshTokenStr)` | 刷新 Access Token |
| `void logout(String userId)` | 指定用户登出 |
| `void logout()` | 当前用户登出 |

**默认实现** [`DefaultLoginService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/service/login/DefaultLoginService.java:37)：

**登录流程：**
1. 从请求头获取客户端信息（`ClientDetailsService.getClientDetails`）
2. 根据 `LoginTypeAdapter` 路由到对应 `LoginManager`
3. 调用 `LoginManager.login()` 执行认证（模板方法）
4. 校验用户状态（启用、未过期、未锁定）
5. 构建 `TokenData`，生成 AccessToken 和 RefreshToken
6. 执行 `LoginSucProcess` 责任链（保存信息、记录日志）
7. 返回 Token 数据（`type`、`accessToken`、`refreshToken`、`exp`、`scopes`）

**刷新流程：**
1. 验证 RefreshToken，提取 `ati`、`jti`
2. 校验客户端 ID 匹配
3. 从缓存获取用户内省信息
4. 生成新的 AccessToken 和 RefreshToken
5. 先保存新 Token，再删除旧 Token（避免并发问题）
6. 返回新 Token 数据

**登出流程：**
- `logout(userId)`：清除该用户所有 Token + 发布 `UserLoggedOutEvent` 事件
- `logout()`：根据 `oneLogin` 配置决定是清除全部还是仅当前 Token

### 5.7 登录管理器

#### 5.7.1 `LoginManager` 接口

[`LoginManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/login/LoginManager.java:47)

| 方法 | 说明 |
|------|------|
| `boolean support(Object adapter)` | 判断是否支持该登录请求类型 |
| `AbsUserDetails login(ClientDetails clientDetails, Object adapter)` | 执行登录认证 |

#### 5.7.2 `AbsLoginManager` 抽象基类

[`AbsLoginManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/login/AbsLoginManager.java:40) — 模板方法模式：

**`login()` 方法（final，不可重写）执行流程：**
1. `checkErrorNum()` — 校验登录失败次数（责任链 `LoginErrorProcess`）
2. `doLogin()` — 执行具体登录逻辑（子类实现）
3. `loginSuccess()` — 登录成功，清除失败记录
4. 异常时 `loginError()` — 记录失败次数

**子类只需实现：**
```java
protected abstract AbsUserDetails doLogin(ClientDetails clientDetails, Object adapter);
```

### 5.8 客户端认证

#### 5.8.1 `ClientDetailsService` 接口

[`ClientDetailsService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/client/ClientDetailsService.java:41)

| 方法 | 说明 |
|------|------|
| `ClientDetails getClientDetails(String header)` | 从 Authorization 头解析客户端信息 |
| `String getClientToken(String clientId, String clientSecret)` | 生成客户端 Token |
| `ClientDetails getClientDetails(HttpServletRequest request)` | 便捷方法，自动提取请求头 |

**抽象基类** [`AbsClientDetailsService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/service/client/AbsClientDetailsService.java:45)：
- 解析 Basic Auth 格式（`Basic base64(clientId:clientSecret)`）
- 默认 AccessToken 有效期 12 小时，RefreshToken 有效期 30 天
- 子类实现 `checkClientDetails(String clientId, String clientSecret)` 从数据库查询

### 5.9 权限管理

#### 5.9.1 `PermissionManageService` 接口

[`PermissionManageService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/service/permission/PermissionManageService.java:33)

| 方法 | 说明 |
|------|------|
| `void addPermissions(String projectCode, String roleKey, Map<String, String> permissions)` | 新增权限（默认发布事件） |
| `void addPermissions(String projectCode, String roleKey, Map<String, String> permissions, boolean publishEvent)` | 新增权限（可控是否发布事件） |
| `void updateRolePermission(String projectCode, String roleKey, Map<String, String> permissions)` | 全量替换角色权限 |
| `void deletePermission(String projectCode, String roleKey, String permissionKey)` | 删除单个权限 |
| `void deletePermissions(String projectCode, String roleKey, List<String> permissionKeys)` | 批量删除权限 |
| `void clearPermissions(String projectCode, String roleKey)` | 清空角色所有权限 |

**默认实现** [`DefaultPermissionManageService`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/service/permission/DefaultPermissionManageService.java:37)：
- 权限缓存 key 格式：`login:auth:{roleKey}:{projectCode}`
- 所有操作后自动发布 `PermissionChangeEvent` 事件
- `addPermissions` 的 `publishEvent=false` 用于登录时预缓存权限

### 5.10 请求鉴权处理链

#### 5.10.1 `AuthProcess` 接口

[`AuthProcess`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/process/AuthProcess.java:33) 继承 `BasProcessService`，责任链模式。

**执行顺序**（[`AuthInterceptorKindProcess`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/enums/process/AuthInterceptorKindProcess.java:15)）：

| 顺序 | 枚举值 | 处理器 | 说明 |
|------|--------|--------|------|
| 1 | `CHECK_TOKEN` | [`CheckTokenAuthProcess`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/process/CheckTokenAuthProcess.java:45) | Token 合法性校验，解析用户信息到 ThreadLocal |
| 2 | `CHECK_SCOPE_AUTH` | [`CheckScopeAuthProcess`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/process/CheckScopeAuthProcess.java:25) | 授权范围校验 |
| 3 | `CHECK_ROLE` | [`CheckRoleAuthProcess`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/process/CheckRoleAuthProcess.java:28) | 基于 URL 的角色权限校验 |

#### 5.10.2 `AuthHandlerInterceptor` 拦截器

[`AuthHandlerInterceptor`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/interceptor/AuthHandlerInterceptor.java:31) 执行流程：

1. OPTIONS 请求直接放行
2. 若 `clientAuthInfo.getLogin()` 为 `false`，放行
3. IP 白名单校验（若开启）
4. URL 白名单匹配放行
5. 提取 `Authorization: Bearer {token}` 请求头
6. 无 Token 抛出 `LoginException.RE_LOGIN_EXPIRED`
7. 执行 `AuthProcess` 责任链
8. `afterCompletion` 中清除 ThreadLocal

### 5.11 登录成功/失败处理链

#### 5.11.1 `LoginSucProcess`（登录成功处理）

[`LoginSucProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/process/LoginSucProcess.java:32)

| 处理器 | 说明 |
|--------|------|
| [`SaveInfoLoginSucProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/process/SaveInfoLoginSucProcess.java) | 保存用户登录信息到 Redis |
| [`LogLoginSucProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/process/LogLoginSucProcess.java) | 记录登录成功日志 |

#### 5.11.2 `LoginErrorProcess`（登录失败处理）

[`LoginErrorProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/process/LoginErrorProcess.java:31)

| 方法 | 说明 |
|------|------|
| `boolean checkErrorNum(ClientDetails, Object, String ip)` | 检查失败次数是否超限 |
| `void recordError(ClientDetails, Object, String ip)` | 记录失败 |
| `void clearError(ClientDetails, Object, String ip)` | 清除失败记录 |

| 处理器 | 说明 |
|--------|------|
| [`IpLoginErrorProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/process/IpLoginErrorProcess.java) | IP 维度登录失败限制 |

### 5.12 缓存管理

#### 5.12.1 `CacheManager` 接口

[`CacheManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/cache/CacheManager.java:12) 提供统一的缓存操作接口，支持 String、Hash、Set 三种数据结构操作。

**工厂类** [`CacheManagerFactory`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/manager/cache/CacheManagerFactory.java:13)：

| 缓存类型 | 实现类 | 说明 |
|---------|--------|------|
| `REDIS` | [`RedisCacheManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/manager/cache/RedisCacheManager.java) | 基于 `StringRedisTemplate` |
| `LOCAL` | [`LocalCacheManager`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/manager/cache/LocalCacheManager.java) | 基于 Caffeine 本地缓存 |

**三个独立 CacheManager Bean：**

| Bean 名称 | 用途 | 配置来源 |
|-----------|------|---------|
| `authCacheManager` | 用户信息、权限缓存 | `AuthProperties.cacheType` |
| `signCacheManager` | 签名防重放 nonce | `SignProperties.cacheType` |
| `csrfCacheManager` | CSRF Token | `CsrfProperties.cacheType` |

### 5.13 事件驱动

> **对外通讯层（exchange）**：所有客户端-服务端对外通讯统一收敛到 `exchange/` 单一入口。HTTP 出站客户端与端点契约在 `exchange/http/`；事件通讯在 `exchange/event/` 下按职责三层隔离——`event/`（事件契约 `@Event` 对象）、`publisher/`（发布器）、`handler/`（处理器）。事件契约对象位于 client 模块，服务端发布方经 `auth-server → auth-client` 依赖引用；事件发布器两侧皆有（client 侧 `SecretEventPublisher`、server 侧 `LogoutEventPublisher` / `PermissionEventPublisher`）。

#### 5.13.1 权限变更事件

[`PermissionChangeEvent`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/event/PermissionChangeEvent.java:49) — `@Event(targets = {EventConstant.TARGET_ALL_X})`

| 字段 | 类型 | 说明 |
|------|------|------|
| `projectCode` | `String` | 项目编码 |
| `roleKey` | `String` | 角色标识 |
| `permissions` | `Map<String, String>` | 变更的权限数据 |
| `changeType` | [`PermissionChangeTypeEnum`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/enums/PermissionChangeTypeEnum.java:16) | 变更类型 |
| `timestamp` | `Long` | 操作时间戳 |

**变更类型枚举：**

| 枚举值 | 说明 |
|--------|------|
| `ADD` | 新增权限（增量添加） |
| `UPDATE` | 更新权限（全量替换） |
| `DELETE` | 删除权限 |
| `CLEAR` | 清空所有权限 |
| `REFRESH` | 刷新权限 |

**客户端处理器** [`PermissionChangeEventHandler`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/handler/PermissionChangeEventHandler.java:43)：
- 按项目编码过滤，只处理与自身匹配的事件
- 根据变更类型执行不同的缓存更新策略

#### 5.13.2 用户登出事件

[`UserLoggedOutEvent`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/event/UserLoggedOutEvent.java) — 广播到所有系统，触发各客户端清除本地 Token 缓存。

#### 5.13.3 密钥事件

[`SecretEvent`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/event/SecretEvent.java) — 密钥变更事件，客户端通过 [`SecretEventHandler`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/handler/SecretEventHandler.java) 接收并更新本地密钥缓存。

### 5.14 密钥管理

#### 5.14.1 `UnifiedSecretManager` 接口（服务端）

[`UnifiedSecretManager`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/manager/secret/UnifiedSecretManager.java:64)

| 方法 | 说明 |
|------|------|
| `Map<String, String> getSecrets(String projectCode)` | 获取项目密钥（含 `jwt` 和 `sign`） |
| `String generateJwtSecret()` | 生成 JWT 密钥（至少 64 位） |
| `String generateSignSecret()` | 生成签名密钥（至少 32 位） |

#### 5.14.2 `SecretEventPublisher` 接口（客户端）

[`SecretEventPublisher`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/exchange/event/publisher/SecretEventPublisher.java:33)

| 方法 | 说明 |
|------|------|
| `void broadcastJwtSecret(String secret, List<String> targetProjectCodes)` | 广播 JWT 密钥 |
| `void broadcastSignSecret(String secret, List<String> targetProjectCodes)` | 广播签名密钥 |

### 5.15 核心实体

#### 5.15.1 `UserTemporary` — 登录用户临时信息

[`UserTemporary`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/entity/login/UserTemporary.java:23)

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `String` | 用户 ID |
| `nickname` | `String` | 用户昵称 |
| `loginKey` | `String` | 登录标识（区分同用户不同设备） |
| `jti` | `String` | JWT 唯一标识 |
| `path` | `String` | 请求路径 |
| `clientId` | `String` | 客户端 ID |
| `clientName` | `String` | 客户端名称 |
| `appNames` | `String` | 可访问资源名称集合 |
| `wxAppId` | `String` | 微信小程序 AppID |
| `scopes` | `HashSet<String>` | 权限作用域（all/write/read） |
| `loginRole` | `HashSet<String>` | 登录角色集合 |
| `extension` | `Object` | 扩展信息 |
| `dataPermission` | [`DataPermission`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/entity/login/DataPermission.java) | 数据权限对象 |

#### 5.15.2 `AbsUserDetails` — 用户详情基类

[`AbsUserDetails`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/entity/AbsUserDetails.java:23)

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `String` | 用户 ID |
| `nickname` | `String` | 用户名称 |
| `loginRole` | `Set<String>` | 角色 |
| `loginKey` | `String` | 第三方登录标识 |
| `extension` | `Object` | 扩展信息（服务端全局获取） |
| `extensionResponse` | `Map<String, String>` | 扩展信息（直接返回前端） |
| `dataPermission` | `DataPermission` | 数据权限 |
| `isAccountNonExpired` | `int` | 帐户是否过期（1-未过期） |
| `isAccountNonLocked` | `int` | 帐户是否锁定（1-未锁定） |
| `isCredentialsNonExpired` | `int` | 密码是否过期（1-未过期） |
| `isEnabled` | `int` | 帐户是否可用（1-可用） |

#### 5.15.3 `ClientDetails` — 客户端详情

[`ClientDetails`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/entity/ClientDetails.java:20)

| 字段 | 类型 | 说明 |
|------|------|------|
| `clientId` | `String` | 客户端 ID |
| `clientName` | `String` | 客户端名称 |
| `clientSecret` | `String` | 客户端密钥 |
| `wxAppId` | `String` | 微信 AppID |
| `resourceIds` | `String` | 可访问资源名称集合 |
| `scope` | `List<String>` | 作用域（all/write/read） |
| `accessTokenValidity` | `int` | AccessToken 有效期（秒），默认 43200（12 小时） |
| `refreshTokenValidity` | `int` | RefreshToken 有效期（秒），默认 2592000（30 天） |

#### 5.15.4 `ClientAuthInfo` — 客户端权限配置

[`ClientAuthInfo`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/entity/auth/ClientAuthInfo.java:16)

| 配置方法 | 说明 |
|---------|------|
| `openLogin()` | 开启登录验证，所有请求需登录 |
| `openAuthentication()` | 开启权限鉴权 |
| `openOneLogin()` | 开启单点登录限制 |
| `anyClient()` | 标记为客户端模式 |
| `openIPWhitelist()` | 开启 IP 白名单 |
| `antMatchers(url)` | 配置 URL 匹配规则，返回 [`UrlOperation`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/entity/auth/UrlOperation.java) |
| `addScope(scope)` | 添加权限作用域 |

### 5.16 数据权限

[`DataScopeEnum`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/enums/DataScopeEnum.java:20) — 数据权限范围枚举，值越小权限越大：

| 枚举值 | value | 说明 |
|--------|-------|------|
| `ALL` | 1 | 全部数据权限（最高，仅受租户隔离约束） |
| `DEPT_AND_CHILD` | 2 | 部门及以下数据权限 |
| `DEPT` | 3 | 部门数据权限 |
| `SELF` | 4 | 仅本人数据权限（最低） |

### 5.17 错误码

[`LoginException`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/enums/login/LoginException.java:14) 枚举：

| 枚举值 | code | message |
|--------|------|---------|
| `LOGIN_EXPIRED` | 1000 | 登录失效，请重新登录 |
| `RE_LOGIN_EXPIRED` | 1001 | 登录失效，请重新登录 |
| `INSUFFICIENT_PERMISSIONS` | 1002 | 权限不足，请联系管理员 |
| `LOGIN_ERROR_NUM` | 1003 | 登录失败次数过多，请稍后再试 |
| `LOGIN_IP_ERROR_NUM` | 1004 | 该 IP 登录失败次数过多，请稍后再试 |

---

## 6. 使用示例

### 6.1 客户端集成

#### 6.1.1 配置权限信息

继承 [`AbsClientAuthConfig`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/config/AbsClientAuthConfig.java:41)：

```java
@Configuration
public class MyAuthConfig extends AbsClientAuthConfig {

    @Override
    protected void configure(ClientAuthInfo clientAuthInfo) {
        // 开启登录验证和权限鉴权
        clientAuthInfo.openLogin()
                      .openAuthentication();

        // 配置白名单路径
        clientAuthInfo.antMatchers("/api/public/**").permitAll();
        clientAuthInfo.antMatchers("/api/login").permitAll();

        // 配置 URL 角色映射
        clientAuthInfo.antMatchers("/api/admin/**").hasRole("admin");
        clientAuthInfo.antMatchers("/api/user/**").hasAnyRole("admin", "user");
    }
}
```

#### 6.1.2 使用权限注解

```java
@RestController
@RequestMapping("sys/user")
public class SysUserController {

    @HasAuthority("sys:user:list")
    @GetMapping("list")
    public R<List<SysUser>> list() {
        return R.ok(userService.findAll());
    }

    @HasAuthority({"sys:user:create", "sys:user:edit"})
    @PostMapping
    public R<String> create(@RequestBody @Validated CreateSysUserRequest request) {
        return R.ok(userService.create(request));
    }
}
```

#### 6.1.3 使用签名注解

```java
@Sign
@PostMapping("transfer")
public R<Void> transfer(@RequestBody TransferRequest request) {
    transferService.execute(request);
    return R.ok();
}
```

前端需在请求头中携带：
- `X-SIGN`：HMAC-SHA256 签名
- `X-TIMESTAMP`：当前时间戳（毫秒）
- `X-NONCE`：唯一随机数

#### 6.1.4 使用 CSRF 防御

```java
@CsrfDefense
@PostMapping("order/create")
public R<String> createOrder(@RequestBody CreateOrderRequest request) {
    return R.ok(orderService.create(request));
}
```

前端先调用 `GET /csrf/generate?path=/api/order/create` 获取 Token，然后在提交时携带 `X-CSRF-TOKEN` 请求头。

#### 6.1.5 获取当前登录用户

```java
String userId = LoginUserUtils.getUserTemporary().getUserId();
String nickname = LoginUserUtils.getUserTemporary().getNickname();
HashSet<String> roles = LoginUserUtils.getUserTemporary().getLoginRole();
```

### 6.2 服务端集成

#### 6.2.1 实现客户端认证服务

```java
@Service
public class MyClientDetailsService extends AbsClientDetailsService {

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public ClientDetails checkClientDetails(String clientId, String clientSecret) {
        SysClient entity = clientRepository.findByClientId(clientId);
        AssertUtils.notNull(entity, "客户端不存在");
        AssertUtils.isTrue(entity.getClientSecret().equals(clientSecret), "客户端密钥错误");

        return new ClientDetails()
            .setClientId(entity.getClientId())
            .setClientName(entity.getClientName())
            .setClientSecret(entity.getClientSecret())
            .setScope(entity.getScopes())
            .setAccessTokenValidity(entity.getAccessTokenValidity())
            .setRefreshTokenValidity(entity.getRefreshTokenValidity());
    }
}
```

#### 6.2.2 实现登录管理器

```java
@Component
public class PwdLoginManager extends AbsLoginManager {

    @Autowired
    private SysUserService sysUserService;

    @Override
    public boolean support(Object adapter) {
        return adapter instanceof PwdLoginRequest;
    }

    @Override
    protected AbsUserDetails doLogin(ClientDetails clientDetails, Object adapter) {
        PwdLoginRequest request = (PwdLoginRequest) adapter;

        // 验证账号密码
        SysUser user = sysUserService.findByUsername(request.getUsername());
        AssertUtils.notNull(user, "账号不存在");
        AssertUtils.isTrue(BCrypt.checkpw(request.getPassword(), user.getPassword()), "密码错误");

        // 构建用户详情
        return new AbsUserDetails()
            .setUserId(user.getId())
            .setNickname(user.getNickname())
            .setLoginRole(user.getRoleKeys())
            .setIsEnabled(user.getEnabled());
    }
}
```

#### 6.2.3 定义登录类型适配器

```java
public enum MyLoginTypeAdapter implements LoginTypeAdapter {
    PASSWORD(PwdLoginManager.class),
    SMS(SmsLoginManager.class);

    private final Class<? extends LoginManager> loginManagerClass;

    MyLoginTypeAdapter(Class<? extends LoginManager> loginManagerClass) {
        this.loginManagerClass = loginManagerClass;
    }

    @Override
    public Class<? extends LoginManager> getAClass() {
        return loginManagerClass;
    }
}
```

#### 6.2.4 登录接口示例

```java
@RestController
@RequestMapping("auth")
public class AuthController {

    @Autowired
    private LoginService loginService;

    @PostMapping("login")
    public R<Map<String, String>> login(@RequestBody PwdLoginRequest request) {
        Map<String, String> tokenData = loginService.login(request, MyLoginTypeAdapter.PASSWORD);
        return R.ok(tokenData);
    }

    @PostMapping("refresh")
    public R<Map<String, String>> refresh(@RequestHeader("Authorization") String refreshToken) {
        Map<String, String> tokenData = loginService.refresh(refreshToken);
        return R.ok(tokenData);
    }

    @PostMapping("logout")
    public R<Void> logout() {
        loginService.logout();
        return R.ok();
    }
}
```

#### 6.2.5 权限管理示例

```java
@Autowired
private PermissionManageService permissionManageService;

// 新增权限
Map<String, String> perms = new HashMap<>();
perms.put("sys:user:create", "创建用户");
perms.put("sys:user:delete", "删除用户");
permissionManageService.addPermissions("my-project", "admin", perms);

// 全量替换
Map<String, String> newPerms = new HashMap<>();
newPerms.put("sys:user:list", "查询用户");
newPerms.put("sys:user:create", "创建用户");
permissionManageService.updateRolePermission("my-project", "admin", newPerms);

// 删除权限
permissionManageService.deletePermission("my-project", "admin", "sys:user:delete");

// 清空权限
permissionManageService.clearPermissions("my-project", "editor");
```

#### 6.2.6 实现权限自动加载器

```java
@Component
public class SysRoleMenuPermissionLoader implements PermissionAutoLoader {

    @Autowired
    private SysRoleMenuView sysRoleMenuView;

    @Override
    public Map<String, String> loadPermissions(String roleKey, String projectCode) {
        List<String> perms = sysRoleMenuView.findPermsByRoleKeyAndProjectCode(roleKey, projectCode);
        if (perms == null || perms.isEmpty()) {
            return Map.of();
        }
        return perms.stream()
            .filter(p -> p != null && !p.isEmpty())
            .collect(Collectors.toMap(p -> p, p -> ""));
    }
}
```

---

## 7. 扩展点与自定义方式

### 7.1 自定义登录方式

实现 `LoginManager` 接口（推荐继承 `AbsLoginManager`）：

```java
@Component
public class WeChatLoginManager extends AbsLoginManager {

    @Override
    public boolean support(Object adapter) {
        return adapter instanceof WeChatLoginRequest;
    }

    @Override
    protected AbsUserDetails doLogin(ClientDetails clientDetails, Object adapter) {
        WeChatLoginRequest request = (WeChatLoginRequest) adapter;
        // 调用微信 API 获取用户信息
        // 构建并返回 AbsUserDetails
    }
}
```

### 7.2 自定义登录成功处理

实现 `LoginSucProcess` 接口：

```java
@Component
public class SendLoginNotificationProcess implements LoginSucProcess {

    @Override
    public DefaultKindProcess getProcess() {
        return LoginSucKindProcess.SAVE_INFO; // 或自定义枚举
    }

    @Override
    public void execute(TokenData tokenData) {
        // 发送登录通知、积分奖励等
    }
}
```

### 7.3 自定义登录失败处理

实现 `LoginErrorProcess` 接口（可继承 [`AbsLoginErrorProcess`](simple-common-auth/simple-common-auth-server/src/main/java/com/simple/common/auth/server/common/process/AbsLoginErrorProcess.java)）：

```java
@Component
public class SmsCodeErrorProcess extends AbsLoginErrorProcess {

    @Override
    public DefaultKindProcess getProcess() {
        return LoginErrorKindProcess.IP_ERROR; // 或自定义枚举
    }

    @Override
    public boolean checkErrorNum(ClientDetails clientDetails, Object adapter, String ip) {
        // 校验短信验证码错误次数
    }

    @Override
    public void recordError(ClientDetails clientDetails, Object adapter, String ip) {
        // 记录失败
    }

    @Override
    public void clearError(ClientDetails clientDetails, Object adapter, String ip) {
        // 清除记录
    }
}
```

### 7.4 自定义鉴权处理器

实现 `AuthProcess` 接口：

```java
@Component
public class CustomAuthProcess implements AuthProcess {

    @Override
    public DefaultKindProcess getProcess() {
        // 返回 AuthInterceptorKindProcess 枚举或自定义枚举
    }

    @Override
    public void execute(HttpServletRequest request, HttpServletResponse response,
                        String token, String path, String ipAddr) {
        // 自定义鉴权逻辑
    }
}
```

### 7.5 自定义缓存策略

实现 `CacheManager` 接口，并在 `CacheManagerFactory` 中注册：

```java
public class CustomCacheManager implements CacheManager {
    // 实现所有接口方法
}
```

### 7.6 自定义密钥管理

实现 `UnifiedSecretManager` 接口，从数据库或配置中心加载密钥：

```java
@Component
public class DatabaseUnifiedSecretManager implements UnifiedSecretManager {

    @Autowired
    private ProjectClientRepository repository;

    @Override
    public Map<String, String> getSecrets(String projectCode) {
        ProjectClient client = repository.findByProjectCode(projectCode);
        Map<String, String> secrets = new HashMap<>();
        secrets.put("jwt", client.getJwtSecret());
        secrets.put("sign", client.getSignSecret());
        return secrets;
    }

    @Override
    public String generateJwtSecret() {
        return IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();
    }

    @Override
    public String generateSignSecret() {
        return IdUtil.fastSimpleUUID();
    }
}
```

### 7.7 自定义 Token 实现

实现 `TokenManager` 接口（可继承 `AbsTokenManager`）：

```java
@Component
@Primary
public class CustomTokenManager extends AbsTokenManager {

    @Override
    public String create(Map<String, Object> headers, Map<String, Object> payload) {
        // 自定义 Token 生成逻辑
    }

    @Override
    public Map<String, Object> check(String token, boolean isRefresh) {
        // 自定义 Token 验证逻辑
    }

    @Override
    public void addSecret(String secret) {
        // 自定义密钥管理
    }

    @Override
    public String generateSecret() {
        // 生成密钥
    }
}
```

---

## 8. 注意事项

### 8.1 必填配置

- `simple.auth.project-code` **必须配置**，否则应用启动时抛出 `IllegalStateException`
- 项目编码用于权限隔离，必须与认证中心注册的 `clientId` 一致

### 8.2 缓存类型选择

| 场景 | 推荐缓存类型 | 说明 |
|------|------------|------|
| 微服务架构（多实例） | `REDIS` | 保证多实例缓存一致 |
| 单体应用 | `LOCAL` | 减少 Redis 依赖，提升性能 |
| 混合使用 | 各模块独立配置 | auth/sign/csrf 可分别配置 |

### 8.3 Token 安全

- JWT 密钥长度至少 64 位，否则 `JJwtTokenManager.addSecret()` 抛出异常
- AccessToken 过期返回 code=1000，RefreshToken 过期返回 code=1001
- 前端应根据 code=1000/1001 触发 Token 刷新或跳转登录

### 8.4 签名验证

- `@Sign` 注解仅支持 POST/PUT 请求
- 方法参数只允许 1 个业务参数对象（排除 `HttpServletRequest` 类型）
- 签名验证顺序：**先验签，再防重放**，防止攻击者用错误签名消耗 nonce
- nonce 防重放使用 Caffeine 本地缓存，默认容量 10000

### 8.5 CSRF 防御

- `@CsrfDefense(consume=true)` 为一次性 Token，校验后立即删除
- `@CsrfDefense(consume=false)` 可重复使用，适用于非最终提交的校验场景
- CSRF Token 通过 `GET /csrf/generate?path=xxx` 获取

### 8.6 权限缓存

- 权限缓存 key 格式：`login:auth:{roleKey}:{projectCode}`
- 默认过期时间 24 小时（`permission-cache-expire`）
- 缓存未命中时，若实现了 `PermissionAutoLoader`，会自动从数据库回源加载
- 权限变更通过 EventBus 事件实时同步到所有客户端

### 8.7 内部服务间用户信息透传

[`CheckTokenAuthProcess`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/process/CheckTokenAuthProcess.java:45) 支持通过 `X-User-Context` 和 `X-User-Signature` 请求头直接传递用户信息，绕过 JWT 解析。

> **安全提醒**：此机制需集成方根据自身部署架构实现安全策略，确保仅信任来自可信网关的请求。若未正确实施安全策略而直接使用，存在认证绕过风险。

### 8.8 单点登录

- `ClientAuthInfo.openOneLogin()` 开启后，同一账号只允许同时一人在线
- 新登录会踢掉旧登录（清除旧 Token）
- 登出时发布 `UserLoggedOutEvent` 事件，广播到所有客户端清除本地缓存

### 8.9 环境差异

[`AbsClientAuthConfig`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/config/AbsClientAuthConfig.java:41) 在非 `produce` 环境下自动放行：
- `/webjars/**`、`/v3/**`、`/doc.html` — 接口文档
- `/favicon.ico`
- `/actuator/**` — 监控端点

### 8.10 拦截器顺序

[`AuthClientWebMvcConfig`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/config/AuthClientWebMvcConfig.java:19) 注册两个拦截器：

| 顺序 | 拦截器 | 说明 |
|------|--------|------|
| 1 | [`CookieProcessingHandlerInterceptor`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/interceptor/CookieProcessingHandlerInterceptor.java) | Cookie 处理 |
| 2 | [`AuthHandlerInterceptor`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/interceptor/AuthHandlerInterceptor.java) | 认证鉴权 |

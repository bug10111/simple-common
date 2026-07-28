# simple-common-alibaba 模块文档

> 模块: `simple-common-alibaba`
> 包路径: `com.come.on.alibaba`
> 作者: qty

---

## 1. 模块介绍

`simple-common-alibaba` 是 simple-common 框架中集成阿里巴巴微服务组件的模块，主要提供两大核心能力：

1. **用户级两级限流**：基于 Sentinel 的 `UserRateLimitAspect`，自动拦截所有子服务 Controller 方法，按当前登录用户 `userId` 维度执行接口级 + 全局级两级参数限流。
2. **Feign 客户端配置**：`FeignConfig` 作为统一请求拦截器，在微服务间 Feign 调用时自动透传认证相关的请求头（Token、用户上下文、签名），实现无感知的身份信息传递。

该模块通过 Spring Boot 自动装配机制（`AutoConfiguration.imports`）自动注册，引入依赖即可生效，无需额外配置类。

### 限流优先级体系

```
高  ┌─────────────────────────────────────┐
    │ Gateway 层 (Sentinel gw_flow)       │  ← IP/路由级别限流，由 Gateway 独立处理
    ├─────────────────────────────────────┤
    │ 接口级 : {Controller}:{method}      │  ← 按 userId 维度，逐接口配置
    ├─────────────────────────────────────┤
低  │ 全局   : user:rate:limit             │  ← 按 userId 维度，所有接口合计
    └─────────────────────────────────────┘
```

- **接口级**优先于**全局级**：两者可叠加，Nacos 中配置了哪个就生效哪个。
- **IP 限流**由 Gateway 层 `RealIpExtractionGlobalFilter` + Sentinel `gw_flow` 处理，本模块不涉及。

---

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-alibaba</artifactId>
</dependency>
```

该模块已传递引入以下核心依赖，使用方无需重复声明：

| 依赖 | 说明 |
|------|------|
| `spring-cloud-starter-alibaba-nacos-config` | Nacos 配置中心（动态规则下发） |
| `spring-cloud-starter-bootstrap` | Spring Cloud Bootstrap 支持（加载 bootstrap.yaml） |
| `spring-cloud-starter-alibaba-nacos-discovery` | Nacos 服务发现（支持 `lb://` 路由） |
| `spring-cloud-starter-alibaba-sentinel` | Sentinel 限流核心 |
| `sentinel-datasource-nacos` | Sentinel Nacos 数据源适配器 |
| `spring-cloud-starter-openfeign` | OpenFeign 声明式 HTTP 客户端 |
| `spring-cloud-starter-loadbalancer` | Spring Cloud 负载均衡 |
| `spring-boot-starter-actuator` | Actuator（优雅关机健康检查） |
| `simple-common-auth-client` | 认证客户端（提供 `LoginUserUtils`、`TokenConstant`） |

---

## 3. 核心功能表格

| 核心类 | 类型 | 功能说明 |
|--------|------|----------|
| [`UserRateLimitAspect`](simple-common-alibaba/src/main/java/com/come/on/alibaba/aspect/UserRateLimitAspect.java:33) | AOP 切面 | 自动拦截 `com.simple.*.controller.*.*` 所有 Controller 方法，按 `userId` 执行两级 Sentinel 参数限流 |
| [`FeignConfig`](simple-common-alibaba/src/main/java/com/come/on/alibaba/FeignConfig.java:16) | 配置类 + 请求拦截器 | 实现 `feign.RequestInterceptor`，在 Feign 调用时透传 `Authorization`、`X-User-Context`、`X-User-Signature` 三个请求头 |

---

## 4. 配置说明

### 4.1 自动装配

模块通过 Spring Boot 自动装配机制注册，配置文件位于：

[`simple-common-alibaba/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`](simple-common-alibaba/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1)

```
com.come.on.alibaba.FeignConfig
com.come.on.alibaba.aspect.UserRateLimitAspect
```

引入 Maven 依赖后，`FeignConfig` 和 `UserRateLimitAspect` 自动注册为 Spring Bean，无需手动 `@Import` 或 `@ComponentScan`。

### 4.2 Sentinel Nacos 数据源配置

在 `bootstrap.yaml` 中配置 Sentinel 参数流控规则的数据源：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        param-flow:
          nacos:
            server-addr: ${NACOS_SERVER_ADDR:192.168.200.110:8848}
            data-id: ${spring.application.name}-param-flow-rules
            rule-type: param_flow
```

### 4.3 限流规则配置（Nacos）

在 Nacos 中创建配置项，Data ID 为 `{service-name}-param-flow-rules`，内容为 JSON 数组：

```json
[
  {
    "resource": "OrderController:create",
    "count": 5,
    "grade": 1,
    "paramItem": {
      "index": 0,
      "parseStrategy": 0
    }
  },
  {
    "resource": "user:rate:limit",
    "count": 50,
    "grade": 1,
    "paramItem": {
      "index": 0,
      "parseStrategy": 0
    }
  }
]
```

#### 规则字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `resource` | 是 | 全局：`user:rate:limit`；接口级：`{Controller类名}:{方法名}` |
| `count` | 是 | 单机限流阈值（每个 userId 的 QPS 上限） |
| `grade` | 否 | `1`=QPS（默认），`0`=线程数 |
| `paramItem.index` | 是 | 固定为 `0`（即 `userId`，作为 `SphU.entry()` 的第一个参数） |
| `paramItem.parseStrategy` | 否 | `0`=从参数提取（默认） |

---

## 5. 核心类与接口详细说明

### 5.1 UserRateLimitAspect

**文件**: [`simple-common-alibaba/src/main/java/com/come/on/alibaba/aspect/UserRateLimitAspect.java`](simple-common-alibaba/src/main/java/com/come/on/alibaba/aspect/UserRateLimitAspect.java:33)

**职责**: 统一用户限流切面，自动拦截所有子服务 Controller 方法，按 `userId` 执行两级 Sentinel 参数限流。

#### 核心机制

| 要素 | 说明 |
|------|------|
| 切点表达式 | `@Around("execution(* com.simple.*.controller.*.*(..))")` — 拦截所有 `com.simple.*.controller` 包下的 Controller 方法 |
| 用户身份获取 | `LoginUserUtils.getUserTemporary().getUserId()` — 从认证上下文获取当前登录用户 ID |
| 匿名放行 | `userId` 为空时直接 `pjp.proceed()`，跳过限流 |
| 接口级资源名 | `{Controller类名}:{方法名}`，如 `OrderController:create` |
| 全局资源名 | 常量 `RESOURCE_GLOBAL = "user:rate:limit"` |
| 限流调用 | `SphU.entry(resource, EntryType.IN, 1, userId)` — `userId` 作为参数限流的第 0 个参数 |
| 资源释放 | try-with-resources 自动调用 `Entry.close()` 释放 Sentinel 上下文 |
| 限流异常处理 | 捕获 `BlockException`，记录 warn 日志并抛出 `RuntimeException("请求已被限流，请稍后重试")` |

#### 两级限流执行顺序

```java
// 接口级优先，全局级其次（按优先级排列）
try (Entry e1 = SphU.entry(apiResource, EntryType.IN, 1, userId);
     Entry e2 = SphU.entry(RESOURCE_GLOBAL, EntryType.IN, 1, userId)) {
    return pjp.proceed();
} catch (BlockException e) {
    // 限流处理
}
```

- 先进入接口级资源，再进入全局资源。
- 任一级别触发限流即抛出 `BlockException`，方法不会执行。
- 两者可叠加：接口级阈值 5 QPS + 全局级阈值 50 QPS，表示该用户对此接口最多 5 QPS，对所有接口合计最多 50 QPS。

#### 资源名构建逻辑

```java
private static String buildApiResource(ProceedingJoinPoint pjp) {
    return pjp.getTarget().getClass().getSimpleName() + ":" + pjp.getSignature().getName();
}
```

使用目标类的**简单类名**（非全限定名）+ `:` + 方法名。例如 `ComplaintController` 的 `chat` 方法，资源名为 `ComplaintController:chat`。

### 5.2 FeignConfig

**文件**: [`simple-common-alibaba/src/main/java/com/come/on/alibaba/FeignConfig.java`](simple-common-alibaba/src/main/java/com/come/on/alibaba/FeignConfig.java:16)

**职责**: Feign 请求拦截器，在微服务间 Feign 调用时自动透传认证相关请求头，实现身份信息的无感知传递。

#### 实现方式

`FeignConfig` 实现 `feign.RequestInterceptor` 接口，重写 `apply(RequestTemplate)` 方法。Spring Cloud OpenFeign 会自动将所有 `RequestInterceptor` Bean 应用于每次 Feign 调用。

#### 透传的请求头

| 请求头常量 | 实际 Header 名 | 来源 | 说明 |
|-----------|---------------|------|------|
| [`TokenConstant.Authorization`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/constant/TokenConstant.java:26) | `Authorization` | 当前 HTTP 请求 | Bearer Token，用于下游服务认证 |
| [`TokenConstant.userHead`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/constant/TokenConstant.java:16) | `X-User-Context` | 当前 HTTP 请求 | 用户上下文序列化数据 |
| [`TokenConstant.userSignHead`](simple-common-auth/simple-common-auth-client/src/main/java/com/simple/common/auth/client/common/constant/TokenConstant.java:21) | `X-User-Signature` | 当前 HTTP 请求 | 用户签名，用于防篡改校验 |

#### 透传逻辑

```java
@Override
public void apply(RequestTemplate requestTemplate) {
    ServletRequestAttributes requestAttributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (requestAttributes != null) {
        // 传递 Token
        String token = requestAttributes.getRequest().getHeader(TokenConstant.Authorization);
        if (token != null) {
            requestTemplate.header(TokenConstant.Authorization, token);
        }
        // 传递用户上下文序列化数据
        String encoded = requestAttributes.getRequest().getHeader(TokenConstant.userHead);
        if (encoded != null) {
            requestTemplate.header(TokenConstant.userHead, encoded);
        }
        // 传递签名
        String sign = requestAttributes.getRequest().getHeader(TokenConstant.userSignHead);
        if (sign != null) {
            requestTemplate.header(TokenConstant.userSignHead, sign);
        }
    }
}
```

- 通过 `RequestContextHolder` 获取当前 Servlet 请求上下文。
- 逐个判断请求头是否存在，存在则透传到 Feign 请求模板。
- 当无 Servlet 上下文时（如异步调用），不透传任何头。

---

## 6. 使用示例

### 6.1 限流自动生效（ComplaintController 示例）

以 [`simple-common-test`](simple-common-test/src/main/java/com/simple/common/test/controller/ComplaintController.java:19) 中的 `ComplaintController` 为例：

```java
@Slf4j
@RestController
@RequestMapping("/api/complaint")
public class ComplaintController {

    @PostMapping("/chat")
    public R<Object> chat(@RequestBody AiRequest request) {
        String sessionId = ObjUtil.isEmpty(request.getSessionId())
            ? IdUtils.getFastSimpleUUID() : request.getSessionId();
        String message = request.getUserMessage();

        AiAliBuilder<Object>.AiResult aiResult = AiAliBuilder.builder().sendMsg(sessionId, message);
        if (aiResult.isStructured()) {
            log.info("接收到结构化数据[{}]", aiResult.getMsg());
            return R.ok("感谢你的反馈");
        }
        return R.ok(Map.of("sessionId", sessionId, "reply", aiResult.getMsg()));
    }
}
```

**限流行为说明**：

- `ComplaintController` 位于 `com.simple.common.test.controller` 包下，匹配切点表达式 `com.simple.*.controller.*.*`。
- 当已认证用户调用 `/api/complaint/chat` 时，`UserRateLimitAspect` 自动拦截：
  - 接口级资源名：`ComplaintController:chat`
  - 全局资源名：`user:rate:limit`
- 若 Nacos 中配置了 `ComplaintController:chat` 的参数流控规则（如 count=5），则该用户对此接口每秒最多调用 5 次。
- 若 Nacos 中配置了 `user:rate:limit` 的参数流控规则（如 count=50），则该用户对所有接口合计每秒最多调用 50 次。
- 超出阈值时抛出 `RuntimeException("请求已被限流，请稍后重试")`。

### 6.2 Feign 调用自动透传

在任意服务中通过 `@FeignClient` 调用其他服务时，`FeignConfig` 自动生效：

```java
@FeignClient(name = "order-service")
public interface OrderApiClient {

    @PostMapping("/api/order/create")
    R<String> createOrder(@RequestBody CreateOrderRequest request);
}
```

调用 `createOrder` 时，`FeignConfig.apply()` 自动执行，将当前请求中的 `Authorization`、`X-User-Context`、`X-User-Signature` 透传到下游 `order-service`，下游服务可无缝获取用户身份进行认证和限流。

---

## 7. 扩展点与自定义方式

### 7.1 自定义限流资源名

`UserRateLimitAspect` 中资源名构建逻辑为 `类名:方法名`。若需自定义（如加入模块前缀），可继承或替换该切面：

```java
@Aspect
@Component
public class CustomRateLimitAspect extends UserRateLimitAspect {

    @Override
    protected String buildApiResource(ProceedingJoinPoint pjp) {
        // 自定义资源名格式，如加入模块前缀
        String moduleName = extractModule(pjp);
        return moduleName + ":" + pjp.getTarget().getClass().getSimpleName()
            + ":" + pjp.getSignature().getName();
    }
}
```

> 注意：`buildApiResource` 当前为 `private static` 方法，如需覆盖需将其改为 `protected` 或自行实现完整切面。建议通过 Nacos 规则中的 `resource` 字段与实际资源名保持一致。

### 7.2 扩展 Feign 请求头透传

若需透传额外的请求头（如链路追踪 TraceId），可新增 `RequestInterceptor` 实现：

```java
@Component
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String traceId = attributes.getRequest().getHeader("X-Trace-Id");
            if (traceId != null) {
                requestTemplate.header("X-Trace-Id", traceId);
            }
        }
    }
}
```

多个 `RequestInterceptor` 会按 Spring Bean 注册顺序依次执行。

### 7.3 限流异常自定义处理

当前限流触发时抛出 `RuntimeException`。若需返回统一的 `R` 响应体，可在全局异常处理器中捕获：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRateLimit(RuntimeException e) {
        if ("请求已被限流，请稍后重试".equals(e.getMessage())) {
            return R.fail(429, "请求已被限流，请稍后重试");
        }
        return R.fail(e.getMessage());
    }
}
```

---

## 8. 注意事项

1. **包名特殊**：本模块包路径为 `com.come.on.alibaba`（非 `com.simple.common.alibaba`），这是历史命名，引用时注意路径正确。

2. **切点范围**：`UserRateLimitAspect` 的切点为 `com.simple.*.controller.*.*`，仅拦截符合此包名模式的 Controller。若业务服务包名不同（如 `com.example.controller`），需自定义切面调整切点表达式。

3. **匿名访问不限流**：当 `userId` 为空（匿名访问）时直接放行，不执行限流。匿名访问的限流应由 Gateway 层 IP 限流处理。

4. **Sentinel 规则必须配置**：引入依赖后若 Nacos 中未配置对应的参数流控规则，`SphU.entry()` 不会限流（无规则即放行）。限流效果完全取决于 Nacos 规则配置。

5. **Feign 透传依赖 Servlet 上下文**：`FeignConfig` 通过 `RequestContextHolder` 获取当前请求，在异步线程（如 `@Async`、线程池）中调用 Feign 时，`RequestContextHolder` 默认无法透传。需配合 `RequestContextHolder.setRequestAttributes(attributes, true)` 或自定义 `TaskDecorator` 解决。

6. **两级限流叠加**：接口级和全局级规则可同时配置，两者叠加生效。建议全局级阈值大于单接口阈值，避免全局阈值过小导致接口级规则无法触发。

7. **自动装配**：`FeignConfig` 和 `UserRateLimitAspect` 通过 `AutoConfiguration.imports` 自动注册。若需排除，可在启动类使用 `@SpringBootApplication(exclude = ...)` 或在配置中关闭（需自定义排除逻辑）。

8. **依赖 simple-common-auth-client**：`UserRateLimitAspect` 依赖 `LoginUserUtils` 获取用户身份，`FeignConfig` 依赖 `TokenConstant` 获取请求头常量。使用本模块需确保认证链路正常。

# 用户级别限流接入指南

## 概述

`simple-common-alibaba` 提供统一的 `UserRateLimitAspect`，自动拦截所有子服务 Controller 方法进行**两级 userId 参数限流**。

## 优先级体系

```
高  ┌─────────────────────────────────────┐
    │ Gateway 层 (Sentinel gw_flow)       │  ← IP/路由级别限流，由 Gateway 独立处理
    ├─────────────────────────────────────┤
    │ 接口级 : {Controller}:{method}      │  ← 按 userId 维度，逐接口配置
    ├─────────────────────────────────────┤
低  │ 全局   : user:rate:limit             │  ← 按 userId 维度，所有接口合计
    └─────────────────────────────────────┘
```

## 接入步骤

### 1. 确认依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-alibaba</artifactId>
</dependency>
```

### 2. 配置 Nacos 数据源

`bootstrap.yaml` 添加：

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

### 3. 发布限流规则

Nacos 配置 `{service-name}-param-flow-rules`：

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
### 4. 规则字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `resource` | 是 | 全局：`user:rate:limit`；接口级：`{Controller类名}:{方法名}` |
| `count` | 是 | 单机限流阈值（每个 userId 的 QPS 上限） |
| `grade` | 否 | 1=QPS（默认），0=线程数 |
| `paramItem.index` | 是 | 固定为 0（即 userId） |
| `paramItem.parseStrategy` | 否 | 0=从参数提取（默认） |

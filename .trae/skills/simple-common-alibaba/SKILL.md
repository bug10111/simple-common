---
name: "simple-common-alibaba"
description: "Sentinel 两级限流 + Feign 客户端配置模块。UserRateLimitAspect 自动按用户ID对 Controller 做接口级和全局级限流；FeignConfig 统一提供请求拦截器、编码器等基础配置。当需要接口限流保护或配置 Feign 客户端时使用。"
---

# simple-common-alibaba 认知文档

**Maven**: `simple-common-alibaba`
**包路径**: `com.come.on.alibaba`

## UserRateLimitAspect — Sentinel两级限流

自动拦截所有 Controller 方法，按当前登录用户 `userId` 做两级限流：

| 级别 | 资源名 | 说明 |
|------|--------|------|
| 接口级 | `Controller类名:方法名` | 针对每个接口单独限流 |
| 全局级 | `user:rate:limit` | 用户总请求量限制 |

**注意**：POM中已包含 Sentinel 依赖，无需额外引入。

## FeignConfig — Feign配置

统一提供 Feign 客户端的基础配置（请求拦截器、编码器等）。

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-alibaba</artifactId>
</dependency>
```
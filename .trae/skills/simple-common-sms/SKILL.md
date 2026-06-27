---
name: "simple-common-sms"
description: "短信服务模块（默认阿里云短信）。提供 SmsService 发送验证码、发送模板短信（支持自动获取/手动指定客户端IP）、校验验证码。当需要发送短信验证码或通知时使用。"
---

# simple-common-sms 认知文档

**Maven**: `simple-common-sms`
**包路径**: `com.simple.common.sms`
**默认实现**: `AliSmsService`（阿里云短信）

## SmsService API

```java
@Autowired
private SmsService smsService;

// 发送验证码
smsService.sendCode("13800138000", "123456", "LOGIN");
// 参数：mobile=手机号, code=验证码, sendType=短信类型配置标识

// 发送模板短信（自动获取客户端IP）
smsService.sendTemplateParam("13800138000", "ORDER_NOTIFY", "{\"orderNo\":\"20240101001\"}");
// 参数：mobile=手机号, sendType=类型标识, templateParam=模板参数JSON

// 发送模板短信（手动指定IP）
smsService.sendTemplateParam("13800138000", "ORDER_NOTIFY", "{\"orderNo\":\"20240101001\"}", "192.168.1.1");

// 校验验证码
smsService.checkSms("13800138000", "123456", "LOGIN");
// 校验失败直接抛出异常
```

| 方法 | 参数 | 说明 |
|------|------|------|
| `sendCode` | `String mobile, String code, String sendType` | 发送验证码 |
| `sendTemplateParam` | `String mobile, String sendType, String templateParam, String ip` | 发送模板短信（指定IP） |
| `sendTemplateParam` | `String mobile, String sendType, String templateParam` | 发送模板短信（自动获取IP） |
| `checkSms` | `String mobile, String code, String sendType` | 校验验证码 |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-sms</artifactId>
</dependency>
```
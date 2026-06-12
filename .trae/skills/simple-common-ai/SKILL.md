---
name: "simple-common-ai"
description: "Provides complete API documentation for simple-common-ai module (DashScope AI integration). Invoke when using AiAliBuilder to send conversational messages to the DashScope AI model."
---

# simple-common-ai 认知文档

**Maven**: `simple-common-ai`
**包路径**: `com.simple.common.ai`
**后端**: 阿里云百炼（DashScope）

## AiAliBuilder\<T\> API

```java
// 发送对话消息
AiAliBuilder<MyResponse>.AiResult result = AiAliBuilder.<MyResponse>builder()
    .sendMsg("session-001", "请帮我分析这份数据...");

// 获取AI回复文本
String aiReply = result.getMsg();

// 判断返回是否为结构化JSON
boolean isJson = result.isStructured();

// 将JSON回复转为Java对象
if (result.isStructured()) {
    MyResponse parsed = result.toObj(MyResponse.class);
}
```

**配置类 `AiAliProperties`**（需要配置）：
```yaml
simple:
  ai:
    ali:
      api-key: your-dashscope-api-key
      app-id: your-app-id
      enable-thinking: true   # 是否启用深度思考
```

## AiResult 内部类

| 字段/方法 | 类型 | 说明 |
|-----------|------|------|
| `structured` | `boolean` | 返回内容是否为结构化JSON |
| `msg` | `String` | AI回复文本内容 |
| `toObj(Class<T>)` | `T` | 将回复JSON解析为指定类型对象 |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-ai</artifactId>
</dependency>
```
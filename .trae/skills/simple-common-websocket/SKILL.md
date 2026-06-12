---
name: "simple-common-websocket"
description: "Provides complete API documentation for simple-common-websocket module (Netty WebSocket). Invoke when using @WebSocketListening annotation or ChannelMap for multi-level channel storage."
---

# simple-common-websocket 认知文档

**Maven**: `simple-common-websocket`
**基于**: Netty
**包路径**: `com.simple.common.websocket`

## @WebSocketListening — 消息分发

```java
@Component
public class ChatWebSocketHandler {

    @WebSocketListening(type = "chat", cliKey = "default")
    public void handleChat(ChatMessage message, ChannelHandlerContext ctx) {
        // type="chat": socket类型，区分不同通讯
        // cliKey="default": 区分同类型下不同端的数据
        String reply = chatService.process(message);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(reply));
    }

    @WebSocketListening(type = "notification", cliKey = "mobile")
    public void handleNotification(NotificationMessage message, ChannelHandlerContext ctx) {
        notificationService.push(message);
    }
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `type()` | `String` | 必填 | socket类型，一个类型在同端只能有一个数据通道 |
| `cliKey()` | `String` | `"default"` | 区分相同类型下不同端的数据 |

## ChannelMap\<K, V, T\> — 多级通道存储

**泛型参数**：`K`=主键类型，`V`=子键类型，`T`=值类型（通常是 `Channel`）

```java
ChannelMap<String, String, Channel> channelMap = new ChannelMap<>();

// 添加通道
channelMap.add("userId:123", "mobile", channel);    // K=user, V=设备, T=Channel

// 获取用户所有通道（多端登录）
List<Channel> channels = channelMap.get("userId:123");

// 获取指定设备的通道
Channel channel = channelMap.get("userId:123", "mobile");

// 删除指定设备的通道
channelMap.del("userId:123", "mobile");

// 删除用户所有通道
channelMap.del("userId:123");

// 清理/统计
channelMap.clear();
int size = channelMap.size();   // 总通道数
int keySize = channelMap.keySize();  // 总用户数
```

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-websocket</artifactId>
</dependency>
```
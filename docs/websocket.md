## 8. simple-common-websocket

### 模块介绍

WebSocket实时通讯模块，提供消息监听、通道管理、身份认证等企业级功能。

### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 消息监听注解 | `@WebSocketListening` | 标记消息处理方法 |
| 通道管理 | `ChannelMap<K, V, T>` | 线程安全的通道存储 |
| 消息监听管理 | `WebSocketListeningManager` | 注册和调用消息处理器 |
| 连接认证 | `WebSocketAuthHandler` | WebSocket握手认证 |
| 消息处理 | `WebSocketServerHandler` | WebSocket消息分发 |
| **消息发送工具** | `WebSocketUtils` | **静态工具类，提供发送消息的辅助方法** |

### 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-websocket</artifactId>
    <version>${version}</version>
</dependency>
```

**步骤2：配置WebSocket（必须）**

```yaml
simple:
  websocket:
    # WebSocket服务端口
    port: 8081
    # 路径
    path: /ws
```

**步骤3：实现认证处理器（必须）**

继承 `WebSocketAuthHandler`，定义WebSocket连接认证逻辑：

```java
@Component
public class CustomWebSocketAuthHandler extends WebSocketAuthHandler {
    
    @Override
    protected boolean authenticate(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 从请求头获取token
        String token = request.headers().get("Authorization");
        
        if (StrUtil.isBlank(token)) {
            log.warn("WebSocket连接缺少token");
            return false;
        }
        
        // 验证token
        try {
            Map<String, Object> payload = jwtUtil.verify(token);
            String userId = payload.get("userId").toString();
            
            // 将用户ID存入上下文
            ctx.channel().attr(AttributeKey.valueOf("userId")).set(userId);
            
            log.info("WebSocket认证成功: userId={}", userId);
            return true;
        } catch (Exception e) {
            log.error("WebSocket认证失败", e);
            return false;
        }
    }
}
```

### 使用示例

**1. 定义消息监听器**

```java
@Component
public class ChatMessageListener {
    
    /**
     * 监听聊天消息
     * type: 消息类型标识
     * cliKey: 客户端标识，默认为"default"
     */
    @WebSocketListening(type = "chat", cliKey = "web")
    public String handleChatMessage(WebSocketRequest request) {
        // 解析消息数据
        String messageData = request.getData();
        ChatMessage message = JsonUtils.parse(messageData, ChatMessage.class);
        
        log.info("收到聊天消息: {}", message.getContent());
        
        // 广播给所有在线用户
        broadcastToAll(message);
        
        return "消息已发送";
    }
    
    /**
     * 监听私聊消息
     */
    @WebSocketListening(type = "private_chat")
    public String handlePrivateMessage(WebSocketRequest request) {
        PrivateMessage message = JsonUtils.parse(request.getData(), PrivateMessage.class);
        
        // 发送给指定用户
        sendToUser(message.getToUserId(), message);
        
        return "私聊消息已发送";
    }
}
```

**2. 发送消息（使用WebSocketUtils工具类）**

`WebSocketUtils`是静态工具类，提供便捷的消息发送方法：

```java
@Service
public class NotificationService {
    
    /**
     * 向指定类型的所有客户端广播消息
     */
    public void broadcastToAll(String type, String message) {
        // 向所有 "chat" 类型的客户端发送消息
        WebSocketUtils.sendMsg("chat", message);
    }
    
    /**
     * 向指定客户端发送消息
     */
    public void sendToUser(String type, String userId, String message) {
        // 向指定用户发送消息
        boolean success = WebSocketUtils.sendMsg(type, userId, message);
        
        if (!success) {
            log.warn("用户不在线: {}", userId);
        }
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String type, String userId) {
        return WebSocketUtils.isOnline(type, userId);
    }
    
    /**
     * 获取当前连接总数
     */
    public int getConnectionCount() {
        return WebSocketUtils.getConnectionCount();
    }
}
```

**WebSocketUtils常用方法：**

| 方法 | 说明 | 返回值 |
|------|------|--------|
| `sendMsg(type, msg)` | 向指定类型的所有客户端广播 | void |
| `sendMsg(type, cliKey, msg)` | 向指定客户端发送消息 | boolean |
| `isOnline(type, cliKey)` | 检查客户端是否在线 | boolean |
| `getConnectionCount()` | 获取当前连接总数 | int |
| `cleanInactiveChannels()` | 清理无效通道 | int |
| `clearAll()` | 清空所有通道 | void |

**3. 通道管理**

```java
// 添加通道（用户登录后）
String userId = "user123";
String deviceId = "device_web_001";
Channel channel = ctx.channel();

channelMap.add(userId, deviceId, channel);

// 获取用户的通道
List<Channel> userChannels = channelMap.get(userId);

// 获取指定设备的通道
Channel deviceChannel = channelMap.get(userId, deviceId);

// 删除通道（用户登出时）
channelMap.del(userId, deviceId);

// 删除用户的所有通道
channelMap.del(userId);
```

**4. 多端支持**

通过 `cliKey` 区分不同客户端：

```java
@Component
public class MultiClientListener {
    
    /**
     * Web端消息处理
     */
    @WebSocketListening(type = "notification", cliKey = "web")
    public String handleWebNotification(WebSocketRequest request) {
        // Web端特殊处理
        return "Web端通知";
    }
    
    /**
     * App端消息处理
     */
    @WebSocketListening(type = "notification", cliKey = "app")
    public String handleAppNotification(WebSocketRequest request) {
        // App端特殊处理（如推送）
        pushService.sendPush(request.getData());
        return "App端通知";
    }
    
    /**
     * 小程序端消息处理
     */
    @WebSocketListening(type = "notification", cliKey = "miniprogram")
    public String handleMiniProgramNotification(WebSocketRequest request) {
        return "小程序通知";
    }
}
```

**通道存储结构：**

```
ChannelMap<userId, deviceId, Channel>

userId: "user123"
  ├─ deviceId: "web_001" → Channel(web连接)
  ├─ deviceId: "app_001" → Channel(app连接)
  └─ deviceId: "mp_001"  → Channel(小程序连接)

userId: "user456"
  ├─ deviceId: "web_002" → Channel(web连接)
  └─ deviceId: "app_002" → Channel(app连接)
```

**重要说明：**

1. **type参数**：用于区分不同类型的消息通道，同一type在同一端只能有一个数据通道
2. **cliKey参数**：用于区分相同type下不同客户端的消息处理
3. **方法签名**：监听方法必须接收 `WebSocketRequest` 参数，返回值会发送回客户端
4. **线程安全**：ChannelMap使用读写锁保证复合操作的原子性

---

[返回主文档](../README.md)

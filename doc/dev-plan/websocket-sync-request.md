# WebSocket 同步请求-响应机制 开发计划

## 当前恢复入口

- 当前阶段：阶段三，代码实现
- 当前进行中：[-] 步骤1: 修改 WebSocketRequest 新增 requestId 字段
- 下一步：完成步骤1后继续步骤2，创建 WebSocketSyncManager 接口。

## 整体设计思路

**核心哲学**：基于 Correlation ID + CompletableFuture 模式，将 WebSocket 异步通信转化为同步阻塞调用。
服务端发送请求时生成唯一 requestId，注册 CompletableFuture 到全局 Map，业务线程阻塞等待；
客户端回复时带上相同 requestId，Netty 线程找到 Future 并 complete，唤醒业务线程。

**设计决策记录**：
1. 复用现有 `WebSocketRequest` 协议，新增 `requestId` 字段（null 时行为不变，向后兼容）
2. 使用 `CompletableFuture` 而非 `CountDownLatch`，支持超时和异常处理
3. `WebSocketSyncManager` 定义为接口，支持策略替换
4. `WebSocketUtils` 作为统一入口，API 简洁

## 执行状态清单

- [x] 步骤1: 修改 WebSocketRequest 新增 requestId 字段
- [x] 步骤2: 创建 WebSocketSyncManager 接口
- [x] 步骤3: 创建 DefaultWebSocketSyncManager 实现
- [x] 步骤4: 修改 WebSocketServerHandler 构造函数和消息处理逻辑
- [x] 步骤5: 修改 WebSocketChannelInitializer 传递 SyncManager
- [x] 步骤6: 修改 WebSocketStartInit 注入 SyncManager
- [x] 步骤7: 修改 WebSocketUtils 新增 sendSyncMsg() 方法
- [x] 步骤8: 编译验证：mvn clean package
- [x] 步骤9: 执行 code-inspector 深度自检

## 重要文件索引

| 文件 | 路径 | 改动类型 | 说明 |
|------|------|---------|------|
| WebSocketRequest | simple-common-websocket/src/main/java/com/simple/common/websocket/common/entity/WebSocketRequest.java | 修改 | 新增 requestId 字段 |
| WebSocketSyncManager | simple-common-websocket/src/main/java/com/simple/common/websocket/common/manager/WebSocketSyncManager.java | 新增 | 同步请求管理器接口 |
| DefaultWebSocketSyncManager | simple-common-websocket/src/main/java/com/simple/common/websocket/manager/DefaultWebSocketSyncManager.java | 新增 | 默认实现 |
| WebSocketServerHandler | simple-common-websocket/src/main/java/com/simple/common/websocket/handler/WebSocketServerHandler.java | 修改 | 同步响应检测 |
| WebSocketChannelInitializer | simple-common-websocket/src/main/java/com/simple/common/websocket/initializer/WebSocketChannelInitializer.java | 修改 | 传递 SyncManager |
| WebSocketStartInit | simple-common-websocket/src/main/java/com/simple/common/websocket/init/WebSocketStartInit.java | 修改 | 注入 SyncManager |
| WebSocketUtils | simple-common-websocket/src/main/java/com/simple/common/websocket/utils/WebSocketUtils.java | 修改 | 新增 sendSyncMsg() |

## 编译验证记录

（待执行）

## 深度自检记录

（待执行）
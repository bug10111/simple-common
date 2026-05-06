---
trigger: always_on
---

# simple-common 项目架构与开发约束规则

## 1. 模块依赖与架构约束
- **依赖方向**：服务端模块可以依赖客户端模块，但**客户端模块严禁依赖服务端模块**。
- **自动配置**：每个模块必须在 `resources/META-INF/spring/` 下提供 `org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- **功能复用优先级**：开发时必须优先使用 simple-common 提供的功能（如 `R<T>`, `CacheManager`, `EventBusService`），严禁重新实现通用功能。

## 2. 缓存与事件驱动规范
- **统一缓存入口**：所有缓存操作必须通过 `CacheManager` 接口或 `CacheUtils` 工具类进行，**严禁在业务代码中硬编码 `Caffeine` 或 `RedisTemplate`**。
- **配置化切换**：各模块应通过 `AuthProperties.cacheType` 配置决定使用 Redis 还是本地缓存，业务代码不关心底层实现。
- **事件主动推送**：权限等关键数据变更时，必须通过 `EventBusService` 发布事件并主动推送更新，避免大量客户端同时拉取导致的并发雪崩。

## 3. 组件使用强制约定
- **严禁使用 Spring 原生组件替代**：
  - ❌ 禁止用 `ApplicationEventPublisher` 替代 `EventBusService`。
  - ❌ 禁止用 `RedisTemplate` 替代 `CacheManager`。
  - ✅ 必须使用 `@RabbitMqConsumption` 而非 `@RabbitListener` 进行消息消费。
- **静态工具类识别**：如 WebSocket 模块使用 `WebSocketUtils` 静态类，不得虚构 `WebSocketService`。
- **常见功能映射**：
  - 统一返回结果 → `R<T>` (core)
  - 加密解密 → `CryptoUtil` (core)
  - 分页查询 → `PageUtils` + MP封装 (mp)
  - 事件发布 → `EventBusService.push()` (eventbus)

## 4. 编译与验证要求
- **模块级编译**：修改代码后，必须使用 `mvn clean compile -pl <module-path> -am` 编译受影响模块及其依赖。
- **依赖完整性检查**：确保导入的类和方法存在，且符合 Maven 多模块的依赖传递规则。
- **禁止行为**：严禁编译有错误就停止、忽略 WARNING、假设小改动不需要编译或仅依赖 IDE 提示而不实际编译。

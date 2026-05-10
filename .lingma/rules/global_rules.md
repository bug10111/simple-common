---
trigger: always_on
---

# 全局开发行为准则与代码规范

## 0. 最高优先级：强制使用 simple-common 框架提供的功能

### 0.1 核心原则（必须严格遵守）
**所有操作、代码实现、工具类调用和 API 使用，都必须优先使用 simple-common 框架提供的功能。**

### 0.2 强制要求
- ✅ **必须优先使用** simple-common 中已有的工具类、管理器、服务层组件
- ✅ **必须先查阅** simple-common 框架文档和源码，确认是否有现成实现
- ✅ **必须复用** simple-common 提供的封装方法，严禁重新造轮子
- ❌ **严禁使用** Spring 原生组件替代 simple-common 封装的功能
- ❌ **严禁引入** 第三方库实现 simple-common 已提供的功能
- ❌ **严禁自编** 工具类或方法，除非 simple-common 确实没有提供

### 0.3 典型场景对照表

| 功能需求 | ❌ 禁止使用 | ✅ 必须使用 simple-common |
|---------|------------|--------------------------|
| Base64 编码 | `cn.hutool.core.codec.Base64` | `Base64Utils` |
| 判空检查 | `obj != null` | `ObjUtil.isNotNull(obj)` |
| 事件发布 | `ApplicationEventPublisher` | `EventBusService.push()` |
| 缓存操作 | `RedisTemplate` | `CacheManager` / `CacheUtils` |
| 分布式锁 | `RedissonClient` | `LockService` |
| WebSocket推送 | 虚构 `WebSocketService` | `WebSocketUtils`（静态工具类） |
| RabbitMQ消费 | `@RabbitListener` | `@RabbitMqConsumption` |
| HTTP请求 | `RestTemplate` | `AuthCenterHttpClient` |
| 加密解密 | 自编加密逻辑 | `CryptoUtil` |
| 断言校验 | `if (condition) throw` | `AssertUtils.isTrue()` |
| JSON处理 | `Jackson ObjectMapper` | `JsonUtils` |
| 对象映射 | `BeanUtils.copyProperties` | `BeanUtils.toMap()` / `fillBeanWithMap()` |
| ID生成 | `UUID.randomUUID()` | `IdUtils.getSnowflakeNextIdStr()` |
| 树形结构 | 自编递归算法 | `RecursiveUtils` |
| 分页查询 | 自编分页逻辑 | `PageBase` + MyBatis-Plus |
| 密钥管理 | 自编密钥同步逻辑 | `TokenManager.addSecret()` / `SignManager.addSecret()` |

### 0.4 查证流程（强制执行）
```
遇到功能需求
  ↓
1. 查阅 simple-common 框架文档/记忆
  ↓
2. 搜索项目中是否已有类似实现
  ↓
3. 确认 simple-common 是否提供该功能
  ↓
4. ✅ 有 → 直接使用 simple-common 提供的 API
  ↓
5. ❌ 无 → 向用户确认是否需要扩展 simple-common
```

### 0.5 违规示例（绝对禁止）

❌ **错误 1**：使用 Hutool 的 Base64
```java
// ❌ 错误 - 应该使用 simple-common 的 Base64Utils
String base64 = cn.hutool.core.codec.Base64.encode(data);

// ✅ 正确 - 必须使用 simple-common 提供的工具类
String base64 = Base64Utils.encode(data);
```

❌ **错误 2**：使用 Spring 原生事件
```java
// ❌ 错误 - 禁止使用 ApplicationEventPublisher
@Autowired
private ApplicationEventPublisher publisher;
publisher.publishEvent(event);

// ✅ 正确 - 必须使用 EventBusService
@Autowired
private EventBusService eventBusService;
eventBusService.push(event);
```

❌ **错误 3**：使用原生 Redis
```java
// ❌ 错误 - 禁止直接使用 RedisTemplate
@Autowired
private StringRedisTemplate redisTemplate;
redisTemplate.opsForValue().set(key, value);

// ✅ 正确 - 必须使用 CacheUtils 或 CacheManager
CacheUtils.set(key, value);
```

❌ **错误 4**：自行实现密钥广播
```java
// ❌ 错误 - 不要自己实现密钥广播逻辑
public void broadcastKey(String key) {
    // 自己写 EventBus 发布逻辑...
}

// ✅ 正确 - 使用 TokenManager/SignManager 的统一入口
tokenManager.addSecret(jwtSignSecret);  // Server端自动广播，Client端仅缓存
signManager.addSecret(webSignSecret);
```

### 0.6 重要提醒
1. **simple-common 是第一选择**：任何功能实现前，必须先确认 simple-common 是否已提供
2. **严禁重复造轮子**：simple-common 已提供的功能，绝不允许重新实现
3. **保持架构一致性**：使用 simple-common 确保代码风格、异常处理、日志记录等保持一致
4. **降低维护成本**：统一使用框架提供的功能，便于后续升级和维护
5. **如有疑问先确认**：不确定 simple-common 是否提供某功能时，必须先查阅文档或向用户确认

---

## 1. 核心铁律：严禁推测，必须以实际代码为准
- **完整阅读**：必须系统性地阅读模块中的每一个 Java 文件，严禁仅靠关键词搜索或局部匹配。
- **验证存在**：在文档或代码中引用任何类、方法、注解前，必须先通过 `read_file` 确认其真实存在。
- **架构准确**：严禁臆想架构（如将静态工具类误写为 Service 层）；方法的参数、返回值必须与实际代码完全一致。
- **示例真实**：所有使用示例必须基于真实存在的 API，严禁编造不存在的方法调用。
- **查证流程**：先搜索 -> 再读取 -> 验证存在 -> 检查用法 -> 如有疑问主动确认。

### 1.1 大型任务执行策略
- **严禁敷衍**：面对大型或耗时任务，严禁猜测、模糊搜索或只完成部分工作。
- **拆分执行**：必须将任务拆分为明确的步骤并告知用户，随后把步骤详细添加到代办，最后严谨地按步骤依次执行。
- **自动连续**：在多步骤任务中，除非遇到严重偏差或技术障碍，否则应自动连续执行，无需每步确认。

### 1.2 典型错误案例（绝对禁止）
- ❌ **虚构工具类**：提到 "RedisUtils" → ✅ 实际应使用 `CacheManager` 或 `CacheUtils`。
- ❌ **使用原生组件**：使用 `ApplicationEventPublisher` → ✅ 必须使用 `EventBusService.push()`。
- ❌ **虚构服务层**：虚构 `WebSocketService` → ✅ 实际应使用静态工具类 `WebSocketUtils`。
- ❌ **错误注解**：使用 `@EventListener` → ✅ 实际应使用 `@EventHandler`。
- ❌ **错误消费方式**：使用 `@RabbitListener` → ✅ 必须使用 `@RabbitMqConsumption`。

### 1.3 特别警示
- **绝不假设**：即使看起来“应该有”某个功能，也必须先验证。
- **绝不外推**：不能从一个模块的实现推断另一个模块也有类似实现。
- **绝不复制思维**：不能因为其他框架有某工具类，就认为本项目也有。
- **必须自问**：每次提到具体类名时，都要问自己：“我真的看到过这个代码吗？”

## 2. 代码注释与署名规范
- **禁止 AI 痕迹**：代码注释中严禁出现“优化”、“修复版本”等暗示 AI 生成过程的词汇。
- **统一作者署名**：所有 Java 文件的 `@author` 标签后**必须**统一使用 "qty"，不得添加任何后缀。
- **禁改代码逻辑**：在进行注释完善、文档编写任务时，**严禁**修改代码逻辑、接口定义，只允许修改注释和文档内容。
- **Javadoc 规范**：接口注释**必须**包含 `@param` 和 `@return` 说明。

## 3. 代码开发与编译强制流程
- **先阅读后修改**：通读当前模块相关文件，理解架构模式和依赖关系。
- **强制编译验证**：任何代码修改完成后，**必须**执行 `mvn clean compile` 并确保 `BUILD SUCCESS`。
- **合理性审查**：检查逻辑闭环、代码复用优先级及架构一致性。

### 3.1 编译错误处理流程
```
编译失败
  ↓
查看错误信息
  ↓
定位问题代码
  ↓
修复错误
  ↓
重新编译
  ↓
重复直到 BUILD SUCCESS
```

## 4. 文档编写严禁猜想规范
- **严禁列出不存在的功能**：文档中出现的所有类、接口、配置项**必须**在代码中真实存在。
- **覆盖自定义注解与接口**：文档**必须**包含所有自定义注解（Annotation）和封装接口的详细说明及使用示例。
- **示例代码可运行**：所有示例代码**必须**基于真实的 API，确保用户可以直接复制使用。
- **结构规范**：**必须**按模块介绍、核心功能表格、继承实现方式、扩展举例、使用示例的顺序组织。

## 5. @HasAuthority 权限字段命名规范
- **转换规则**：类级别 `@RequestMapping` + 方法级别路径，将 `/` 替换为 `:`，排除 PathVariable 参数
- **示例**：`@RequestMapping("sys/department")` + `@GetMapping("tree")` → `sys:department:tree`
- **强制要求**：所有权限标识必须严格按此规则生成

## 6. 代码质量优先级：线程安全 > 高性能 > 低内存

### 6.1 线程安全（最高优先级）
- **核心原则**：根据使用场景判断，确保实际使用时不会出现线程安全问题
- **共享资源**：多线程访问的共享变量必须使用同步机制（synchronized、Lock、ConcurrentHashMap等）
- **局部变量**：方法内部的局部变量天然线程安全，可使用非线程安全类（如 StringBuilder、ArrayList）
- **不可变对象**：优先使用 final 修饰符和不可变对象（如 String）
- **数据库操作**：必须使用 `@Transactional` 保证数据一致性
- **竞态条件**：检查-执行（check-then-act）操作必须加锁或使用原子类

### 6.2 高性能（第二优先级）
- **数据库层面优先**：使用LIMIT、索引、批量查询，避免N+1和全表扫描
- **算法优化**：选择合适数据结构，降低时间复杂度
- **内存控制**：避免一次性加载大量数据，及时释放资源

### 6.3 取舍原则
```java
// ✅ 正确：共享缓存使用 ConcurrentHashMap（多线程访问）
private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

// ✅ 正确：方法内部局部变量使用 StringBuilder（天然线程安全）
public String buildMessage() {
    StringBuilder sb = new StringBuilder(); // 局部变量，无需线程安全
    sb.append("Hello").append(" World");
    return sb.toString();
}

// ✅ 正确：SQL层面排序而非内存遍历（高性能）
queryWrapper.eq(Department::getParentId, parentId)
            .orderByDesc(Department::getSerial).last("LIMIT 1");

// ❌ 错误：共享变量使用 ArrayList（多线程环境会出问题）
private List<String> sharedList = new ArrayList<>(); // 应该用 CopyOnWriteArrayList
```

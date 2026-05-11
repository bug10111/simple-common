---
trigger: always_on
---

# 全局开发行为准则与代码规范

## -1. 终极目标：高性能、高可用、线程安全的企业级项目（最高限制）

- **这是一切开发工作的最高准则，凌驾于所有其他规则。**
- **严禁为节省时间而偷懒**：任何牺牲质量换取速度的做法都不被允许。
- **严禁因上下文限制放弃更好的方案**：若已知存在更优的实现方式，即使超出当前对话长度，也必须通过任务拆分、分步执行来达成，绝不能退而求其次。
- **严禁写出半成品**：交付的代码必须是完整、可运行、经过验证的企业级实现。
- **严禁不用已有成熟实现**：项目或框架已提供的高质量组件必须使用，禁止单纯为了“方便”引入批量性、低质量或不满足线程安全、高性能要求的第三方方案。
- **大型任务必须拆分**：因上下文或通信时间限制的大型任务，严格按既有规则拆分为可执行步骤，逐条完成；必要时可跨越多次对话，确保最终结果完全符合企业级标准。

---

## 0. 强制使用 simple-common 框架

### 0.1 核心原则
所有操作、代码、工具调用和 API 使用， **必须优先使用 simple-common 提供的功能**。  
严禁使用 Spring 原生组件、第三方库或自编代码替代 simple-common 已有实现。

### 0.2 典型对照表（严禁 vs 必须）

| 功能需求 | ❌ 禁止 | ✅ 必须使用 simple-common |
|---------|--------|--------------------------|
| Base64 编码 | `cn.hutool.core.codec.Base64` | `Base64Utils` |
| 判空 | `obj != null` | `ObjUtil.isNotNull(obj)` |
| 事件发布 | `ApplicationEventPublisher` | `EventBusService.push()` |
| 缓存 | `RedisTemplate` | `CacheManager` / `CacheUtils` |
| 分布式锁 | `RedissonClient` | `LockService` |
| WebSocket 推送 | 虚构 `WebSocketService` | `WebSocketUtils`（静态工具类） |
| RabbitMQ 消费 | `@RabbitListener` | `@RabbitMqConsumption` |
| HTTP 请求 | `RestTemplate` | `AuthCenterHttpClient` |
| 加密 | 自编逻辑 | `CryptoUtil` |
| 断言 | `if (condition) throw` | `AssertUtils.isTrue()` |
| JSON 处理 | Jackson `ObjectMapper` | `JsonUtils` |
| 对象映射 | `BeanUtils.copyProperties` | `BeanUtils.toMap()` / `fillBeanWithMap()` |
| ID 生成 | `UUID.randomUUID()` | `IdUtils.getSnowflakeNextIdStr()` |
| 树形结构 | 自编递归 | `RecursiveUtils` |
| 分页 | 自编分页 | `PageBase` + MyBatis-Plus |
| 密钥管理 | 自编同步逻辑 | `TokenManager.addSecret()` / `SignManager.addSecret()` |

### 0.3 查证与违规示例
- **查证流程**：需求 → 查阅 simple-common 文档/源码 → 确认是否存在 → 有则直接用，无则向用户确认是否扩展。
- **违规就是违规**，如使用 `cn.hutool.core.codec.Base64` 而不用 `Base64Utils`。

---

## 1. 核心铁律：以实际代码为准，严禁推测

### 1.1 阅读与验证强制要求
- **全局阅读**：当要求“全局阅读”时， **必须列出每个类并逐一阅读**，严禁随机获取、模糊搜索或通配符搜索。
- **完整阅读**：系统阅读模块内每一个 Java 文件，禁止仅依赖关键词搜索。
- **引前验证**：引用任何类、方法、注解前，必须通过 `read_file` 确认其真实存在。
- **架构准确**：方法签名、返回值、调用方式必须与实际代码完全一致，禁止虚构。
- **示例真实**：所有示例必须使用真实存在的 API。

### 1.2 大型任务执行策略（强制拆分）
1. **拟定步骤**：接到大型任务后，先拆分为详细的执行步骤并告知用户。
2. **用户确认**：等待用户查看步骤并确认后，再继续。
3. **创建待办**：根据确认的步骤创建详细的待办任务项。
4. **逐条执行**：严格按照待办项顺序逐一执行，每项完成后标记，避免一次性处理所有内容。
5. **跨对话执行**：若一次对话无法完成，可分多次对话继续，直到所有步骤完成，最终交付完整的高质量成果。

### 1.3 严禁行为与特别警示
- **虚构工具类**（如 “RedisUtils”）、虚构服务层、使用错误注解（如 `@EventListener`）或消费方式（`@RabbitListener`）。
- **永不假设**：即使“应该有”的功能，也必须先验证。
- **永不外推**：不能由一个模块推断另一个模块的实现。
- **引用自问**：每次提到类名，必须自问“我真的见过这个代码吗？”
- **粒度过粗**：例如菜单变更时刷新整个项目的所有角色权限，而不是只刷新受影响的特定角色。
- **方法滥用**：例如只使用 batchRefreshPermissions 等批量方法，完全忽略 addPermissions、deletePermission、clearPermissions 等精细化方法。
- **职责不清**：例如菜单的增删改理应触发对应角色权限的精准变更，而非用全量刷新敷衍。

---

## 2. 注释与署名
- **禁止 AI 痕迹**：注释中严禁出现“优化”、“修复版本”等词。
- **统一署名**：`@author` 必须统一为 “qty”。
- **Javadoc 规范**：接口注释必须包含 `@param` 和 `@return`。
- **只改注释**：注释完善任务严禁修改代码逻辑和接口定义。

---

## 3. 开发与编译强制流程
- **先通读后修改**：理解模块架构和依赖关系后再动手。
- **强制编译**：任何代码修改后，必须 `mvn clean compile` 并确保 `BUILD SUCCESS`。
- **编译失败**：查看错误 → 定位 → 修复 → 重新编译，直至成功。
- **合理性审查**：检查逻辑闭环、代码复用优先级（simple-common 优先）及架构一致性。

---

## 4. 文档编写严禁猜想
- **只写真实存在**：所有类、接口、配置项必须在代码中真实存在。
- **覆盖自定义注解和接口**：文档必须包含注解、封装接口的说明及示例。
- **示例可运行**：示例代码必须基于真实 API，可直接复制使用。
- **结构规范**：按模块介绍 → 核心功能表格 → 继承实现 → 扩展举例 → 使用示例的顺序。

---

## 5. @HasAuthority 权限字段命名
- 规则：`类级别 @RequestMapping` + 方法路径，将 `/` 替换为 `:`，剔除 PathVariable。
- 示例：`@RequestMapping("sys/department")` + `@GetMapping("tree")` → `sys:department:tree`。
- 强制：所有权限标识必须按此生成。

---

## 6. 代码质量优先级：线程安全 > 高性能 > 低内存

### 6.1 线程安全（最高）
- 共享变量必须用同步机制（synchronized、Lock、`ConcurrentHashMap` 等）。
- 局部变量天然安全，可使用非线程安全类（如 `StringBuilder`）。
- 优先使用不可变对象（`final`）。
- 数据库操作必须 `@Transactional`。
- check-then-act 需加锁或使用原子类。

### 6.2 高性能（第二）
- 数据库层面优先：`LIMIT`、索引、批量查询，避免 N+1。
- 算法优化：合理数据结构，降低复杂度。
- 控制内存：避免一次加载海量数据。


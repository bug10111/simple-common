---
name: "bug-fix"
description: "Bug修复技能：排查问题时必须追溯完整数据链路定位根因，严禁通过if-else/switch/三元运算符/Optional.orElse等条件判断做兼容性兜底。修复根因后移除所有临时兼容代码。Invoke when user reports a bug or asks to fix an issue."
---

# Bug 修复技能（Bug Fix）

## 核心原则

**绝对禁止使用 `if-else`、`switch`、三元运算符、`Objects.requireNonNullElse`、`Optional.orElse` 等任何条件判断手段，来应对字段缺失、数据为空、结构变化、版本差异、接口演变、类型不匹配等兼容性问题。**

所有兼容性需求必须通过**补全数据链路、统一数据清洗层、设计模式（适配器/防腐层）、配置化映射**等结构化方式解决。

---

## 适用范围

| 场景 | 禁止做法 |
|------|---------|
| 数据库字段缺失 | `if (entity.getField() == null) setDefault()` |
| DTO/VO 字段缺失 | `if (dto.getXxx() == null) dto.setXxx(...)` |
| 外部接口字段变更 | `if (response.hasField(...)) else ...` |
| 旧版本数据兼容 | `if (version < V2) doOldWay() else doNewWay()` |
| 枚举值不在范围内 | `if (code == null) setDefaultEnum()` |
| 类型转换失败 | `if (obj instanceof A) ... else ...` 做兜底 |
| 多态扩展 | `if (type == 1)` 来实例化不同实现 |

---

## 工作流程

### 第一步：追溯完整数据链路

从 Bug 报错点开始，沿调用链向上追溯，绘制完整数据流：

```
Controller → Service → CopyMapper → DTO → View → Repository → Mapper XML → DB
```

每一步明确记录：
- 输入数据是什么
- 经过什么转换
- 哪些字段在哪个环节丢失/变空

### 第二步：定位根因节点

在数据链路中定位**最早出现问题的节点**：

- 是否在 CopyMapper 中遗漏了 `@Mapping` 注解？
- 是否在 DTO 中缺少对应字段？
- 是否在 Mapper XML 中遗漏了字段？
- 是否在 Service 层某个转换方法中未处理该字段？

**核心原则：根因在哪里，就在哪里修复，不上移做兼容。**

### 第三步：结构性修复

根据根因类型选择修复方式：

#### 3.1 数据库层 → 修改表结构

```sql
-- ❌ 错误：代码中 if (user.getNickname() == null) user.setNickname("匿名")
-- ✅ 正确：
ALTER TABLE user ADD COLUMN nickname VARCHAR(64) DEFAULT '匿名' NOT NULL;
```

同步实体类注解。

#### 3.2 Mapper/Converter 层 → 补充映射关系

```java
// ❌ 错误：在 Service 中写 if (order.getSourceCode() != null) dto.setDictValue(...)
// ✅ 正确：在 MapStruct CopyMapper 中补充映射
@Mapping(source = "sourceCode", target = "dictValue")
UpdateSysDictDataRequest toOldUpdateRequest(UpdateDataSourceRequest request);
```

同时在目标 DTO 中补充对应字段。

#### 3.3 外部接口/第三方数据 → 使用防腐层

```java
// ❌ 错误：业务代码中 if (externalResp.getPhone() == null) phone = "000-0000"
// ✅ 正确：封装 ExternalUserAdapter，在 toDomain() 集中处理
// 业务层直接取 domain.getPhone()，不出现 if-else
```

#### 3.4 版本兼容 → 使用版本化接口或适配器模式

```java
// ❌ 错误：if (apiVersion >= 2) { parseNewField(); } else { parseOldField(); }
// ✅ 正确：独立 Controller / Service 实现
//   /api/v1/order → OrderV1Handler
//   /api/v2/order → OrderV2Handler
```

#### 3.5 枚举值扩展 → 保证枚举字段永不为空

```java
// ❌ 错误：if (status == OrderStatus.PAID) { ... } else if (status == null) { ... }
// ✅ 正确：DB 和代码保证枚举字段 NOT NULL，新状态同步新增枚举值
// 反序列化未知值时抛出明确异常，不静默替换
```

#### 3.6 多态行为 → 使用策略模式

```java
// ❌ 错误：if ("WECHAT".equals(payType)) { ... } else if ("ALIPAY".equals(payType)) { ... }
// ✅ 正确：PayServiceFactory.get(payType).pay();
// 工厂内用 Map 存储策略实例，无需条件判断
```

#### 3.7 空值处理 → 从源头保证非空

```java
// ❌ 错误：if (order == null) return new Order();
// ✅ 正确：方法签名用 Optional<Order> 或 @NotNull 注解
// 或从源头保证对象永不为空（构造时初始化、数据库约束）
```

### 第四步：移除临时兼容代码

根因修复后，**必须移除之前做的所有临时兼容代码**：

- 删除 `if (x == null) setDefault()` 类代码
- 删除 `if (x != null) doSomething()` 类代码
- 删除 null 回填、条件绕过等防御性代码

### 第五步：编译验证

```bash
mvn clean compile
```

---

## 正确做法的核心思想

- **结构性修复**：问题出在哪个层级，就在该层级补充完整，而不是在上层用条件跳过。
- **数据清洗层**：如果确实需要默认值或数据转换，集中在一个防腐层/数据清洗层处理，业务层完全干净。
- **设计模式**：策略模式、适配器模式、工厂模式、装饰器模式等替代条件判断。
- **类型系统与约束**：利用非空类型、数据库约束、`Optional` 等语言特性消除空值条件判断。

---

## 执行检查清单（排查/修复 Bug 时逐条核对）

- [ ] 是否已沿调用链向上追溯完整数据链路？
- [ ] 是否已定位到最早出现问题的根因节点？
- [ ] 修复方案是否在根因节点直接修复，而非在上层做兼容？
- [ ] 是否存在任何 `if (x == null)`、`if (x != null)` 等条件判断用于数据补全或类型兼容？
- [ ] 是否存在 `switch` 根据类型码/版本号/枚举值来执行不同逻辑？
- [ ] 是否存在三元运算符 `condition ? value1 : value2` 用于提供默认值？
- [ ] 是否存在 `Objects.requireNonNullElse`、`Optional.orElse`、`Optional.orElseGet` 用于替代缺失值？
- [ ] 如果存在上述任何一种，是否可以用结构性修复替代？
- [ ] 根因修复后，是否已移除所有临时兼容代码？
- [ ] 编译是否通过？

---

## 例外条款（极其严格）

仅在以下情况下允许使用单次、集中的条件判断：

- 调用**第三方二进制库**，其返回数据结构不可控且无文档，必须用 `instanceof` 或 `null` 检测才能安全调用。
- **反射/动态代理**场景下判断字段是否存在（但应优先使用 `@JsonIgnoreProperties(ignoreUnknown=true)` 等配置）。

即使满足例外，该判断必须**封装在独立的基础设施方法中**，不污染业务逻辑。

---

## 示例对比表

| 场景 | ❌ 错误做法（条件兼容） | ✅ 正确做法（结构化修复） |
|------|------------------------|---------------------------|
| 数据库某列可能为 null | `if (user.getAge() == null) user.setAge(0);` | `ALTER TABLE user MODIFY age INT NOT NULL DEFAULT 0;` |
| MapStruct 漏映射字段 | `if (dto.getDictValue() == null) dto.setDictValue(entity.getDictValue());` | `@Mapping(source = "sourceCode", target = "dictValue")` + DTO 补字段 |
| 外部接口缺少电话号码 | `if (resp.getPhone() == null) phone = "000-0000";` | 在 `ExternalUserAdapter` 的 `toDomain()` 中集中填充 |
| API 版本 v1 和 v2 结构不同 | `if (version == 1) { parseV1(); } else { parseV2(); }` | 独立 Controller：`/api/v1/order` 和 `/api/v2/order` |
| 支付渠道不同逻辑 | `if (type.equals("wx")) ... else if (type.equals("zfb")) ...` | `PaymentStrategy` 接口 + 工厂注入 Map |
| 枚举反序列化未知值 | `if (OrderStatus.fromCode(code) == null) return OrderStatus.UNKNOWN;` | 反序列化器中抛出异常，不静默替换 |

---

## 铁律

1. **追溯根因**：Bug 排查必须沿数据链路向上追溯，定位最早出问题的节点。
2. **禁止打补丁**：严禁在任何层级使用 `if-else`/`switch`/三元运算符做兼容性兜底。
3. **向下修复**：问题出在哪个层级就在哪个层级修复，不上移。
4. **向上干净**：根因修复后，上层代码不出现任何防御性条件判断。
5. **移除兼容代码**：根因修复后，所有临时兼容代码必须删除。
6. **编译验证**：修复后必须 `mvn clean compile` 通过。
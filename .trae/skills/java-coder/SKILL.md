---
name: "java-coder"
description: "代码实现与编译技能。按计划逐步编码：创建 DTO/View/Repository/Mapper XML/Service/Controller，强制遵守链式调用规范（禁止超2个.）、注释规范（Javadoc+方法内换行注释）、SQL规范（<where>标签/防注入/禁止SELECT */DATE_FORMAT）、数据安全规范（用户ID/时间后台赋值）、排序默认值逻辑、View层单参数查询。最后 mvn clean compile 强制编译通过。当用户要求开始编码或执行开发计划时调用。"
---

# 代码实现与编译技能（Java Coder）

## 触发条件

需求分析完成、开发计划已输出后，用户确认开始编码时调用本技能。承接 `requirement-planner` 的计划输出。

---

## 前置条件

必须先完成 `requirement-planner` 的输出，拿到：
- 开发计划步骤列表
- 业务流程图
- 涉及模块及改动类型

---

## 工作流程（严格按序执行）

### 第零步：验证计划可执行性（编码前必做）

在写任何一行代码之前，必须验证：

```
验证清单：
□ 需要新建的 DTO 类 → 确认目录存在，不存在则先 mkdir
□ 需要修改的接口/类 → read_file 确认文件存在
□ 需要的依赖类（如 PageBase、AssertUtils）→ 确认 import 路径正确
□ 需要的 Mapper XML 目录存在
□ 需要引用的 View/Repository 方法已存在
```

**验证不通过** → 报告无法执行的原因，要求修正计划。

---

### 第一步：阅读框架认知文档

**必须先找技能**（如果存在）：

```
必须先找 simple-common 相关技能
```

1. 列出 docs 目录下文件
2. 读取索引文件，了解框架能力
3. 确认本次编码需要用到的框架工具类/方法

**必须优先使用 simple-common 框架**：
- 工具类（BeanUtils、AssertUtils、Base64Utils 等）
- 基础类（PageBase、BaseEntity 等）
- 异常处理
- 权限控制

若未使用框架而用了第三方库或自写代码，在编译报告中注明原因。

---

### 第二步：按计划逐步编码

按照开发计划步骤，**严格逐步骤执行**，每完成一步标记完成。

#### 2.1 创建 DTO（Request/Response）

- 遵循命名规范：`{操作}{实体}Request` / `{操作}{实体}Response`
- 分页 Request 必须继承 `PageBase`
- 字段命名遵循 §10.3 规范
- 使用 `@Data` 注解（Lombok）
- 禁止链式 setter，每个字段独立一行赋值

#### 2.2 创建/修改 View 接口与实现

- 接口命名：`{实体}View`
- 实现命名：`MP{实体}View`
- 方法命名遵循 §10.2 规范
- 接口必须有标准 Javadoc（@param、@return）

#### 2.3 创建/修改 Repository 接口

- 接口命名：`{实体}Repository`
- 方法参数直接使用 DTO，禁止拆解
- 分页方法使用 `IPage<T>` 返回

#### 2.4 编写/修改 Mapper XML

- 文件位置：`src/main/resources/mapper/`
- 使用 `<where>` 标签，禁止 `WHERE 1=1`
- 禁止 `SELECT *`，明确列出字段
- 列表查询必须有 `LIMIT`
- 禁止 `DATE_FORMAT` 函数
- 通过 JOIN 获取外键对应的名称
- 参数使用 `#{}`，禁止 `${}`（防注入）

#### 2.5 创建/修改 Service 接口与实现

- 接口命名：`{业务模块}Service`
- 实现命名：`Default{业务模块}Service`
- 响应组装遵循 §12.2 规范：
  - 基础字段用 `BeanUtils.copyProperties` 或 `copy.toXxxResponse()`
  - 关联字段手动 set ≤2 个，≥3 个用专用 CopyMapper，不存在的时候自行创建
- 数据库操作加 `@Transactional`

#### 2.6 创建/修改 Controller

- 命名：`{业务模块}Controller`
- 权限标识按 §7 规则生成：`类路径:方法路径`
- 接口必须有标准 Javadoc

---

### 第三步：编码规范强制执行

#### 3.1 链式调用规范

**禁止业务逻辑中出现超过 2 个 `.` 的链式调用**。排除项：Builder 模式、Stream 终止操作、Optional 判空链。

```java
// 错误：超过 2 级链式
String balance = request.getUser().getAccount().getBalance();

// 正确：逐层判空分步承接
User user = request.getUser();
AssertUtils.notEmpty(user, "用户信息为空");
Account account = user.getAccount();
AssertUtils.notEmpty(account, "账户信息为空");
String balance = account.getBalance();
```

```java
// 错误：链式 setter
BeanUtils.copyProperties(a, b).setXxx(value);

// 正确：分步操作
BeanUtils.copyProperties(a, b);
b.setXxx(value);
```

#### 3.2 注释规范

- 接口方法必须有标准 Javadoc（`@param`、`@return`）
- 方法内核心步骤注释**必须换行后标注在代码上方**，不放在行尾
- 禁止出现序号（"步骤1"、"1."等）

```java
/**
 * 分页查询租户信息
 *
 * @param pageRequest 分页查询请求
 * @return 租户分页数据
 */
@Override
public IPage<Tenant> findAll(PageTenantRequest pageRequest) {
    
    // 构建分页对象
    Page<Tenant> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());

    // 执行分页查询
    IPage<Tenant> result = tenantRepository.selectPage(page, pageRequest);

    return result;
}
```

#### 3.3 日志规范

- 统一使用 `@Slf4j` 注解
- 敏感信息（密码、token、密钥）使用 `log.debug`
- 关键业务节点使用 `log.info`

---

#### 3.4 View 层查询方法调用规范

**所有 View 层的 `findAll`/`findOne` 必须使用单参数重载，禁止传 `null` 作为 `neRequest`。**

```java
// 错误：第二个参数传 null
List<TagGroupConfig> configs = tagGroupConfigView.findAll(
        new FindAllTagGroupConfigRequest().setTagGroupId(id), null);
TagGroup existsGroup = tagGroupView.findOne(queryReq, null);

// 正确：使用单参数重载（内部自动创建空 neRequest 对象）
List<TagGroupConfig> configs = tagGroupConfigView.findAll(
        new FindAllTagGroupConfigRequest().setTagGroupId(id));
TagGroup existsGroup = tagGroupView.findOne(queryReq);
```

**原因**：`null` 作为 `neRequest` 传入双参数方法时，MyBatis-Plus 的 `.ne()` 条件会调用 `null.getId()` 等，触发 NPE。所有 View 接口均已提供 `default` 单参数重载，内部创建空对象安全替代。

每写一个方法，立即检查：

```
□ 用户ID、租户ID 是否从 SecurityContext/Token 获取？不可从请求参数传入。
□ createTime/updateTime 是否用 new Date() 后台赋值？不可从前端传入。
□ createUserId/updateUserId 是否从上下文获取？
□ 排序字段 sort：非必填时是否自动取最大值 +1？
□ 枚举值是否校验合法性？
□ Request → Entity 转换是否有字段遗漏？
```

**排序字段处理模板**：
```java
if (request.getSort() == null) {
    
    // 获取同组最大排序值
    Integer maxSort = xxxView.findMaxSort(request.getGroupId());
    
    // 若组内无数据则从 1 开始，否则加 1
    request.setSort(maxSort == null ? 1 : maxSort + 1);
}
```

#### 3.5 批量写入规范

**循环内禁止逐条 `save()`**。必须收集到 List 后调用 `saves()` 批量插入。

```java
// 错误：循环内逐条插入（N+1 INSERT）
for (CreateChildRequest req : request.getChildren()) {
    Child child = new Child();
    BeanUtils.copyProperties(req, child);
    childView.save(child);
}

// 正确：收集后批量插入（1 条 INSERT）
List<Child> children = new ArrayList<>();
for (CreateChildRequest req : request.getChildren()) {
    Child child = new Child();
    BeanUtils.copyProperties(req, child);
    children.add(child);
}
childView.saves(children);
```

#### 3.6 覆盖更新规范（先删后增）

**一对多关系更新场景**：子表必须先按外键删除旧数据，再批量插入新数据。禁止直接追加插入。

```java
// 错误：更新时直接追加，旧数据不清理 → 数据翻倍
for (ChildRequest req : request.getChildren()) {
    childView.save(req);  // 每次更新都追加，旧数据还在
}

// 正确：先删后增
// 删除该父记录下所有旧子数据
DeleteChildRequest deleteReq = new DeleteChildRequest();
deleteReq.setParentId(parentId);
childView.delete(deleteReq);

// 批量插入新子数据
List<Child> children = new ArrayList<>();
for (ChildRequest req : request.getChildren()) {
    Child child = new Child();
    child.setParentId(parentId);
    BeanUtils.copyProperties(req, child);
    children.add(child);
}
childView.saves(children);
```

**适用场景**：标签→控件、分组→配置项、订单→订单明细等所有一对多关系的更新接口。

---

### 第五步：编译验证

**代码全部写完后立即执行**：

```bash
mvn clean compile
```

#### 编译失败处理流程：

```
编译失败
  ↓
查看错误信息
  ↓
定位错误文件 + 行号
  ↓
read_file 确认代码
  ↓
修复
  ↓
重新 mvn clean compile
  ↓
直到 BUILD SUCCESS
```

**严禁**：编译失败后不修复直接进入下一步。

---

### 第六步：修改清单输出

编译通过后输出：

```
═══════════════════════════════════════════
  代码实现完成
═══════════════════════════════════════════

修改类及代码行数：
- com.example.XxxRequest | +30（新建）
- com.example.XxxResponse | +25（新建）
- com.example.XxxView | +3
- com.example.MPXxxView | +15
- com.example.XxxRepository | +3
- mapper/XxxDao.xml | +25
- com.example.XxxService | +3
- com.example.DefaultXxxService | +20
- com.example.XxxController | +15

编译结果：✅ BUILD SUCCESS / ❌ BUILD FAILURE

{若未使用 simple-common，此处附加原因说明}

═══════════════════════════════════════════
```

---

## 铁律

1. **验证优先**：编码前必须验证计划可执行性。
2. **框架优先**：必须先获取 simple-common 相关技能，优先使用其功能。
3. **注释铁律**：Javadoc 完备 + 方法内注释换行标注在代码上方。
4. **链式铁律**：业务逻辑禁止超过 2 个 `.` 链式调用。
5. **数据铁律**：用户ID/租户ID/时间等必须后台获取，不能信任前端。
6. **排序铁律**：sort 非必填时必须自动取最大值 +1。
7. **编译铁律**：必须 `mvn clean compile` 通过且 `BUILD SUCCESS`。
8. **Sql 铁律**：必须 `@Slf4j` 日志记录。
9. **View 查询铁律**：`findAll`/`findOne` 必须使用单参数重载，禁止传 `null` 作为 `neRequest`。
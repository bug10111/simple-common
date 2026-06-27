---
name: "functional-tester"
description: "功能测试技能：完整阅读控制层及相关代码，绘制调用链，生成 .http 文件用于回归测试，按真实场景构造测试数据，验证增删改链路闭环。Invoke when user asks to test a feature/controller or wants to generate .http test files for regression testing."
---

# 功能测试技能（Functional Tester）

## 触发条件

用户要求对某个功能模块或 Controller 进行功能测试、回归测试、生成 `.http` 测试文件时调用本技能。

---

## 工作流程（严格按序执行）

### 第一步：代码全面阅读

1. **定位 Controller**
   - 根据用户指定的功能或 Controller 名称，找到对应的 Controller 类。
   - 完整读取 Controller 全部代码（read_file）。

2. **追踪调用链**
   - 从 Controller → Service 接口 → Service 实现 → View 接口 → View 实现 → Repository → Mapper XML。
   - 每一层都必须 `read_file` 验证，不可推测。
   - 如果涉及 Feign 调用、MQ 消息、定时任务等外部依赖，一并追踪。

3. **梳理实体与 DTO**
   - 读取涉及的 Entity 类。
   - 读取每个接口的 Request 和 Response DTO，理解所有字段含义。
   - 读取枚举类、常量类等辅助类。

4. **找不到时立即中断**
   - 任何一层找不到源码或逻辑断链，立即向用户描述已探明部分和缺失部分，等待用户指示。

---

### 第二步：绘制调用链

用 ASCII 字符绘制以下内容：

```
【功能名称】调用链

Controller (XXXController)
  │  @PostMapping("/xxx")
  ↓
Service (XXXService)
  │
  ├──→ XXXView
  │     ├──→ XXXRepository
  │     │     └──→ Mapper XML: selectXxx
  │     └──→ 返回 Entity
  │
  └──→ XXXView (关联查询)
        └──→ ...

数据流向：Request DTO → Entity → DB → Entity → Response DTO
```

**必须标注**：
- 每个节点的类名和方法名
- SQL 操作类型（SELECT/INSERT/UPDATE/DELETE）
- 数据转换点（BeanUtils.copyProperties 等）
- 事务边界（@Transactional）

---

### 第三步：生成 .http 测试文件

**文件路径**：`{项目根目录}/doc/http/{功能名称}.http`

**文件格式**：

```http
### ==================================================
### {功能描述}
### Base URL: http://localhost:{port}/{context-path}
### ==================================================

@BASE_URL = http://localhost:{port}
@TOKEN = {token}

### {步骤描述}
POST {{BASE_URL}}/{path}
Content-Type: application/json
x-access-token: {{TOKEN}}

{
  "field": "value"
}

###
```

#### 3.1 响应值提取与变量传递（铁律）

**禁止使用 `@name` + `{{requestName.response.body.xxx}}` 方式提取变量**，该方式在 REST Client 中存在解析时求值问题，会导致变量未被替换。

**必须使用 `> {% %}` 脚本注入方式**，将响应中的关键字段提取到全局变量：

```http
### 02. 创建数据
POST {{BASE_URL}}/sys/xxx/create
Content-Type: application/json
x-access-token: {{TOKEN}}

{
  "name": "华东大区"
}

> {%
    client.global.set("xxx_id", response.body.data);
%}

###
### 03. 查询验证（使用上一步提取的ID）
GET {{BASE_URL}}/sys/xxx/find/{{xxx_id}}
Content-Type: application/json
x-access-token: {{TOKEN}}
```

**提取规则**：
- 必须先读取 Controller 和 Service 代码，确认每个接口的**实际返回格式**，不可凭经验猜测
- 添加接口（create/save）通常在 `data` 中直接返回新实体的 ID（String 类型），提取方式：`client.global.set("xxx_id", response.body.data);`
- 每个返回 ID 的接口，**必须**用 `> {% %}` 脚本提取，供下游接口使用
- 变量命名规范：`{业务实体}_id`，如 `dimension_id`、`tag_id`、`control_id`
- 分页查询接口：根据 Response DTO 确认分页对象字段名（通常为 `records`），如提取首条 ID：`response.body.data.records[0].id`
- 查询详情接口（findById/findOne）：根据 Response DTO 确认嵌套结构路径，如 `response.body.data.baseInfo.name`
- 后续所有需要引用该值的请求，统一使用 `{{变量名}}`

**完整示例**（多步依赖链条）：
```http
### 第一步：创建维度
POST {{BASE_URL}}/sys/analysis-dimension/create
Content-Type: application/json
x-access-token: {{TOKEN}}

{
  "name": "客户来源分析"
}

> {%
    client.test("01-创建维度", function() {
        client.assert(response.body.code === "200", "失败 code=" + response.body.code + " msg=" + response.body.message);
    });
    client.global.set("dimension_id", response.body.data);
%}

###
### 第二步：创建标签（依赖维度ID）
POST {{BASE_URL}}/sys/analysis-dimension/tag/create
Content-Type: application/json
x-access-token: {{TOKEN}}

{
  "relatedId": "{{dimension_id}}",
  "tagName": "抖音获客",
  ...
}

> {%
    client.test("02-创建标签", function() {
        client.assert(response.body.code === "200", "失败 code=" + response.body.code + " msg=" + response.body.message);
    });
    client.global.set("tag_id", response.body.data);
%}
```

#### 3.2 请求断言校验（铁律）

**每个请求都必须添加 `client.test()` 断言**，批量执行时通过 Test Results 面板快速区分通过/失败。

```http
### 01. 分页查询
GET {{BASE_URL}}/sys/xxx/list?current=1&size=10
Content-Type: application/json
x-access-token: {{TOKEN}}

> {%
    client.test("01-分页查询", function() {
        client.assert(response.body.code === "200", "失败 code=" + response.body.code + " msg=" + response.body.message);
    });
%}
```

**断言规则**：
- **每个请求**（包括分页查询、详情查询、新增、修改、删除）都必须有 `client.test()`
- test 名称格式：`"序号-业务描述"`，如 `"01-分页查询"`、`"02-创建维度"`、`"12-级联删除"`
- 校验标准：`response.body.code === "200"`（code 不是 200 一律算失败）
- 失败信息格式：`"失败 code=" + response.body.code + " msg=" + response.body.message`，方便定位问题
- 需要提取变量的请求，**断言写在提取之前**，先校验再提取

**组合示例**（断言 + 提取变量）：
```http
> {%
    client.test("02-创建维度", function() {
        client.assert(response.body.code === "200", "失败 code=" + response.body.code + " msg=" + response.body.message);
    });
    if (response.body.code === "200") {
        client.global.set("dimension_id", response.body.data);
    }
%}
```

**⚠ 提取变量必须加守卫**：`client.global.set()` 必须包裹在 `if (response.body.code === "200")` 中，防止请求失败时把错误信息（如 `"主键为[xxx]的数据不存在"`）当作变量值存入，导致后续请求全部使用错误的变量值。
```

**为什么不用 `@name` 方式**：
- `@name` + `{{requestName.response.body.xxx}}` 存在解析时求值问题，变量未替换
- `client.global.set()` + `client.test()` 是请求时执行，稳定可靠
- `client.test()` 提供可视化断言结果，批量执行时清晰看到每个请求的通过/失败状态

**⚠ 执行顺序铁律**：`.http` 文件中存在依赖链条（如创建 → 提取ID → 查询/更新/删除），变量依赖 `client.global.set()` 注入。**重新执行时不得从中间步骤开始**，必须从**第一个写操作**（通常是创建接口）开始执行，确保变量被正确注入。跳过创建步骤会导致变量为上一次已删除数据的旧值，后续请求全部失败。

---

### 第四步：测试数据构造规则

**严禁使用**：
- `"string"`、`"test"`、`"123"` 等无意义值
- 随机 UUID
- 纯数字递增序列（如 `"name1"`, `"name2"`）

**必须遵循**：
- 必须是中文，参数不允许中文，除非是必要的枚举之类
- 阅读 Request DTO 的每个字段，理解其业务含义
- 字符串字段：使用真实业务场景值，如 `"华东区域营销部"`、`"张三"`
- 日期字段：使用真实日期格式，如 `"2026-06-12"`
- 枚举字段：查阅枚举类，使用合法枚举值
- 金额/数字字段：使用符合业务逻辑的数值，如 `150000`（15万）
- ID引用字段：使用 `> {% %}` 脚本方式从上一个接口响应中提取，如 `"{{dimension_id}}"`

#### 4.0 枚举值核实铁律

**请求参数中的枚举值必须以实际枚举类源码为准，禁止凭经验推测。**

- 必须 `read_file` 读取枚举类源码，确认每个枚举常量的真实名称（如 `SINGLE_INPUT` 而非 `INPUT`、`CUSTOM_TIME_PICKER` 而非 `CUSTOM_DATE_PICKER`、`SPECIFIED_DATE` 而非 `APPOINT_DATE`、`TIME` 而非 `DATE`）。
- 禁止根据字段名或注释猜测枚举值，必须逐字核对枚举类中定义的常量。
- 如果枚举类不存在，立即中断并向用户确认。

```
检测方法：
- 生成 .http 文件前，必须对每个枚举字段对应的枚举类执行 read_file 验证
- 将 .http 中的枚举值与源码中的枚举常量逐字比对
- 发现不一致立即修正
```

**测试数据示例**：
```json
// ✅ 正确
{
  "name": "华东大区",
  "code": "HD",
  "sort": 1,
  "status": 1
}

// ❌ 错误
{
  "name": "test",
  "code": "123",
  "sort": 1,
  "status": 1
}
```

#### 4.1 请求参数全覆盖规则（铁律）

**每个请求体的每个属性都必须有值**，禁止遗漏任何字段。包括但不限于：

- 普通字段：String、Integer、Date、Enum 等全部填写
- 集合字段：`List<T>` 类型必须至少包含 **1 个元素**
- 嵌套集合：集合元素内部如果还有集合（如标签的 `controls` 列表），也必须至少包含 **1 个元素**
- 嵌套对象：对象内部的所有字段同样必须全部填写
- 枚举值：必须从枚举类中定义的常量中选择，不能使用经验推测的值。

**组合覆盖场景**：
- 集合中至少包含 2 个不同类型的元素（如有控件/无控件、启用/禁用等不同组合）
- 第一个元素覆盖所有字段（含嵌套集合），第二个元素可使用最小字段集

```json
// ✅ 正确：标签集合全覆盖
"tags": [
  {
    "id": "{{tag_id}}",                    // 更新场景带ID
    "tagName": "抖音引流获客",
    "tagType": "ANALYSIS_DIMENSION",
    "countField": "source_channel",
    "hasControl": "TRUE",
    "value": "douyin",
    "status": "ENABLED",
    "controls": [                           // 嵌套集合有值
      {
        "name": "获客成本范围",
        "controlType": "RANGE",
        "valueType": "NUMERIC",
        "dataComparisonMethod": "BETWEEN",
        "hasUnit": "TRUE",
        "unit": "元",
        "dataSource": "customer_core",
        "dataInterface": "customerInfo",
        "dataObject": "cost",
        "valueMin": "0",
        "minVal": "0",
        "maxVal": "500"
      }
    ]
  },
  {
    "tagName": "自然流量获客",              // 新增场景不带ID
    "tagType": "ANALYSIS_DIMENSION",
    "countField": "source_channel",
    "hasControl": "FALSE",                  // 不同组合：无控件
    "value": "organic",
    "status": "ENABLED"
  }
]

// ❌ 错误：标签集合为空或遗漏字段
"tags": [
  {
    "tagName": "抖音获客"
    // 缺少 tagType、countField、hasControl 等字段
  }
]
```

---

### 第五步：接口操作按业务流程编排

**.http 文件中的接口顺序必须反映真实业务流程**：

```
最新状态确认（分页查询，了解当前数据状态）
  ↓
新增（create/save）
  ↓
查询验证新增结果（findById/findAll）
  ↓
修改（update）
  ↓
查询验证修改结果
  ↓
删除（delete）
  ↓
查询验证删除结果（确认数据已不存在或状态已变更）
```

**每个写操作后必须紧跟验证查询**，在 .http 文件中用 `###` 分隔并标注验证点。

---

### 第七步：执行 .http 测试文件

生成的 `.http` 文件可通过以下方式执行：

- **VS Code**：安装 REST Client 插件，打开 `.http` 文件，点击每个请求上方的 `Send Request` 逐条执行。
- **IntelliJ IDEA**：原生支持 `.http` 文件，打开后点击请求旁的绿色运行按钮执行。
- **httpyac CLI**（若已安装）：
  ```
  npx httpyac doc/http/{功能名称}.http --all
  ```

逐条验证每个请求的响应，确保：
- 所有请求返回 `code=200`
- 增删改链路闭环，数据状态正确流转
- 如有报错，修复报错 → 重启项目 → 再次执行测试

### 第八步：问题汇总与报告（只输出不改代码）

完成测试后，输出以下报告：

```
═══════════════════════════════════════════
  功能测试报告 - {功能名称}
═══════════════════════════════════════════

一、调用链图示
（ASCII 流程图）

二、.http 文件位置
{项目根目录}/doc/http/{功能名称}.http

三、接口清单
| 序号 | 方法 | 路径 | 描述 |
|------|------|------|------|

四、发现的问题
| 问题编号 | 严重程度 | 问题描述 | 定位 |

五、修复建议
（每条问题对应的修复方案）

六、业务流程图
（测试场景的 ASCII 流程图）

═══════════════════════════════════════════
⚠ 以上问题请确认是否需要修复，确认后由老覃002主智能体执行修改。
═══════════════════════════════════════════
```

---

## 铁律
1. **完整追踪**：必须追踪到 Mapper XML 层，SQL 也必须查看。
2. **真实数据**：测试参数必须符合真实业务场景。
3. **链路闭环**：增→查→改→查→删→查，每一步都要验证。
4. **有问题必报**：发现任何疑点、潜在 bug、数据链路问题，必须写入报告。
5. **找不到必问**：任何源码找不到立即中断，向用户描述现状和缺失项。
6. **使用中文**：所有测试数据和注释说明、http文件命名都必须使用中文。枚举等特殊字符除外。
7. **保留业务流程图**：http文件里面，最上方需要保留业务流程图。
8. **响应值提取**：必须使用 `> {% client.global.set("key", response.body.data); %}` 脚本方式提取响应中的关键字段（如ID），**禁止**使用 `@name` + `{{requestName.response.body.xxx}}` 方式。
9. **参数全覆盖**：每个请求体的每个属性都必须填写有意义的业务值，集合字段至少包含 1 个元素，嵌套集合也必须包含元素，禁止遗漏任何字段。
10. **枚举值**：必须从枚举类中定义的常量中选择，不能使用经验推测的值。


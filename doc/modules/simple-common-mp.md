# simple-common-mp MyBatis-Plus 封装模块

> @author qty
> 版本：1.0.1

## 1. 模块介绍

`simple-common-mp` 是 simple-common 框架对 MyBatis-Plus 的封装增强模块，为业务层提供开箱即用的数据库操作基础设施。

该模块提供以下核心能力：

- **分页基类**：[`PageBase`](simple-common-mp/src/main/java/com/simple/common/mp/page/PageBase.java:29) 提供通用的分页参数（当前页、每页大小、动态排序），内置 SQL 注入检测，业务查询 DTO 继承即可使用
- **雪花ID自动生成**：[`CustomIdGenerator`](simple-common-mp/src/main/java/com/simple/common/mp/generator/CustomIdGenerator.java:14) 实现 MyBatis-Plus `IdentifierGenerator` 接口，自动为实体主键生成雪花ID
- **审计字段自动填充**：[`MybatisPlusOperationHandler`](simple-common-mp/src/main/java/com/simple/common/mp/handler/MybatisPlusOperationHandler.java:16) 实现 `MetaObjectHandler` 接口，自动填充 `createTime` / `updateTime`
- **数据权限注解**：[`@DataScopeTable`](simple-common-mp/src/main/java/com/simple/common/mp/common/annotation/DataScopeTable.java:29) 标注实体类，配合 [`DataScopeSqlHandler`](simple-common-mp/src/main/java/com/simple/common/mp/common/handler/DataScopeSqlHandler.java:29) 接口实现自动数据权限过滤
- **全局异常处理**：[`MPExceptionHandler`](simple-common-mp/src/main/java/com/simple/common/mp/exception/MPExceptionHandler.java:23) 捕获 MyBatis-Plus 相关异常并统一返回
- **通用枚举**：[`Status`](simple-common-mp/src/main/java/com/simple/common/mp/common/enums/Status.java:14) 状态枚举、[`DeleteState`](simple-common-mp/src/main/java/com/simple/common/mp/common/enums/DeleteState.java:14) 删除状态枚举

## 2. Maven 依赖

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-mp</artifactId>
    <version>1.0.1</version>
</dependency>
```

该模块会自动传递引入以下依赖：

- `mybatis-plus-spring-boot3-starter`：MyBatis-Plus Spring Boot 3 启动器
- `mybatis-plus-jsqlparser`：MyBatis-Plus SQL 解析器（用于数据权限拦截器解析 SQL AST）
- `postgresql`：PostgreSQL 驱动
- `mysql-connector-j`：MySQL 驱动
- `simple-common-core`：核心基础模块（提供 `AssertUtils`、`IdUtils`、`R` 等）

## 3. 核心功能

| 类名/注解 | 功能描述 | 关键特性 |
|------|---------|---------|
| [`PageBase`](simple-common-mp/src/main/java/com/simple/common/mp/page/PageBase.java:29) | 分页参数基类 | current/size/pageSort 三字段，驼峰转下划线排序，SQL 注入检测，最大1000条限制 |
| [`CustomIdGenerator`](simple-common-mp/src/main/java/com/simple/common/mp/generator/CustomIdGenerator.java:14) | 雪花ID生成器 | 实现 `IdentifierGenerator`，`nextId` 返回 Number，`nextUUID` 返回 String |
| [`MybatisPlusOperationHandler`](simple-common-mp/src/main/java/com/simple/common/mp/handler/MybatisPlusOperationHandler.java:16) | 审计字段自动填充 | insert 填充 createTime+updateTime，update 填充 updateTime |
| [`MPExceptionHandler`](simple-common-mp/src/main/java/com/simple/common/mp/exception/MPExceptionHandler.java:23) | MP 异常处理器 | 捕获 `DuplicateKeyException`，返回 400 + "数据已存在" |
| [`@DataScopeTable`](simple-common-mp/src/main/java/com/simple/common/mp/common/annotation/DataScopeTable.java:29) | 数据权限表标记注解 | 标注实体类，声明 tenantColumn/deptColumn/userColumn 三个权限字段 |
| [`DataScopeSqlHandler`](simple-common-mp/src/main/java/com/simple/common/mp/common/handler/DataScopeSqlHandler.java:29) | 数据权限 SQL 处理器接口 | `@FunctionalInterface`，业务方实现 `buildCondition` 返回 JSQLParser Expression |
| [`DataScopeInnerInterceptor`](simple-common-mp/src/main/java/com/simple/common/mp/common/interceptor/DataScopeInnerInterceptor.java:39) | 数据权限 SQL 拦截器 | JSQLParser 解析 SQL AST，支持 SELECT/UPDATE/DELETE，处理主表和 JOIN 表 |
| [`MybatisPlusConfig`](simple-common-mp/src/main/java/com/simple/common/mp/common/config/MybatisPlusConfig.java:23) | MyBatis-Plus 配置类 | 注册分页拦截器、数据权限拦截器（条件注入），自动扫描 `com.simple.common.mp` |
| [`Status`](simple-common-mp/src/main/java/com/simple/common/mp/common/enums/Status.java:14) | 通用状态枚举 | ON(1)/OFF(2)/NOT_USED(11)/USED(22)/OK(111)/ERROR(222)/INFO(333)，`@EnumValue` 映射 code |
| [`DeleteState`](simple-common-mp/src/main/java/com/simple/common/mp/common/enums/DeleteState.java:14) | 删除状态枚举 | DELETE(1)/OK(0)，`@EnumValue` 映射 code |

## 4. 配置说明

### 4.1 自动配置

模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动注册 [`MybatisPlusConfig`](simple-common-mp/src/main/java/com/simple/common/mp/common/config/MybatisPlusConfig.java:23)，提供以下功能：

- `@ComponentScan(basePackages = {"com.simple.common.mp"})`：自动扫描 mp 模块所有组件（`CustomIdGenerator`、`MybatisPlusOperationHandler`、`MPExceptionHandler` 等）
- 注册 `PaginationInnerInterceptor`：MyBatis-Plus 分页内置拦截器，数据库类型固定为 `DbType.POSTGRE_SQL`
- 条件注册 `DataScopeInnerInterceptor`：当 Spring 容器中存在 `DataScopeSqlHandler` Bean 时才启用数据权限拦截

### 4.2 拦截器装配逻辑

[`MybatisPlusConfig`](simple-common-mp/src/main/java/com/simple/common/mp/common/config/MybatisPlusConfig.java:23) 中 `MybatisPlusInterceptor` 的装配方式：

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(List<InnerInterceptor> interceptors) {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    for (InnerInterceptor inner : interceptors) {
        interceptor.addInnerInterceptor(inner);
    }
    return interceptor;
}
```

所有 `InnerInterceptor` 类型的 Bean（包括 `PaginationInnerInterceptor` 和条件注入的 `DataScopeInnerInterceptor`）都会被自动收集并注册到 `MybatisPlusInterceptor` 中。

### 4.3 无额外配置属性

本模块不提供独立的 `@ConfigurationProperties` 配置类，所有行为通过自动配置和条件注入完成。分页数据库类型固定为 PostgreSQL，如需切换为 MySQL，需在业务项目中覆盖 `PaginationInnerInterceptor` Bean 定义。

## 5. 核心类与接口详细说明

### 5.1 PageBase 分页基类

**类路径**：`com.simple.common.mp.page.PageBase`

**注解**：`@Slf4j`、`@Getter`、`@Setter`、`@Schema(description = "分页参数基类")`

**字段**：

| 字段 | 类型 | 默认值 | 校验 | 说明 |
|------|------|--------|------|------|
| `current` | `Integer` | `1` | `@NotNull(message = "当前页不能为空")` | 当前页码 |
| `size` | `Integer` | `10` | `@NotNull(message = "每页显示条数不能为空")` | 每页条数，最大1000，小于0时默认查询所有 |
| `pageSort` | `String[]` | `{}` | 无 | 排序规则数组，格式为 `字段名-true/false`，true 正序，false 倒序，字段名为驼峰命名 |

**方法**：

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `getPage()` | `Page<?>` | 获取分页对象（泛型为未知类型） |
| `getPage(Class<T> clazz)` | `Page<T>` | 获取指定实体类型的分页对象 |

**排序处理逻辑**（`createPage` 私有方法）：

1. `current` 为 null 时默认为 1，`size` 为 null 时默认为 10
2. 断言 `size <= 1000`，否则抛出异常 `"每页显示条数不能超过1000条"`
3. 遍历 `pageSort` 数组，每个元素按 `-` 分割，断言分割后长度为 2，否则抛出 `"排序字段格式必须为：字段名-true/false"`
4. 对排序字段名进行 SQL 注入检测（`SqlInjectionUtils.check`），检测到注入时记录 error 日志并抛出 `"请求错误"`
5. 排序字段名去除空格后通过 `StrUtil.toUnderlineCase` 转为下划线风格
6. 通过 `Boolean.parseBoolean` 解析排序方向
7. 构建 `OrderItem` 列表并设置到 `Page` 对象

### 5.2 CustomIdGenerator 雪花ID生成器

**类路径**：`com.simple.common.mp.generator.CustomIdGenerator`

**注解**：`@Component`

**实现接口**：`com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `nextId(Object entity)` | `Number` | 返回雪花ID（`Long` 类型），基于 `cn.hutool.core.util.IdUtil.getSnowflakeNextId()` |
| `nextUUID(Object entity)` | `String` | 返回雪花ID字符串，基于 `com.simple.common.core.utils.IdUtils.getSnowflakeNextIdStr()` |

**工作原理**：

- `nextId` 使用 Hutool 的 `IdUtil.getSnowflakeNextId()`，每次调用创建新的 Snowflake 实例（基于本机IP自动计算 workerId 和 datacenterId）
- `nextUUID` 使用 simple-common-core 的 `IdUtils.getSnowflakeNextIdStr()`，基于本机IP完整哈希计算 workerId（0-31）和 datacenterId（0-31），保证分布式唯一
- 实体类主键字段需标注 `@TableId(type = IdType.ASSIGN_ID)`，MyBatis-Plus 会自动调用 `nextId` 方法
- 实体类主键字段需标注 `@TableId(type = IdType.ASSIGN_UUID)`，MyBatis-Plus 会自动调用 `nextUUID` 方法

### 5.3 MybatisPlusOperationHandler 审计字段自动填充

**类路径**：`com.simple.common.mp.handler.MybatisPlusOperationHandler`

**注解**：`@Component`

**实现接口**：`com.baomidou.mybatisplus.core.handlers.MetaObjectHandler`

| 方法签名 | 说明 |
|---------|------|
| `insertFill(MetaObject metaObject)` | 插入时自动填充 `createTime` 和 `updateTime`，值为 `DateTime.now()` |
| `updateFill(MetaObject metaObject)` | 更新时自动填充 `updateTime`，值为 `DateTime.now()` |

**填充方式**：使用 `strictInsertFill` 方法，仅当实体类对应字段标注了 `@TableField(fill = FieldFill.INSERT)` 或 `@TableField(fill = FieldFill.INSERT_UPDATE)` 时才填充。

**实体类配置示例**：

```java
@TableField(fill = FieldFill.INSERT)
private Date createTime;

@TableField(fill = FieldFill.INSERT_UPDATE)
private Date updateTime;
```

### 5.4 MPExceptionHandler 异常处理器

**类路径**：`com.simple.common.mp.exception.MPExceptionHandler`

**注解**：`@Slf4j`、`@ControllerAdvice`、`@Order(Ordered.HIGHEST_PRECEDENCE)`

| 异常类型 | HTTP 状态码 | 响应消息 | 处理逻辑 |
|---------|-----------|---------|---------|
| `DuplicateKeyException` | 400 (BAD_REQUEST) | `"数据已存在"` | 调用 `ExceptionHandlerUtils.errorHandler` 记录异常日志，返回 `R.error("400", "数据已存在")` |

**优先级**：`@Order(Ordered.HIGHEST_PRECEDENCE)` 确保此处理器优先于其他全局异常处理器捕获 `DuplicateKeyException`。

### 5.5 @DataScopeTable 数据权限注解

**类路径**：`com.simple.common.mp.common.annotation.DataScopeTable`

**元注解**：`@Documented`、`@Retention(RetentionPolicy.RUNTIME)`、`@Target(ElementType.TYPE)`

**属性**：

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `tenantColumn` | `String` | `"tenant_id"` | 租户字段名（数据库列名，下划线风格） |
| `deptColumn` | `String` | `"dept_id"` | 部门字段名（数据库列名，下划线风格） |
| `userColumn` | `String` | `"create_user_id"` | 创建用户字段名（数据库列名，下划线风格） |

**使用方式**：标注在实体类上，声明该实体对应的数据库表需要全局数据权限过滤。拦截器会解析 SQL 中的 FROM 子句和 JOIN 子句，仅对标注了此注解的表追加 WHERE 条件。

### 5.6 DataScopeSqlHandler 数据权限 SQL 处理器接口

**类路径**：`com.simple.common.mp.common.handler.DataScopeSqlHandler`

**注解**：`@FunctionalInterface`

| 方法签名 | 返回值 | 说明 |
|---------|--------|------|
| `buildCondition(String tableAlias, DataScopeTable annotation)` | `Expression` | 构建数据权限 WHERE 条件表达式，返回 null 表示不追加条件 |

**参数说明**：

- `tableAlias`：SQL 中该表的别名（无别名时使用表名）
- `annotation`：实体类上的 `@DataScopeTable` 注解实例

**返回值**：JSQLParser 的 `Expression` 对象，将通过 `AND` 合并到 SQL 的 WHERE 子句中。

**启用条件**：实现此接口并注册为 Spring Bean 后，mp 模块自动启用数据权限拦截。若无任何实现，`DataScopeInnerInterceptor` 不会被创建，不影响正常使用。

### 5.7 DataScopeInnerInterceptor 数据权限 SQL 拦截器

**类路径**：`com.simple.common.mp.common.interceptor.DataScopeInnerInterceptor`

**实现接口**：`com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor`

**核心字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `annotationCache` | `Map<Class<?>, DataScopeTable>` | 注解缓存（`ConcurrentHashMap`），避免重复反射获取注解 |
| `handler` | `DataScopeSqlHandler` | 数据权限 SQL 处理器，由 Spring 条件注入 |

**拦截流程**（`beforePrepare` 方法）：

1. 获取 `MappedStatement`，判断 SQL 类型，仅处理 `SELECT`、`UPDATE`、`DELETE`
2. 通过 `MappedStatement.getResultMaps()` 获取实体类（`getEntityClass` 方法）
3. 从实体类上获取 `@DataScopeTable` 注解（`getAnnotation` 方法，使用 `ConcurrentHashMap.computeIfAbsent` 缓存）
4. 若注解不存在，直接返回不处理
5. 使用 JSQLParser 解析 SQL AST（`processSql` 方法），根据 SQL 类型分别处理：
   - **SELECT**：处理 `PlainSelect` 的 FROM 主表和 JOIN 表，对每个匹配的表调用 `handler.buildCondition` 获取条件表达式，通过 `AndExpression` 合并到 WHERE 子句
   - **UPDATE**：处理 `Update` 的目标表
   - **DELETE**：处理 `Delete` 的目标表
6. 若 SQL 发生变化，通过 `PluginUtils.mpBoundSql(boundSql).sql(processedSql)` 替换原始 SQL
7. 异常处理：拦截过程中发生异常时记录 error 日志，不中断执行

**别名解析**（`resolveAlias` 方法）：优先使用表的别名，无别名时使用表名。

**SELECT 处理细节**（`processPlainSelect` 方法）：

- 支持 `PlainSelect`（普通查询）和 `SetOperationList`（UNION 等集合操作）
- 对 FROM 主表：若为 `Table` 类型，调用 `handler.buildCondition` 获取条件，合并到 WHERE
- 对 JOIN 表：遍历所有 `Join`，对右侧 `Table` 类型同样处理

### 5.8 MybatisPlusConfig 配置类

**类路径**：`com.simple.common.mp.common.config.MybatisPlusConfig`

**注解**：`@Configuration`、`@ComponentScan(basePackages = {"com.simple.common.mp"})`

**Bean 定义**：

| Bean 名称 | 类型 | 条件 | 说明 |
|-----------|------|------|------|
| `dataScopeInnerInterceptor` | `DataScopeInnerInterceptor` | `@ConditionalOnBean(DataScopeSqlHandler.class)` | 数据权限拦截器，仅当容器中存在 `DataScopeSqlHandler` 时创建 |
| `paginationInnerInterceptor` | `PaginationInnerInterceptor` | 无条件 | 分页拦截器，数据库类型 `DbType.POSTGRE_SQL` |
| `mybatisPlusInterceptor` | `MybatisPlusInterceptor` | 无条件 | 主拦截器，自动收集所有 `InnerInterceptor` Bean 并注册 |

### 5.9 Status 通用状态枚举

**类路径**：`com.simple.common.mp.common.enums.Status`

**注解**：`@Getter`、`@AllArgsConstructor`

**字段**：

| 字段 | 类型 | 注解 | 说明 |
|------|------|------|------|
| `code` | `Integer` | `@EnumValue` | MyBatis-Plus 枚举映射字段，存入数据库的值 |
| `label` | `String` | 无 | 中文描述 |

**枚举值**：

| 枚举值 | code | label |
|--------|------|-------|
| `ON` | 1 | 启用 |
| `OFF` | 2 | 禁用 |
| `NOT_USED` | 11 | 未使用 |
| `USED` | 22 | 已使用 |
| `OK` | 111 | 成功 |
| `ERROR` | 222 | 失败 |
| `INFO` | 333 | 审核中 |

### 5.10 DeleteState 删除状态枚举

**类路径**：`com.simple.common.mp.common.enums.DeleteState`

**注解**：`@Getter`、`@AllArgsConstructor`

**字段**：

| 字段 | 类型 | 注解 | 说明 |
|------|------|------|------|
| `code` | `Integer` | `@EnumValue` | MyBatis-Plus 枚举映射字段 |
| `label` | `String` | 无 | 中文描述 |

**枚举值**：

| 枚举值 | code | label |
|--------|------|-------|
| `DELETE` | 1 | 已删除 |
| `OK` | 0 | 有效 |

## 6. 使用示例

### 6.1 PageBase 分页查询

> 示例来源：[`FindAllSysSmsCodeRequest`](simple-common-sms/src/main/java/com/simple/common/sms/common/dto/sysSmsCode/FindAllSysSmsCodeRequest.java:17) 和 [`FindAllSysSmsTemplateRequest`](simple-common-sms/src/main/java/com/simple/common/sms/common/dto/sysSmsTemplate/FindAllSysSmsTemplateRequest.java:16)

**第一步：创建分页请求 DTO，继承 PageBase**

```java
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(title = "短信验证码列表请求参数")
public class FindAllSysSmsCodeRequest extends PageBase {

    @Schema(description = "短信类型")
    private String sendType;

    @Schema(description = "电话号码")
    private String phone;

    @Schema(description = "验证码")
    private String code;

    @Schema(description = "请求状态")
    private Status reqStatus;

    @Schema(description = "使用状态")
    private Status status;

    @Schema(description = "删除状态")
    private DeleteState deleted;
}
```

**第二步：Controller 接收分页参数**

```java
@GetMapping("page")
public R<IPage<FindAllSysSmsCodeResponse>> page(FindAllSysSmsCodeRequest req) {
    return R.ok(sysSmsCodeService.findAll(req));
}
```

**第三步：Service 实现分页查询**

```java
@Override
public IPage<FindAllSysSmsCodeResponse> findAll(FindAllSysSmsCodeRequest req) {
    // 获取 MyBatis-Plus 分页对象
    Page<?> page = req.getPage();
    // 执行分页查询
    List<FindAllSysSmsCodeResponse> list = repository.selectPageWithNames(page, req);
    // 组装分页结果
    return new Page<FindAllSysSmsCodeResponse>(
            page.getCurrent(), page.getSize(), page.getTotal()
    ).setRecords(list);
}
```

**第四步：Repository 接口**

```java
List<FindAllSysSmsCodeResponse> selectPageWithNames(
        @Param("page") Page<SysSmsCode> page,
        @Param("pageRequest") FindAllSysSmsCodeRequest pageRequest);
```

**前端传参示例**：

```
GET /sms/code/page?current=1&size=10&pageSort=createTime-false&phone=13800138000
```

- `current=1`：第1页
- `size=10`：每页10条
- `pageSort=createTime-false`：按 createTime 倒序排列（驼峰字段名自动转下划线 `create_time`）

### 6.2 雪花ID自动生成

实体类主键配置 `@TableId(type = IdType.ASSIGN_ID)`，插入时自动生成雪花ID：

```java
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String nickname;
    // ...
}
```

插入数据时无需手动设置 id，`CustomIdGenerator.nextUUID` 会自动生成雪花ID字符串。

### 6.3 审计字段自动填充

实体类字段配置 `@TableField(fill = ...)`，插入/更新时自动填充时间：

```java
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String nickname;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- **插入**时：`createTime` 和 `updateTime` 自动填充为当前时间
- **更新**时：`updateTime` 自动填充为当前时间

### 6.4 通用枚举使用

实体类中使用 `Status` 和 `DeleteState` 枚举，数据库存储 `code` 值：

```java
@TableName("sys_sms_code")
public class SysSmsCode {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String phone;

    private Status status;       // 数据库存储 1(ON) 或 2(OFF)

    private DeleteState deleted; // 数据库存储 0(OK) 或 1(DELETE)
}
```

MyBatis-Plus 通过 `@EnumValue` 注解自动完成枚举与数据库值的映射，无需额外配置。

### 6.5 数据权限过滤

**第一步：实体类标注 `@DataScopeTable`**

```java
@DataScopeTable(tenantColumn = "tenant_id", deptColumn = "dept_id", userColumn = "create_user_id")
@TableName("t_order")
public class Order {
    private String id;
    private String tenantId;
    private String deptId;
    private String createUserId;
    // ...
}
```

**第二步：实现 `DataScopeSqlHandler` 接口并注册为 Spring Bean**

```java
@Component
public class LoginUserDataScopeHandler implements DataScopeSqlHandler {

    @Override
    public Expression buildCondition(String tableAlias, DataScopeTable annotation) {
        // 从登录上下文获取当前用户的数据权限信息
        DataPermission dp = LoginUserUtils.getUserTemporary().getDataPermission();

        // 根据权限级别构建 WHERE 条件
        // 例如：tenant_id = '当前租户' AND dept_id IN (可见部门列表)
        // 使用 JSQLParser 构建 Expression 对象返回

        // 返回 null 表示不追加条件（如超级管理员）
        return null;
    }
}
```

实现 `DataScopeSqlHandler` 并注册为 Bean 后，`MybatisPlusConfig` 会自动检测到该 Bean 并创建 `DataScopeInnerInterceptor`，对所有标注了 `@DataScopeTable` 的实体表自动追加数据权限 WHERE 条件。

## 7. 扩展点与自定义方式

### 7.1 自定义分页排序逻辑

[`PageBase`](simple-common-mp/src/main/java/com/simple/common/mp/page/PageBase.java:29) 的排序逻辑在私有方法 `createPage` 中实现，如需自定义排序规则，可继承 `PageBase` 并覆盖 `getPage` 方法：

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomPageBase extends PageBase {

    @Override
    public <T> Page<T> getPage(Class<T> clazz) {
        Page<T> page = super.getPage(clazz);
        // 自定义排序逻辑，例如添加默认排序
        if (page.orders().isEmpty()) {
            page.addOrder(new OrderItem("create_time", false));
        }
        return page;
    }
}
```

### 7.2 自定义 ID 生成策略

如需替换默认的雪花ID生成器，实现 `IdentifierGenerator` 接口并注册为 Spring Bean，覆盖 `CustomIdGenerator`：

```java
@Component
@Primary
public class CustomIdGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        // 自定义 ID 生成逻辑，例如使用 UUID 的哈希值
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    @Override
    public String nextUUID(Object entity) {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

### 7.3 扩展审计字段填充

如需填充更多审计字段（如 `createUserId`、`updateUserId`），继承或替换 `MybatisPlusOperationHandler`：

```java
@Component
@Primary
public class CustomOperationHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        // 扩展：填充创建人ID
        this.strictInsertFill(metaObject, "createUserId", String.class,
                LoginUserUtils.getUserTemporary().getUserId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        // 扩展：填充修改人ID
        this.strictInsertFill(metaObject, "updateUserId", String.class,
                LoginUserUtils.getUserTemporary().getUserId());
    }
}
```

实体类对应字段需标注 `@TableField(fill = FieldFill.INSERT)` 或 `@TableField(fill = FieldFill.INSERT_UPDATE)`。

### 7.4 自定义数据权限控制

**方式一：实现 `DataScopeSqlHandler` 接口**

实现 `buildCondition` 方法，根据业务需求构建 JSQLParser `Expression`：

```java
@Component
public class BusinessDataScopeHandler implements DataScopeSqlHandler {

    @Override
    public Expression buildCondition(String tableAlias, DataScopeTable annotation) {
        // 构建列名（带别名前缀）
        String tenantColumn = tableAlias + "." + annotation.tenantColumn();

        // 使用 JSQLParser 构建: tenant_id = '当前租户ID'
        try {
            return CCJSqlParserUtil.parseCondExpression(tenantColumn + " = '" + getTenantId() + "'");
        } catch (JSQLParserException e) {
            return null;
        }
    }
}
```

**方式二：自定义 `@DataScopeTable` 注解属性**

如需更多权限维度（如角色字段），可创建自定义注解替代 `@DataScopeTable`，并在 `DataScopeSqlHandler` 实现中处理自定义注解。

### 7.5 切换分页数据库类型

默认分页拦截器固定为 `DbType.POSTGRE_SQL`，如需切换为 MySQL，在业务项目中覆盖 `PaginationInnerInterceptor` Bean：

```java
@Configuration
public class MyBatisPlusCustomConfig {

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        return new PaginationInnerInterceptor(DbType.MYSQL);
    }
}
```

由于 `MybatisPlusConfig` 中的 `paginationInnerInterceptor` 未使用 `@ConditionalOnMissingBean`，覆盖时需确保业务配置类的优先级更高，或排除 `MybatisPlusConfig` 的自动配置。

## 8. 注意事项

### 8.1 分页排序字段的安全处理

- [`PageBase`](simple-common-mp/src/main/java/com/simple/common/mp/page/PageBase.java:29) 使用 `SqlInjectionUtils.check` 对排序字段名进行 SQL 注入检测，检测到注入语句时会记录 error 日志并抛出 `"请求错误"` 异常
- 排序字段名会先去除空格（`StrUtil.replace(split[0], " ", "")`），再通过 `StrUtil.toUnderlineCase` 转为下划线风格，前端传入驼峰命名即可
- 排序格式必须为 `字段名-true/false`，否则抛出 `"排序字段格式必须为：字段名-true/false"` 异常
- `size` 最大限制为 1000，超过会抛出 `"每页显示条数不能超过1000条"` 异常

### 8.2 雪花ID的时钟回拨处理

- [`CustomIdGenerator.nextId`](simple-common-mp/src/main/java/com/simple/common/mp/generator/CustomIdGenerator.java:22) 使用 Hutool 的 `IdUtil.getSnowflakeNextId()`，该方法每次调用创建新的 Snowflake 实例，不依赖上一次生成的时间戳，因此**不存在时钟回拨问题**
- [`CustomIdGenerator.nextUUID`](simple-common-mp/src/main/java/com/simple/common/mp/generator/CustomIdGenerator.java:32) 使用 simple-common-core 的 `IdUtils.getSnowflakeNextIdStr()`，基于本机IP完整哈希计算 workerId 和 datacenterId，同样每次创建新实例
- 代价是每次调用都会创建新的 Snowflake 实例，有轻微性能开销，但在正常业务量下可忽略

### 8.3 自动填充的字段范围

- [`MybatisPlusOperationHandler`](simple-common-mp/src/main/java/com/simple/common/mp/handler/MybatisPlusOperationHandler.java:16) 仅填充 `createTime` 和 `updateTime` 两个字段
- 使用 `strictInsertFill` 方法，仅当实体类字段标注了对应的 `@TableField(fill = ...)` 注解时才生效
- **不填充** `createUserId`、`updateUserId`、`tenantId` 等身份相关字段，这些字段需在 Service 层通过 `LoginUserUtils` 手动赋值
- `updateFill` 方法中使用的是 `strictInsertFill`（而非 `strictUpdateFill`），这是 MyBatis-Plus 的 API 特性，`strictInsertFill` 在 update 场景下同样可用于填充字段

### 8.4 数据权限拦截器的使用限制

- [`DataScopeInnerInterceptor`](simple-common-mp/src/main/java/com/simple/common/mp/common/interceptor/DataScopeInnerInterceptor.java:39) 通过 `MappedStatement.getResultMaps()` 获取实体类，**仅对返回类型为标注了 `@DataScopeTable` 的实体类的 SQL 生效**
- 若 SQL 的 resultMap 类型为 `Object` 或无 resultMap，拦截器不会处理
- 拦截器在处理 SQL 解析异常时会记录 error 日志但**不中断执行**，原始 SQL 会继续执行（不追加权限条件）
- 注解缓存使用 `ConcurrentHashMap.computeIfAbsent`，线程安全，但实体类上的 `@DataScopeTable` 注解在运行时不可变更

### 8.5 分页数据库类型固定

- [`MybatisPlusConfig`](simple-common-mp/src/main/java/com/simple/common/mp/common/config/MybatisPlusConfig.java:32) 中 `PaginationInnerInterceptor` 固定使用 `DbType.POSTGRE_SQL`
- 若业务项目使用 MySQL，需按 [7.5 节](#75-切换分页数据库类型) 方式覆盖 Bean 定义

### 8.6 枚举使用注意

- [`Status`](simple-common-mp/src/main/java/com/simple/common/mp/common/enums/Status.java:14) 和 [`DeleteState`](simple-common-mp/src/main/java/com/simple/common/mp/common/enums/DeleteState.java:14) 使用 `@EnumValue` 注解映射 `code` 字段到数据库
- MyBatis-Plus 配置文件中需确保 `default-enum-type-handler` 为 `MybatisEnumTypeHandler`（Spring Boot 3 + MyBatis-Plus 3.5.x 默认已配置）
- 枚举的 `code` 值为 `Integer` 类型，数据库对应字段应为整型

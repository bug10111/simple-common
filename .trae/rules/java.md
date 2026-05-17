---
alwaysApply: true
---
# Java 开发规范（补充完整版）

## 1. 终极目标：高性能、高可用、线程安全的企业级项目

这是一切开发工作的最高准则，凌驾于所有其他规则。

- 严禁为节省时间而偷懒：任何牺牲质量换取速度的做法都不被允许
- 严禁因上下文限制放弃更好的方案：若已知存在更优的实现方式，即使超出当前对话长度，也必须通过任务拆分、分步执行来达成，绝不能退而求其次
- 严禁写出半成品：交付的代码必须是完整、可运行、经过验证的企业级实现
- 严禁不用已有成熟实现：项目或框架已提供的高质量组件必须使用，禁止单纯为了"方便"引入批量性、低质量或不满足线程安全、高性能要求的第三方方案
- 大型任务必须拆分：因上下文或通信时间限制的大型任务，严格按既有规则拆分为可执行步骤，逐条完成；必要时可跨越多次对话，确保最终结果完全符合企业级标准

> **补充强调：** 高性能、高可用、线程安全是一切开发工作的最高准则，任何代码都必须以此为首要目标。

---

## 2. 强制使用 simple-common 框架

### 2.1 核心原则

所有操作、代码、工具调用和 API 使用，必须优先使用 simple-common 提供的功能。严禁使用 Spring 原生组件、第三方库或自编代码替代 simple-common 已有实现。

### 2.2 查证流程

1. 需求分析
2. 查阅 simple-common 文档/源码
3. 确认是否存在对应实现
4. 有则直接使用，无则向用户确认是否扩展

### 2.3 违规示例

- ❌ 使用 `cn.hutool.core.codec.Base64` 而不用 `Base64Utils`

---

## 3. 核心铁律：以实际代码为准，严禁推测

### 3.1 阅读与验证强制要求

- **全局阅读：** 当要求"全局阅读"时，必须列出每个类并逐一阅读，严禁随机获取、模糊搜索或通配符搜索
- **完整阅读：** 系统阅读模块内每一个 Java 文件，禁止仅依赖关键词搜索
- **引前验证：** 引用任何类、方法、注解前，必须通过 read_file 确认其真实存在
- **架构准确：** 方法签名、返回值、调用方式必须与实际代码完全一致，禁止虚构
- **示例真实：** 所有示例必须使用真实存在的 API

### 3.2 大型任务执行策略

1. 拟定详细执行步骤并告知用户
2. 等待用户确认后再继续
3. 根据确认的步骤创建待办任务项
4. 严格按照待办项顺序逐一执行，每项完成后标记
5. 若一次对话无法完成，可分多次对话继续

### 3.3 严禁行为

- 虚构工具类（如 "RedisUtils"）、虚构服务层
- 使用错误注解（如 `@EventListener`）或消费方式（`@RabbitListener`）
- 由一个模块推断另一个模块的实现
- 粒度过粗的操作（如菜单变更时刷新整个项目的所有角色权限）
- 方法滥用（如只使用批量方法，忽略精细化方法）
- 职责不清（如菜单增删改不触发对应角色权限的精准变更）

---

## 4. 注释与署名规范

- **禁止 AI 痕迹：** 注释中严禁出现"优化"、"修复版本"等词
- **统一署名：** `@author` 必须统一为 `"qty"`
- **Javadoc 规范：** 接口注释必须包含 `@param` 和 `@return`
- **只改注释：** 注释完善任务严禁修改代码逻辑和接口定义

---

## 5. 开发与编译强制流程

1. **先通读后修改：** 理解模块架构和依赖关系后再动手
2. **强制编译：** 任何代码修改后，必须执行 `mvn clean compile` 并确保 BUILD SUCCESS
3. **编译失败处理：** 查看错误 -> 定位问题 -> 修复 -> 重新编译，直至成功
4. **合理性审查：** 检查逻辑闭环、代码复用优先级（simple-common 优先）及架构一致性

---

## 6. 文档编写规范

- **只写真实存在：** 所有类、接口、配置项必须在代码中真实存在
- **覆盖自定义注解和接口：** 文档必须包含注解、封装接口的说明及示例
- **示例可运行：** 示例代码必须基于真实 API，可直接复制使用
- **结构规范：** 按模块介绍 -> 核心功能表格 -> 继承实现 -> 扩展举例 -> 使用示例的顺序

---

## 7. @HasAuthority 权限字段命名规则

- **规则：** 类级别 `@RequestMapping` + 方法路径，将 `/` 替换为 `:`，剔除 PathVariable
- **示例：** `@RequestMapping("sys/department")` + `@GetMapping("tree")` → `sys:department:tree`
- **强制：** 所有权限标识必须按此生成

---

## 8. 代码质量优先级

**线程安全 > 高性能 > 低内存**

### 8.1 线程安全（最高优先级）

- 共享变量必须使用同步机制（`synchronized`、`Lock`、`ConcurrentHashMap` 等）
- 局部变量天然安全，可使用非线程安全类（如 `StringBuilder`）
- 优先使用不可变对象（`final`）
- 数据库操作必须添加 `@Transactional`
- check-then-act 操作需加锁或使用原子类

### 8.2 高性能（第二优先级）

- 数据库层面优先：LIMIT、索引、批量查询，避免 N+1 查询问题
- 算法优化：使用合理数据结构，降低复杂度
- 控制内存：避免一次加载海量数据

---

## 9. 全限定类名使用规则

### 9.1 禁止使用场景

- **任何地方：** 禁止在代码中的任何位置使用全限定类名
- **方法参数和返回值：** 禁止在接口定义和实现中使用全限定类名作为参数类型或返回类型
- **复杂类型声明：** 禁止在泛型类型、数组类型、嵌套类型等复杂类型中使用全限定类名
- **跨模块引用：** 禁止在引用其他模块或第三方库的类时使用全限定类名

### 9.2 正确示例

```java
import com.simple.oauth.common.dto.sysUser.PageSysUserResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

List<SysUser> selectPageWithUserNames(
    @Param("page") Page<SysUser> page,
    @Param("pageRequest") PageSysUserRequest pageRequest);
```

### 9.3 错误示例

```java
List<SysUser> selectPageWithUserNames(
    @Param("page") com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> page,
    @Param("pageRequest") com.simple.oauth.common.dto.sysUser.PageSysUserRequest pageRequest);
```

### 9.4 导入语句要求

- 必须在文件顶部添加对应的 import 语句
- 禁止在方法体内使用全限定类名而不添加导入语句
- 禁止任何形式的全限定类名使用

---

## 10. 代码命名风格规范

### 10.1 类命名规则

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 服务接口 | `{业务模块}Service` | `SysUserService`, `SysRoleService` |
| 服务实现 | `Default{业务模块}Service` | `DefaultSysUserService`, `DefaultSysRoleService` |
| 视图接口 | `{实体}View` | `SysUserView` |
| 视图实现 | `MP{实体}View` | `MPSysUserView` |
| 仓库接口 | `{实体}Repository` | `SysUserRepository` |
| 控制器 | `{业务模块}Controller` | `SysUserController`, `SysRoleController` |
| DTO请求 | `{操作}{业务模块}Request` | `PageSysUserRequest`, `CreateSysUserRequest` |
| DTO响应 | `{操作}{业务模块}Response` | `PageSysUserResponse`, `InfoSysUserResponse` |
| 实体类 | `{实体}` | `SysUser`, `SysRole`, `SysMenu` |
| 枚举类 | `{业务模块}{分类}KindProcess` | `OauthLoginErrorKindProcess`, `LoginErrorKindProcess` |
| 异常处理 | `{业务模块}ExceptionHandler` | `MPExceptionHandler` |
| 配置类 | `{功能}Config` | `FeignConfig`, `SecurityConfig` |

### 10.2 方法命名规则

| 操作 | 命名模式 | 示例 |
|------|---------|------|
| 分页查询 | `findAll` | `IPage<SysUser> findAll(PageSysUserRequest pageRequest)` |
| 单条查询 | `findById`, `findOne` | `SysUser findById(String id)` |
| 条件查询 | `findAll`（带条件参数） | `List<SysUser> findAll(FindAllSysUserRequest request)` |
| 统计查询 | `findCount` | `Long findCount(FindOneSysUserRequest request)` |
| 新增 | `save`, `create` | `void save(SysUser sysUser)` |
| 更新 | `updateById` | `void updateById(SysUser sysUser)` |
| 批量更新 | `updateById`（List参数） | `void updateById(List<SysUser> list)` |
| 删除 | `deleteByIds`, `delete` | `void deleteByIds(List<String> ids)` |
| 批量新增 | `saves` | `void saves(List<SysUser> list)` |

### 10.3 字段命名规则

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 主键 | `id` | `private String id` |
| 创建人ID | `createUserId` | `private String createUserId` |
| 创建人名称 | `createUserName` | `private String createUserName` |
| 创建时间 | `createTime` | `private Date createTime` |
| 修改人ID | `updateUserId` | `private String updateUserId` |
| 修改人名称 | `updateUserName` | `private String updateUserName` |
| 修改时间 | `updateTime` | `private Date updateTime` |
| 扩展字段 | `reserve` | `private Map<String, Object> reserve` |

### 10.4 目录结构规范

```
src/main/java/com/simple/oauth/
├── common/                    # 公共模块
│   ├── dto/                   # 数据传输对象
│   │   └── sysUser/           # 业务模块 DTO
│   ├── entity/                # 实体类
│   ├── enums/                 # 枚举类
│   └── service/               # 服务接口
│       └── view/              # 视图接口
├── controller/                # 控制器
├── manager/                   # 管理器
├── repository/                # 数据访问层
├── service/                   # 服务实现
│   └── view/                  # 视图实现
└── SimpleOauthApplication.java
```

### 10.5 命名一致性要求

- **保持与项目现有代码一致：** 新增代码必须遵循项目已有的命名风格
- **检查历史代码：** 在编写新代码前，查看同类型的现有代码，确保命名风格一致
- **统一后缀：** 严格按照上述表格使用统一的类名后缀

---

## 11. MyBatis XML 文件规范

### 11.1 文件命名规则

- **文件位置：** 所有 MyBatis XML 文件必须放在 `src/main/resources/mapper/` 目录下
- **文件命名：** 文件名必须以 `Dao.xml` 结尾
- **示例：** `SysUserDao.xml`、`SysRoleDao.xml`、`DepartmentDao.xml`

### 11.2 SQL 编写规范（新增）

- **禁止使用 DATE_FORMAT 函数：** 依赖全局配置处理时间格式
- **必须通过 JOIN 查询获取外键对应的名称/code：** 不能仅返回 ID
- **必须在xml中写sql：** 不能在注解里面写sql

### 11.3 SQL 标签使用

- 使用 `<where>` 标签：替代 `WHERE 1=1`，MyBatis 会自动处理空条件和前缀 AND/OR

**正确示例：**

```xml
<select id="selectPageWithUserNames" resultType="...">
    SELECT
        u.id, u.nickname, u.username, u.phone, u.reserve,
        u.create_user_id as createUserId,
        cu.nickname as createUserName,
        u.create_time as createTime,
        u.update_user_id as updateUserId,
        uu.nickname as updateUserName,
        u.update_time as updateTime
    FROM sys_user u
    LEFT JOIN sys_user cu ON u.create_user_id = cu.id
    LEFT JOIN sys_user uu ON u.update_user_id = uu.id
    <where>
        <if test="pageRequest.nickname != null and pageRequest.nickname != ''">
            AND u.nickname LIKE CONCAT('%', #{pageRequest.nickname}, '%')
        </if>
        <if test="pageRequest.username != null and pageRequest.username != ''">
            AND u.username LIKE CONCAT('%', #{pageRequest.username}, '%')
        </if>
    </where>
    ORDER BY u.create_time DESC
</select>
```

### 11.4 Repository 接口参数规范

- **直接使用 DTO 对象：** 方法参数应直接接收 DTO 对象，不要拆解参数

```java
// ✅ 正确：直接使用 PageSysUserRequest
List<SysUser> selectPageWithUserNames(
    @Param("page") Page<SysUser> page,
    @Param("pageRequest") PageSysUserRequest pageRequest);

// ❌ 错误：拆解参数
List<SysUser> selectPageWithUserNames(
    @Param("page") Page<SysUser> page,
    @Param("nickname") String nickname,
    @Param("username") String username);
```

---

## 12. DTO 设计规范（新增）

### 12.1 响应DTO规范

- **响应DTO返回名称而非ID：** 例如返回 `sysProjectName` 而非 `sysProjectId`
- **禁止在响应中暴露敏感信息：** 如密钥、明文密码等敏感字段不得出现在响应DTO中

---

## 13. 日志规范（新增）

### 13.1 敏感信息日志记录

- 敏感信息使用 `debug` 级别记录，生产环境不输出
- 避免在 `info/warn/error` 级别中打印敏感数据（如密码、token、密钥）

---

## 14. 编码完成检查清单

- [ ] 调用了 simple-common 对应技能并查阅文档
- [ ] 所有引用的类、方法已验证存在
- [ ] 共享变量已使用同步机制
- [ ] 数据库操作已添加 `@Transactional`
- [ ] 异常已正确处理，未吞掉异常
- [ ] 日志已正确记录，未包含敏感信息
- [ ] 已执行 `mvn clean compile` 并成功
- [ ] 代码逻辑已验证闭环
- [ ] 权限标识已按规则生成
- [ ] `@author` 已设置为 `qty`
- [ ] 使用了 `<where>` 标签而非 `WHERE 1=1`
- [ ] Repository 接口参数直接使用 DTO 对象
- [ ] SQL中禁止使用 `DATE_FORMAT`，必须 JOIN 获取名称
- [ ] 响应DTO返回名称而非ID，无敏感信息
- [ ] 敏感信息使用 debug 级别日志

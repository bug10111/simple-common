## 5. simple-common-mp

### 模块介绍

MyBatis-Plus封装模块，提供分页、通用Mapper、数据权限全局过滤等功能。

### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 分页基类 | `PageBase` | 分页参数基类，支持动态排序 |
| 通用Mapper | `BaseMapper` | MyBatis-Plus通用Mapper |
| 数据权限注解 | `@DataScopeTable` | 标记实体类需要全局数据权限过滤 |
| 数据权限拦截器 | `DataScopeInnerInterceptor` | SQL拦截器，自动追加租户/部门/用户过滤条件 |
| 数据权限处理器 | `DefaultDataPermissionLineHandler` | 从登录上下文获取数据权限，生成WHERE条件 |

### 数据权限功能

mp模块已内置数据权限全局过滤拦截器，无需业务代码手动拼接 tenant_id/dept_id/create_user_id 条件。

**4种权限级别（定义于 `DataScopeEnum`）：**

| 权限 | Value | 自动追加的 WHERE 条件 |
|------|-------|----------------------|
| ALL | 1 | `alias.tenant_id = 'xxx'` |
| DEPT_AND_CHILD | 2 | `alias.tenant_id = 'xxx' AND alias.dept_id IN (...)` |
| DEPT | 3 | `alias.tenant_id = 'xxx' AND alias.dept_id IN (...)` |
| SELF | 4 | `alias.tenant_id = 'xxx' AND alias.create_user_id = 'yyy'` |

> **注意：** ALL 权限依然受租户隔离约束，不跳过 tenant_id 过滤。
> DEPT_AND_CHILD 的 departmentIds 在登录时由服务端展开为完整子部门树，拦截器直接使用。

### 集成方式

**步骤1：添加依赖**

```xml
<dependency>
    <groupId>com.simple.common</groupId>
    <artifactId>simple-common-mp</artifactId>
    <version>${version}</version>
</dependency>
```

**步骤2：配置数据库连接（必须）**

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver  # 或 com.mysql.cj.jdbc.Driver
    url: jdbc:postgresql://localhost:5432/your_database
    username: your_username
    password: your_password
```

**步骤3：配置Mapper扫描路径（必须）**

在启动类或配置类上添加`@MapperScan`注解：

```java
@SpringBootApplication
@MapperScan("com.yourpackage.mapper")  // 替换为你的Mapper包路径
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**步骤4：实体类标注 @DataScopeTable（使用数据权限时必须）**

```java
@Data
@TableName("t_order")
@DataScopeTable(tenantColumn = "tenant_id", deptColumn = "dept_id", userColumn = "create_user_id")
public class Order {
    private String id;
    private String tenantId;
    private String deptId;
    private String createUserId;
}
```

标注后，所有针对该表的 SELECT/UPDATE/DELETE 操作将自动追加数据权限条件，无需手动编写。

**重要说明：**
- mp模块已自动配置分页插件（默认PostgreSQL）和数据权限拦截器
- 自动填充功能已启用（createTime、updateTime字段）
- PageBase支持动态排序，格式：`字段名-true/false`（驼峰命名）
- 数据权限通过 `DataScopeSqlHandler` 接口解耦，mp 不直接依赖任何认证模块
- 提供 `DataScopeSqlHandler` 实现（如 `LoginUserDataScopeSqlHandler`）后，拦截器自动生效
- 无任何实现时，拦截器不注册，不影响正常使用
- 未登录或 DataPermission 为空时，数据权限过滤自动跳过

### 使用示例

**1. 分页查询**

查询对象继承 `PageBase`：

```java
@Data
public class UserQuery extends PageBase {
    private String username;
    private Integer status;
}
```

Service中使用：

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public IPage<User> pageQuery(UserQuery query) {
        Page<User> page = query.getPage(User.class);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StrUtil.isNotBlank(query.getUsername())) {
            wrapper.like(User::getUsername, query.getUsername());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }
        
        return userMapper.selectPage(page, wrapper);
    }
}
```

**2. 多表 JOIN 查询（数据权限自动过滤）**

```xml
<!-- XML Mapper -->
<select id="selectOrderWithUser" resultType="com.example.dto.OrderDTO">
    SELECT o.*, u.nickname
    FROM t_order o
    LEFT JOIN sys_user u ON o.create_user_id = u.id
    <where>
        <if test="status != null">
            AND o.status = #{status}
        </if>
    </where>
    ORDER BY o.create_time DESC
</select>
```

拦截器自动识别 `t_order`（标注了`@DataScopeTable`），在SQL中追加：
```sql
WHERE o.tenant_id = 'tenant_001' AND o.dept_id IN ('dept_a', 'dept_b')
```

多表JOIN中未标注 `@DataScopeTable` 的表（如 `sys_user`）不会追加过滤条件。

---

[返回主文档](../README.md)

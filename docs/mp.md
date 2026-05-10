## 5. simple-common-mp

### 模块介绍

MyBatis-Plus封装模块，提供分页、通用Mapper等功能。

### 核心功能

| 功能分类 | 接口/组件 | 说明 |
|---------|----------|------|
| 分页基类 | `PageBase` | 分页参数基类，支持动态排序 |
| 通用Mapper | `BaseMapper` | MyBatis-Plus通用Mapper |

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

**重要说明：**
- mp模块已自动配置分页插件（默认PostgreSQL），如需切换为MySQL，需自定义`MybatisPlusInterceptor`
- 自动填充功能已启用（createTime、updateTime字段）
- PageBase支持动态排序，格式：`字段名-true/false`（驼峰命名）

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

---

[返回主文档](../README.md)

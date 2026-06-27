---
name: "simple-common-mp"
description: "MyBatis-Plus 封装模块。提供 PageBase 分页基类（current/size/pageSort排序）、CustomIdGenerator 雪花ID自动生成、MybatisPlusOperationHandler 自动填充 createTime/updateTime、@DataScopeTable 数据权限注解。当需要数据库分页查询或MP相关功能时使用。"
---

# simple-common-mp 认知文档

**Maven**: `simple-common-mp`
**包路径**: `com.simple.common.mp`

## PageBase — 分页基类

**DTO 继承方式**：

```java
@Getter
@Setter
public class PageSysUserRequest extends PageBase {
    private String nickname;
    private Integer status;
}
```

**分页参数**：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `current` | `Integer` | `1` | 当前页 |
| `size` | `Integer` | `10` | 每页条数，最大1000 |
| `pageSort` | `String[]` | `{}` | 排序：`{"createTime-false"}`（字段名-true正序/false倒序） |

**Controller/Service 完整用法**：

```java
// Controller
@GetMapping("/user/page")
@HasAuthority({"sys:user:page"})
public R<IPage<PageSysUserResponse>> page(PageSysUserRequest req) {
    return R.ok(sysUserService.findAll(req));
}

// Service实现
@Override
public IPage<PageSysUserResponse> findAll(PageSysUserRequest req) {
    Page<?> page = req.getPage();                           // 获取MP分页对象
    List<PageSysUserResponse> list = repository.selectPageWithUserNames(page, req);
    return new Page<PageSysUserResponse>(page.getCurrent(), page.getSize(), page.getTotal()).setRecords(list);
}

// Repository
List<PageSysUserResponse> selectPageWithUserNames(
    @Param("page") Page<SysUser> page,
    @Param("pageRequest") PageSysUserRequest pageRequest);
```

## 自动功能

| 功能 | 说明 |
|------|------|
| 雪花ID | `CustomIdGenerator` 自动为所有 `String id` 主键生成雪花ID |
| 自动填充 | `MybatisPlusOperationHandler` 自动填充 `createTime` / `updateTime` |
| 数据权限 | 实体加 `@DataScopeTable` → 实现 `DataScopeSqlHandler` → 拦截器自动注入WHERE条件 |

## POM依赖

```xml
<dependency>
    <groupId>com.simple</groupId>
    <artifactId>simple-common-mp</artifactId>
</dependency>
```
package com.simple.common.mp.common.annotation;

import java.lang.annotation.*;

/**
 * 数据权限表标记注解
 * <p>
 * 标注在实体类上，声明该实体对应的数据库表需要全局数据权限过滤。
 * 拦截器会解析 SQL 中的 FROM 子句，仅对标注了此注解的表追加 WHERE 条件。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * &#64;DataScopeTable(tenantColumn = "tenant_id", deptColumn = "dept_id", userColumn = "create_user_id")
 * &#64;TableName("t_order")
 * public class Order {
 *     private String id;
 *     private String tenantId;
 *     private String deptId;
 *     private String createUserId;
 * }
 * }</pre>
 *
 * @author qty
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataScopeTable {

    /**
     * 租户字段名（数据库列名，下划线风格）
     *
     * @return 租户字段名，默认 tenant_id
     */
    String tenantColumn() default "tenant_id";

    /**
     * 部门字段名（数据库列名，下划线风格）
     *
     * @return 部门字段名，默认 dept_id
     */
    String deptColumn() default "dept_id";

    /**
     * 创建用户字段名（数据库列名，下划线风格）
     *
     * @return 创建用户字段名，默认 create_user_id
     */
    String userColumn() default "create_user_id";
}

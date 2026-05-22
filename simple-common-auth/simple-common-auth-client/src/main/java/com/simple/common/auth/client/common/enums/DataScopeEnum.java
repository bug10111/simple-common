package com.simple.common.auth.client.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限枚举
 * 权限值按数据权限范围从大到小递增：ALL(1) < DEPT_AND_CHILD(2) < DEPT(3) < SELF(4)
 * 取值最小即为最大权限范围
 * <p>
 * 所有权限级别均受租户隔离约束，ALL 权限亦不跳过租户过滤。
 * </p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
@Schema(description = "数据权限枚举")
public enum DataScopeEnum {

    /**
     * 全部数据权限（最高）
     * 仅受租户隔离约束，可查看本租户下所有数据
     */
    ALL(1, "全部数据权限", "最高", "仅受租户隔离约束，可查看本租户下所有数据"),

    /**
     * 部门及以下数据权限（较高）
     * 能查看用户所属部门及所有子部门的数据，departmentIds 在登录时展开
     */
    DEPT_AND_CHILD(2, "部门及以下数据权限", "较高", "能查看用户所属部门及所有子部门的数据"),

    /**
     * 部门数据权限（中等）
     * 仅能查看用户所属本部门的数据
     */
    DEPT(3, "部门数据权限", "中等", "仅能查看用户所属本部门的数据"),

    /**
     * 仅本人数据权限（最低）
     * 仅能查看自己创建的数据
     */
    SELF(4, "仅本人数据权限", "最低", "仅能查看自己创建的数据");

    /**
     * 权限值（存储到数据库）
     * 值越小权限越大：ALL(1) > DEPT_AND_CHILD(2) > DEPT(3) > SELF(4)
     */
    private final Integer value;

    /**
     * 权限名称
     */
    private final String name;

    /**
     * 权限等级
     */
    private final String level;

    /**
     * 权限描述
     */
    private final String description;

    /**
     * 判断是否为全部数据权限（跳过部门/用户过滤，仅受租户隔离约束）
     */
    public boolean isAll() {
        return this == ALL;
    }

    /**
     * 判断是否为本部门及以下数据权限
     */
    public boolean isDeptAndChild() {
        return this == DEPT_AND_CHILD;
    }

    /**
     * 判断是否为部门数据权限
     */
    public boolean isDept() {
        return this == DEPT;
    }

    /**
     * 判断是否需要部门过滤（DEPT_AND_CHILD 或 DEPT）
     */
    public boolean isDeptScope() {
        return this == DEPT_AND_CHILD || this == DEPT;
    }

    /**
     * 判断是否为最低权限（仅本人数据权限）
     */
    public boolean isSelf() {
        return this == SELF;
    }

    /**
     * 判断当前权限是否大于等于指定权限
     * 权限值越小，权限越大
     *
     * @param other 待比较的权限
     * @return true 表示当前权限范围更大或相等
     */
    public boolean greaterThanOrEqual(DataScopeEnum other) {
        return this.value <= other.value;
    }

    /**
     * 获取最大权限（取权限值最小的）
     *
     * @param permissions 多个权限
     * @return 范围最大的权限
     */
    public static DataScopeEnum getMaxPermission(DataScopeEnum... permissions) {
        DataScopeEnum max = SELF;
        for (DataScopeEnum permission : permissions) {
            if (permission != null && permission.value < max.value) {
                max = permission;
            }
        }
        return max;
    }
}
package com.simple.common.auth.client.common.enums;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限枚举
 * 权限值按数据权限范围从大到小递增：ALL(1) < CUSTOM(2) < DEPT(3) < DEPT_AND_CHILD(4) < SELF(5)
 * 取值最小即为最大权限范围
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
@Schema(description = "数据权限枚举")
public enum DataScopeEnum {

    /**
     * 全部数据权限（最高）
     * 不添加任何过滤条件，可查看所有数据
     */
    ALL(1, "全部数据权限", "最高", "不添加任何过滤条件，可查看所有数据"),

    /**
     * 自定数据权限（较高）
     * 仅能查看指定若干部门的数据
     */
    CUSTOM(2, "自定数据权限", "较高", "仅能查看指定若干部门的数据"),

    /**
     * 部门及以下数据权限（较低）
     * 能查看用户所属部门及所有子部门的数据
     */
    DEPT_AND_CHILD(3, "部门及以下数据权限", "较低", "能查看用户所属部门及所有子部门的数据"),

    /**
     * 部门数据权限（中等）
     * 仅能查看用户所属本部门的数据
     */
    DEPT(4, "部门数据权限", "中等", "仅能查看用户所属本部门的数据"),


    /**
     * 仅本人数据权限（最低）
     * 仅能查看自己创建的数据
     */
    SELF(5, "仅本人数据权限", "最低", "仅能查看自己创建的数据");

    /**
     * 权限值（存储到数据库）
     * 值越小权限越大：ALL(1) > CUSTOM(2) > DEPT(3) > DEPT_AND_CHILD(4) > SELF(5)
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
     * 判断是否为最高权限（全部数据权限）
     */
    public boolean isAll() {
        return this == ALL;
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
     */
    public boolean greaterThanOrEqual(DataScopeEnum other) {
        return this.value <= other.value;
    }

    /**
     * 获取最大权限（取权限值最小的）
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
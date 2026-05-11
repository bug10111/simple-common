package com.simple.common.auth.client.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 权限变更类型枚举
 * <p>
 * 用于标识权限变更的操作类型，客户端根据此类型执行不同的缓存更新策略。
 * </p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum PermissionChangeTypeEnum {

    /**
     * 新增权限（增量添加，不影响现有权限）
     */
    ADD("ADD", "新增权限"),

    /**
     * 更新权限（全量替换，先删除旧的再设置新的）
     */
    UPDATE("UPDATE", "更新权限"),

    /**
     * 删除权限（删除指定权限）
     */
    DELETE("DELETE", "删除权限"),

    /**
     * 清空所有权限（删除角色的所有权限）
     */
    CLEAR("CLEAR", "清空权限"),

    /**
     * 刷新权限（重新拉取最新权限数据）
     */
    REFRESH("REFRESH", "刷新权限");

    /**
     * 变更类型代码
     */
    private final String code;

    /**
     * 变更类型描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 变更类型代码
     * @return 对应的枚举值，未找到返回 null
     */
    public static PermissionChangeTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (PermissionChangeTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

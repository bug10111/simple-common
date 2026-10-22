package com.simple.common.auth.client.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 权限变更类型枚举
 * <p>
 * 用于标识权限变更的操作类型，客户端根据此类型执行不同的缓存更新策略。
 * code 即常量名的 JSON 序列化值（{@link #getCode()} 恒等于 {@link #name()}），
 * 改名/改值必须整体灰度，否则新旧实例落地缓存策略漂移。
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
     * 更新权限（全量替换：先写入新集合，再删除失效字段；REFRESH 与 UPDATE 同策略落地）
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
     * 刷新权限（重新拉取最新权限数据；接收端按 UPDATE 全量替换策略落地）
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
     * code → 枚举 静态索引（避免每次解析遍历 values() 分配新数组）
     */
    private static final Map<String, PermissionChangeTypeEnum> CODE_MAP = Stream.of(values())
            .collect(Collectors.toUnmodifiableMap(PermissionChangeTypeEnum::getCode, Function.identity()));

    /**
     * 根据代码获取枚举
     * <p>
     * 契约：null 返回 null；未知代码抛出 IllegalArgumentException（fail-fast）——
     * 静默返回 null 会让调用方把未知/新版本代码的变更事件静默丢弃（权限漂移不可见）。
     * </p>
     *
     * @param code 变更类型代码
     * @return 对应的枚举值；code 为 null 返回 null
     * @throws IllegalArgumentException code 非 null 且不在枚举范围内
     */
    public static PermissionChangeTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        PermissionChangeTypeEnum type = CODE_MAP.get(code);
        if (type == null) {
            throw new IllegalArgumentException("未知的权限变更类型代码: " + code + "，请确认发送端与接收端版本一致");
        }
        return type;
    }
}

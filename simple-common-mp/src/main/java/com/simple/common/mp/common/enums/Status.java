package com.simple.common.mp.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 通用状态枚举（兼容存量多域复用：启用/禁用、使用状态、操作结果、审核状态）。
 * <p>
 * 持久化契约：经 {@link EnumValue} 使用 code 值落库；code 取值为预留稀疏段
 * （1/2、11/22、111/222/333），各段含义见常量注释，新增常量必须按预留段扩段，
 * 不得插入既有段中间值，避免存量数据歧义。未知 code 的反查见 {@link #fromCode(Integer)}。
 * <p>
 * 序列化契约：Jackson 默认按枚举常量名序列化（如 "ON"），与持久化 code 是两套表示；
 * API 出入参请勿直接绑定 Integer 类型的 Status 字段。
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum Status {

    /**
     * 启用/禁用域：code 1/2
     */
    ON(1, "启用"),
    OFF(2, "禁用"),

    /**
     * 使用状态域：code 11/22
     */
    NOT_USED(11, "未使用"),
    USED(22, "已使用"),

    /**
     * 操作结果域：code 111/222
     */
    OK(111, "成功"),
    ERROR(222, "失败"),

    /**
     * 审核状态域：code 333
     */
    UNDER_REVIEW(333, "审核中"),

    ;

    @EnumValue
    private final Integer code;

    private final String label;

    /**
     * 按持久化 code 反查枚举
     * <p>
     * 未知 code 返回 null（由调用方决定兜底语义），不做静默映射，不抛异常；
     * 调用方对存量脏数据应显式判断 null 后处理。
     *
     * @param code 数据库中的状态码
     * @return 对应枚举常量；code 为 null 或不在预留段内时返回 null
     */
    public static Status fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (Status status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}

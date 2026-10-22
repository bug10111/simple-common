package com.simple.common.annex.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 附件访问权限类型(公开/私有)。
 * <p>
 * 注意：{@link #code} 经 {@code @EnumValue} 持久化,是稳定存储契约,
 * 既有取值禁止修改或复用;新增常量必须使用全新 code,否则会破坏存量数据行。
 * </p>
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum ShareType {

    PUBLIC(1, "公开"), PRIVATE(2, "私有");

    /**
     * 持久化存储 code(稳定契约,禁止变更/复用)
     */
    @EnumValue
    private final int code;

    private final String label;

    /**
     * 根据持久化 code 反查枚举
     *
     * @param code 持久化存储的 code
     * @return 对应枚举,code 无法识别时抛出 IllegalArgumentException
     */
    public static ShareType fromCode(int code) {
        for (ShareType shareType : values()) {
            if (shareType.code == code) {
                return shareType;
            }
        }
        throw new IllegalArgumentException("未知的附件共享类型 code：" + code);
    }

}

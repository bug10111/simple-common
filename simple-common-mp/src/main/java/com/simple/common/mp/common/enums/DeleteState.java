package com.simple.common.mp.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * <p>
 * 逻辑删除状态。持久化经 {@link EnumValue} 使用 code 值，需与数据库逻辑删除配置保持一致。
 * <p>
 * 注意：常量名 OK 表达「有效（未删除）」而非「删除操作成功」，判断时务必以 code 语义为准：
 * 0=有效（未删除），1=已删除；消费方请勿以常量名望文生义（如 {@code deleted == DeleteState.OK}
 * 的比较应按「未删除」理解）。
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum DeleteState {

    /**
     * 有效（未删除），code = 0；持久化值为 0
     */
    OK(0, "有效"),

    /**
     * 已删除，code = 1；持久化值为 1
     */
    DELETE(1, "已删除"),

    ;

    @EnumValue
    private final Integer code;

    private final String label;
}

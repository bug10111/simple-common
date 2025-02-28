package com.simple.common.mp.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum Status {

    ON(1, "启用"),
    OFF(2, "禁用"),

    NOT_USED(11, "未使用"),
    USED(22, "已使用"),

    OK(111, "成功"),
    ERROR(222, "失败"),
    INFO(333, "审核中"),


    ;

    @EnumValue
    private final Integer code;

    private final String label;
}

package com.simple.common.mp.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author qty
 */
@Getter
@AllArgsConstructor
public enum DeleteState {

    DELETE(1, "已删除"), OK(0, "有效"),

    ;

    @EnumValue
    private final Integer code;

    private final String label;
}

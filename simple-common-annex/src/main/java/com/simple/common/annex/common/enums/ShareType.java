package com.simple.common.annex.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 附件类型
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum ShareType {
    PUBLIC(1, "公开"), PRIVATE(2, "私有");

    @EnumValue
    private final int code;

    private final String label;

}

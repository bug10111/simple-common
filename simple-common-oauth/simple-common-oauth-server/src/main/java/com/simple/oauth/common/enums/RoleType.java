package com.simple.oauth.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 角色类型
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum RoleType {

    SERVER(2, "允许分配权限"), CLIENT(1, "不可分配权限");

    @EnumValue
    private final int code;

    private final String label;

}

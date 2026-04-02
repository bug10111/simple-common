package com.simple.oauth.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 * Description: 服务类型
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum ServerType {

    server("服务端"),
    client("客户端"),

    ;

    private final String label;

}

package com.simple.common.core.common.enums.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Created with IntelliJ IDEA
 *
 * @author 兄台丶请冷静
 */
@Getter
@AllArgsConstructor
public enum SimpleOrder {
    Event(1), Oauth(2), Aviator(3), Auth(4);

    private final Integer order;
}

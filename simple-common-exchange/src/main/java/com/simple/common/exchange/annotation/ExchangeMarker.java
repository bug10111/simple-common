package com.simple.common.exchange.annotation;

import java.lang.annotation.*;

/**
 * 标记 Exchange 接口。所有跨模块 Exchange 接口必须标注此注解。
 * 实现类用 @LocalExchange 或 @CloudExchange 标注，通过 game.exchange.mode 切换。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExchangeMarker {

    /** 接口描述 */
    String value() default "";
}

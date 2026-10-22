package com.simple.common.exchange.annotation;

import java.lang.annotation.*;

/**
 * 标记 Exchange 接口。所有跨模块 Exchange 接口应标注此注解。
 * 实现类用 @LocalExchange 或 @CloudExchange 标注，通过 game.exchange.mode 切换。
 * <p>
 * <b>注意</b>：本注解当前为「契约标记」——仅作为 Exchange 接口的识别与文档锚点，
 * 框架内暂无运行期强制校验（未实现注解扫描/注册校验）；是否标注不影响装配行为。
 * 另受 Java 注解元数据限制，{@code @Target(ElementType.TYPE)} 无法限定仅接口可标注，
 * 类/枚举误标不会在编译期被拒绝，契约遵守依赖评审与后续接入注册校验时兜底。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExchangeMarker {

    /** 接口描述 */
    String value() default "";
}

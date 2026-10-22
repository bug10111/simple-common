package com.simple.common.exchange.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标注 Cloud 模式的 Exchange 实现（Feign 远程调用）
 * <p>
 * 模式条件已组合进注解：实现类标注本注解即自动携带
 * {@code @ConditionalOnProperty(name = "game.exchange.mode", havingValue = "cloud")}，
 * 仅在 cloud 模式装配，与 Local 侧实现互斥，避免双 Bean 并存导致注入歧义。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
@ConditionalOnProperty(name = "game.exchange.mode", havingValue = "cloud")
public @interface CloudExchange {

    /**
     * Bean 名称别名：同契约存在多个 Cloud 实现时用于显式命名/区分，缺省沿用类名
     */
    @AliasFor(annotation = Component.class, attribute = "value")
    String value() default "";
}

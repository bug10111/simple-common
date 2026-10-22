package com.simple.common.exchange.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标注 Local 模式的 Exchange 实现（单体直调）
 * <p>
 * 模式条件已组合进注解：实现类标注本注解即自动携带
 * {@code @ConditionalOnProperty(name = "game.exchange.mode", havingValue = "local", matchIfMissing = true)}，
 * 无需再手写条件注解；未实现对应条件的另一侧（Cloud）实现不会同时装配，
 * 从根上避免 local/cloud 双 Bean 并存导致的注入歧义。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
@ConditionalOnProperty(name = "game.exchange.mode", havingValue = "local", matchIfMissing = true)
public @interface LocalExchange {

    /**
     * Bean 名称别名：同契约存在多个 Local 实现时用于显式命名/区分，缺省沿用类名
     */
    @AliasFor(annotation = Component.class, attribute = "value")
    String value() default "";
}

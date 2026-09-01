package com.simple.common.exchange.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标注 Local 模式的 Exchange 实现（单体直调）
 * 与 @ConditionalOnProperty(name = "game.exchange.mode", havingValue = "local", matchIfMissing = true) 配合使用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface LocalExchange {
}

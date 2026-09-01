package com.simple.common.exchange.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标注 Cloud 模式的 Exchange 实现（Feign 远程调用）
 * 与 @ConditionalOnProperty(name = "game.exchange.mode", havingValue = "cloud") 配合使用
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
@Documented
public @interface CloudExchange {
}

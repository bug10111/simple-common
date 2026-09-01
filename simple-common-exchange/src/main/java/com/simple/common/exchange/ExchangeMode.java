package com.simple.common.exchange;

/**
 * Exchange 模式枚举
 * <p>
 * 用于控制模块间跨服务调用的实现方式：
 * <ul>
 *     <li>LOCAL - 单体模式，通过 Spring 直接注入本地实现类</li>
 *     <li>CLOUD - 微服务模式，通过 Feign 远程调用</li>
 * </ul>
 * <p>
 * 配置项：game.exchange.mode (local | cloud)，默认 local
 */
public enum ExchangeMode {

    /** 单体直调（默认） */
    LOCAL("local"),

    /** Feign 远程调用 */
    CLOUD("cloud");

    private final String value;

    ExchangeMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

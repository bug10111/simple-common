package com.simple.common.exchange;

/**
 * Exchange 模式持有者，从 application.yaml 读取 game.exchange.mode 配置
 * 默认 local（单体直调），切换为 cloud 时走 Feign 远程调用
 */
public interface ExchangeModeHolder {

    /**
     * 获取当前 Exchange 模式
     *
     * @return 当前模式枚举值
     */
    ExchangeMode getMode();
}

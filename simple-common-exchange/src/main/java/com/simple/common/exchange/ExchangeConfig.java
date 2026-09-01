package com.simple.common.exchange;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange 模式配置。读取 game.exchange.mode，暴露 ExchangeModeHolder Bean。
 */
@Configuration
public class ExchangeConfig {

    @Value("${game.exchange.mode:local}")
    private String exchangeMode;

    @Bean
    public ExchangeModeHolder exchangeModeHolder() {
        return () -> ExchangeMode.valueOf(exchangeMode.toUpperCase());
    }
}
